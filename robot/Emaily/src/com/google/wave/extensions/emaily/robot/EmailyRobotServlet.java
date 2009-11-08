/* Copyright (c) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.wave.extensions.emaily.robot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.jdo.identity.StringIdentity;
import javax.servlet.http.HttpServletRequest;

import org.apache.james.mime4j.MimeIOException;
import org.apache.james.mime4j.field.address.Address;
import org.apache.james.mime4j.field.address.Mailbox;
import org.apache.james.mime4j.message.Message;
import org.apache.james.mime4j.parser.MimeEntityConfig;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.wave.api.AbstractRobotServlet;
import com.google.wave.api.Blip;
import com.google.wave.api.Event;
import com.google.wave.api.EventType;
import com.google.wave.api.Gadget;
import com.google.wave.api.RobotMessageBundle;
import com.google.wave.api.StyleType;
import com.google.wave.api.StyledText;
import com.google.wave.api.TextView;
import com.google.wave.api.Wavelet;
import com.google.wave.extensions.emaily.config.EmailyConfig;
import com.google.wave.extensions.emaily.config.HostingProvider;
import com.google.wave.extensions.emaily.data.BlipVersionView;
import com.google.wave.extensions.emaily.data.DataAccess;
import com.google.wave.extensions.emaily.data.EmailToProcess;
import com.google.wave.extensions.emaily.data.PersistentEmail;
import com.google.wave.extensions.emaily.data.WaveletView;
import com.google.wave.extensions.emaily.email.MailUtil;
import com.google.wave.extensions.emaily.scheduler.EmailSchedulingCalculator;
import com.google.wave.extensions.emaily.util.DebugHelper;

/**
 * Servlet for serving the Wave robot requests.
 * 
 * @author dlux
 * 
 */
@Singleton
public class EmailyRobotServlet extends AbstractRobotServlet {
  private static final long serialVersionUID = 8878209094937861353L;

  private static final String PROCESS_EMAIL_MAX_DELAY = "incoming.process_email_max_delay";

  // Injected dependencies
  private final HostingProvider hostingProvider;
  private final Provider<HttpServletRequest> reqProvider;
  private final PersistenceManagerFactory pmFactory;
  private final Logger logger;
  private final EmailSchedulingCalculator emailSchedulingCalculator;
  private final DebugHelper debugHelper;
  private final Provider<DataAccess> dataAccessProvider;
  private final MailUtil mailUtil;
  private final EmailyConfig config;

  private static final String introText = "Emaily helps you sending emails from wave.\n"
      + "Type an email address below, and click 'Add' to add it as an email recipient.\n";
  private static final String emailGadgetPrefix = "email:";

  private MimeEntityConfig mimeEntityConfig = new MimeEntityConfig();

  @Inject
  public EmailyRobotServlet(HostingProvider hostingProvider,
      Provider<HttpServletRequest> reqProvider, PersistenceManagerFactory pmFactory, Logger logger,
      EmailSchedulingCalculator emailSchedulingCalculator, DebugHelper debugHelper,
      Provider<DataAccess> dataAccessProvider, MailUtil mailUtil, EmailyConfig config) {
    this.hostingProvider = hostingProvider;
    this.reqProvider = reqProvider;
    this.pmFactory = pmFactory;
    this.logger = logger;
    this.emailSchedulingCalculator = emailSchedulingCalculator;
    this.debugHelper = debugHelper;
    this.dataAccessProvider = dataAccessProvider;
    this.mailUtil = mailUtil;
    this.config = config;
  }

  /**
   * Handles all robot events.
   */
  @Override
  public void processEvents(RobotMessageBundle bundle) {
    updateWaveletIdForEmail(bundle);
    String proxyingFor = getProxyingFor();
    if (!proxyingFor.isEmpty()) {
      String email = hostingProvider.getEmailAddressFromRobotProxyFor(proxyingFor);
      processWaveletViewModifications(bundle, email);
    } else {
      handleRobotEvents(bundle);
    }
    processIncomingEmails(bundle);
  }

  private void handleRobotEvents(RobotMessageBundle bundle) {
    // Process the events
    for (Event e : bundle.getEvents()) {
      processRobotBlipEvent(bundle, e);
    }
  }

  private void processRobotBlipEvent(RobotMessageBundle bundle, Event e) {
    switch (e.getType()) {
    case WAVELET_SELF_ADDED:
      handleRobotAdded(bundle);
      break;
    case BLIP_VERSION_CHANGED:
      handleBlipChanged(bundle, e);
      break;
    }
  }

  private void handleBlipChanged(RobotMessageBundle bundle, Event e) {
    Blip blip = e.getBlip();
    Gadget gadget = blip.getDocument().getGadgetView().getGadget(getAddEmailAddressGadgetUrl());
    if (gadget == null)
      return;
    Map<String, String> properties = gadget.getProperties();
    HashSet<String> waveletParticipants = new HashSet<String>(bundle.getWavelet().getParticipants());
    for (String key : new HashSet<String>(properties.keySet())) {
      if (!key.startsWith(emailGadgetPrefix))
        continue;
      String emailAddress = key.substring(emailGadgetPrefix.length());
      logger.info("User added email address: " + emailAddress);
      gadget.deleteField(key);
      String newParticipant;
      try {
        newParticipant = hostingProvider.getRobotProxyForFromEmailAddress(emailAddress);
      } catch (IllegalArgumentException ex) {
        // If it is not a valid email address, then we don't add it.
        logger.warning("Invalid user input for email address: " + emailAddress + "\n"
            + ex.toString());
        continue;
      }
      if (waveletParticipants.contains(newParticipant))
        continue;
      bundle.getWavelet().addParticipant(newParticipant);
    }
  }

  /**
   * Process modifications for the wavelet changes and schedule sending.
   * 
   * @param bundle The robot message bundle.
   * @param email The email of the user.
   */
  private void processWaveletViewModifications(RobotMessageBundle bundle, String email) {
    try {
      String waveletId = bundle.getWavelet().getWaveletId();

      // Get or create the WaveletView for the wavelet and the current user.
      WaveletView waveletView = dataAccessProvider.get().getWaveletView(waveletId, email);
      if (waveletView == null) {
        waveletView = new WaveletView(waveletId, email, bundle.getWavelet().getRootBlipId());
        dataAccessProvider.get().persistWaveletView(waveletView);
      }

      updateWaveletView(waveletView, bundle);

      // Process the blip events
      for (Event e : bundle.getEvents()) {
        processBlipEvent(waveletView, e);
      }

      // Calculate the next send time
      emailSchedulingCalculator.calculateWaveletViewNextSendTime(waveletView);

      // Prints the debug info
      logger.info(debugHelper.printWaveletViewInfo(waveletView));
      dataAccessProvider.get().commit();
    } finally {
      dataAccessProvider.get().close();
    }
  }

  /**
   * Updates the wavelet view from the information received by the robot. Currently it just updates
   * the title.
   * 
   * @param waveletView The wavelet view to update.
   * @param bundle The message bundle received by the robot.
   */
  private void updateWaveletView(WaveletView waveletView, RobotMessageBundle bundle) {
    waveletView.setTitle(bundle.getWavelet().getTitle());
  }

  /**
   * Process one blip-related event.
   * 
   * @param waveletView The wavelet view data object of the current event.
   * @param e The wave event.
   */
  private void processBlipEvent(WaveletView waveletView, Event e) {
    logger.finer("Started processing blip events");
    Blip blip = e.getBlip();
    if (blip == null) {
      logger.fine("Not blip event, returning");
      return;
    }

    // If the robot is a contributor, then we don't process the blip:
    if (blip.getContributors().contains(hostingProvider.getRobotWaveId()))
      return;

    // Extract the content
    logger.finer("Extracting content");
    String blipContent = blip.getDocument().getText();
    if (blip.getBlipId().equals(waveletView.getRootBlipId())) {
      // Remove the Wave Title from the root blip content.
      blipContent = blipContent.substring(waveletView.getTitle().length()).trim();
    }

    String waveProxyIdOfUser = hostingProvider.getRobotProxyForFromEmailAddress(waveletView
        .getEmail());
    if (waveProxyIdOfUser.equals(blip.getCreator())) {
      logger.fine("The blip is sent by the user itself, won't create a new blip from it");
      return;
    }

    // find the corresponding blip in the unsent ones
    logger.finer("find the blip if it was already in the unsent list");
    BlipVersionView blipVersionView = null;
    for (BlipVersionView b : waveletView.getUnsentBlips()) {
      if (blip.getBlipId().equals(b.getBlipId())) {
        blipVersionView = b;
        break;
      }
    }

    // If it did not exist yet, create one:
    logger.finer("create one if not exist yet");
    if (blipVersionView == null) {
      if (blipContent.isEmpty()) {
        logger.fine("The blip content is empty, we don't create a new blip");
        return;
      }
      if (e.getType() == EventType.BLIP_SUBMITTED) {
        logger.fine("Blip submitted without edit: don't create a new version of it");
        return;
      }
      // Otherwise, create a new BlipVersionWiew
      blipVersionView = new BlipVersionView(waveletView, blip.getBlipId());
      waveletView.getUnsentBlips().add(blipVersionView);
    }
    // Update the blip version to the latest
    blipVersionView.setVersion(blip.getVersion());

    blipVersionView.setParticipants(blip.getContributors());

    boolean still_editing = false;
    switch (e.getType()) {
    case BLIP_DELETED:
      blipVersionView.setContent("");
      break;
    case BLIP_SUBMITTED:
      break;
    default:
      still_editing = true;
      break;
    }

    if (still_editing && blipContent.equals(blipVersionView.getContent())) {
      // We don't change any timestamp if the content is not changed.
      return;
    }
    blipVersionView.setContent(blipContent);

    emailSchedulingCalculator.updateBlipViewTimestamps(blipVersionView, still_editing);
  }

  /**
   * Returns the "proxyingFor" argument of the Wave Robot query for the currently processed request.
   * 
   * @return The value of the "proxyingFor" field, or if it does not exist, it returns an empty
   *         string.
   */
  private String getProxyingFor() {
    JSONObject json = (JSONObject) reqProvider.get().getAttribute("jsonObject");
    if (json.has("proxyingFor")) {
      try {
        String proxyingFor = json.getString("proxyingFor");
        return proxyingFor == null ? "" : proxyingFor;
      } catch (JSONException e) {
        throw new RuntimeException("JSON error", e);
      }
    } else {
      return "";
    }
  }

  /**
   * Handle when the robot is added as a participant. An introduction blip is added to the wave if
   * robot is added as a participant without "proxyingFor" argument, i.e., adding
   * emaily-wave@appspot.com will trigger a blip, but emaily-wave+abc+xyz.com@appspot.com will not.
   * It also adds a form which makes it easy to add new email participants to the wave.
   * 
   * @param bundle RobotMessageBundle received from the Wave Server.
   */
  private void handleRobotAdded(RobotMessageBundle bundle) {
    logger.info("Robot added to participants");
    // bundle.getWavelet().appendBlip().getDocument().append(introText) will
    // display the text twice due to
    // http://code.google.com/p/google-wave-resources/issues/detail?id=354
    // TODO(karan) Change this once the above Wave API issue is fixed.

    Blip newBlip = bundle.getWavelet().appendBlip();
    newBlip.getDocument().delete();
    newBlip.getDocument().append(introText);
    newBlip.getDocument().appendElement(new Gadget(getAddEmailAddressGadgetUrl()));
  }

  private String getAddEmailAddressGadgetUrl() {
    return hostingProvider.getRobotURL() + "gadgets/add-email-address-gadget.xml";
  }

  /**
   * Updates the wavelet ID reference for the email representing this wavelet.
   * 
   * @param bundle The Wave events to process.
   */
  private void updateWaveletIdForEmail(RobotMessageBundle bundle) {
    final Wavelet wavelet = bundle.getWavelet();
    if (!wavelet.hasDataDocument("Message-ID"))
      return;
    PersistenceManager pm = pmFactory.getPersistenceManager();
    Transaction tx = pm.currentTransaction();
    tx.begin();
    String messageId = wavelet.getDataDocument("Message-ID");
    logger.info("Wavelet links to Message-ID: " + messageId);
    try {
      PersistentEmail email = (PersistentEmail) pm.getObjectById(PersistentEmail.class, messageId);
      if (email.getWaveletId() == null) {
        // Attach the Wavelet ID to the email.
        email.setWaveAndWaveletId(wavelet.getWaveId(), wavelet.getWaveletId());

      } else {
        if (!email.getWaveletId().equals(wavelet.getWaveletId())) {
          logger.warning("Wavelet ID mismatch for email with message ID " + messageId);
          logger.warning("Expecting Wavelet ID " + wavelet.getWaveletId()
              + " but email has Wavelet ID " + email.getWaveletId());
        }
      }
    } catch (JDOObjectNotFoundException onf) {
      onf.printStackTrace();
    }
    tx.commit();
  }

  /**
   * Processes the incoming emails accumulated so far.
   * 
   * TODO(taton) When an email has many wave recipients, this currently duplicates the Wave.
   * 
   * @param bundle The Wave events to process.
   */
  private void processIncomingEmails(RobotMessageBundle bundle) {
    final Wavelet wavelet = bundle.getWavelet();
    final PersistenceManager pm = pmFactory.getPersistenceManager();
    final Transaction tx = pm.currentTransaction();
    try {
      // Retrieve the IDs of the incoming messages to process.
      Query query = pm.newQuery(EmailToProcess.class);
      @SuppressWarnings( { "unchecked" })
      List<EmailToProcess> emailsToProcess = (List<EmailToProcess>) query.execute();

      if (emailsToProcess.isEmpty())
        return;
      Map<String, EmailToProcess> rawMap = new HashMap<String, EmailToProcess>();
      Set<StringIdentity> ids = new HashSet<StringIdentity>();
      for (EmailToProcess email : emailsToProcess) {
        ids.add(new StringIdentity(PersistentEmail.class, email.getMessageId()));
        rawMap.put(email.getMessageId(), email);
      }

      // List of the emails that are successfully processed.
      List<EmailToProcess> processed = new ArrayList<EmailToProcess>();

      @SuppressWarnings( { "unchecked" })
      Collection<PersistentEmail> emails = pm.getObjectsById(ids);
      for (PersistentEmail email : emails) {
        EmailToProcess raw = rawMap.get(email.getMessageId());
        try {
          tx.begin();
          if (processIncomingEmail(bundle, pm, raw, email))
            processed.add(raw);
          tx.commit();
        } catch (Exception exn) {
          exn.printStackTrace();
        } finally {
          if (tx.isActive())
            tx.rollback();
        }
      } // For loop

      // Purge the EmailToProcess objects that have been successfully processed.
      tx.begin();
      pm.deletePersistentAll(processed);
      tx.commit();

    } finally {
      pm.close();
    }
  }

  /**
   * Processes one incoming email. Appends a new blip to the existing Wavelet if one exists already
   * for the thread the email belongs to, otherwise creates a new Wavelet.
   * 
   * @param bundle
   * @param pm
   * @param raw
   * @param email
   * @returns True if the email has been successfully processed, false otherwise.
   */
  private boolean processIncomingEmail(RobotMessageBundle bundle, PersistenceManager pm,
      EmailToProcess raw, PersistentEmail email) throws MimeIOException, IOException {

    // Find the wavelet representing the thread, null if none.
    Wavelet threadWavelet = null;
    if (email.getReferences() != null) {
      // Look for a Wavelet ID in the message references.
      Set<StringIdentity> refIds = new HashSet<StringIdentity>();
      for (String refId : email.getReferences())
        refIds.add(new StringIdentity(PersistentEmail.class, refId));
      @SuppressWarnings( { "unchecked" })
      Collection<PersistentEmail> references = pm.getObjectsById(refIds);
      // We pick the first reference we find.
      for (PersistentEmail reference : references) {
        if (reference.getWaveletId() == null)
          continue;
        if (!reference.getWaveletId().isEmpty()) {
          threadWavelet = bundle.getWavelet(reference.getWaveId(), reference.getWaveletId());
          break;
        }
      }

      if ((threadWavelet == null) && !references.isEmpty()) {
        // This email belongs to a thread, has references, but we could not look the corresponding
        // wavelet ID up in the referenced emails.
        // Postpone the processing a bit, until the references are processed and associated to a
        // Wavelet ID.
        long processingDate = Calendar.getInstance().getTimeInMillis();
        if (raw.getProcessingDate() == null)
          raw.setProcessingDate(processingDate);
        else
          processingDate = raw.getProcessingDate();

        // If the processing delay gets too long, we give up and create a new wave.
        long processingDelay = Calendar.getInstance().getTimeInMillis() - processingDate;
        if (processingDelay < config.getLong(PROCESS_EMAIL_MAX_DELAY) * 1000)
          return false;
      }
    }

    Message message = new Message(raw.getInputStream(), mimeEntityConfig);
    // Create the blip that will contain the email content.
    Blip emailBlip = null;
    if (threadWavelet == null) {
      // We could not link this message to an existing Wavelet:
      // create a new Wavelet and write message to the root blip.
      List<String> participants = new ArrayList<String>();
      participants.addAll(email.getWaveParticipants());
      Wavelet newWavelet = bundle.getWavelet().createWavelet(participants, null);
      newWavelet.setTitle(message.getSubject());
      newWavelet.setDataDocument("Message-ID", email.getMessageId());
      emailBlip = newWavelet.getRootBlip();
    } else {
      // There is a Wavelet for this thread: append message to a new blip.
      email.setWaveAndWaveletId(threadWavelet.getWaveId(), threadWavelet.getWaveletId());
      emailBlip = threadWavelet.appendBlip();
    }

    writeMessageToBlip(message, emailBlip);
    return true;
  }

  /**
   * Formats an email into a Blip.
   * 
   * @param message The incoming email to format.
   * @param blip The blip to write into.
   */
  private void writeMessageToBlip(Message message, Blip blip) throws MimeIOException, IOException {
    TextView textView = blip.getDocument();
    String author = null;
    if (message.getFrom() != null) {
      // Note: appendStyledText has a bug: the style does not apply to the last character.
      textView.appendStyledText(new StyledText("From: ", StyleType.BOLD));
      StringBuilder sb = new StringBuilder();
      for (Mailbox from : message.getFrom()) {
        if (from == null)
          continue;
        if (author == null)
          author = hostingProvider.getRobotProxyForFromEmailAddress(from.getAddress());
        if (sb.length() > 0)
          sb.append(", ");
        sb.append(from.toString());
      }
      textView.append(sb.toString());
      textView.appendMarkup("<br/>\n");
    }
    if (author == null)
      author = hostingProvider.getRobotWaveId();
    textView.setAuthor(author);
    if (message.getTo() != null) {
      textView.appendStyledText(new StyledText("To: ", StyleType.BOLD));
      StringBuilder sb = new StringBuilder();
      for (Address recipient : message.getTo()) {
        if (recipient == null)
          continue;
        if (sb.length() > 0)
          sb.append(", ");
        sb.append(recipient.toString());
      }
      textView.append(sb.toString());
      textView.appendMarkup("<br/>\n");
    }
    if (message.getBody() != null) {
      // TODO(taton) The Wave robot Java API is buggy when processing end-of-lines. This still does
      // not work properly.
      StringBuilder sb = new StringBuilder();
      sb.append('\n');
      mailUtil.mimeEntityToText(sb, message);
      sb.append('\n');
      textView.append(sb.toString());
    }
  }
}
