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
import java.util.logging.Level;
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
import com.google.wave.api.Range;
import com.google.wave.api.RobotMessageBundle;
import com.google.wave.api.StyleType;
import com.google.wave.api.StyledText;
import com.google.wave.api.TextView;
import com.google.wave.api.Wavelet;
import com.google.wave.extensions.emaily.config.EmailyConfig;
import com.google.wave.extensions.emaily.config.HostingProvider;
import com.google.wave.extensions.emaily.data.EmailToProcess;
import com.google.wave.extensions.emaily.data.PersistentEmail;
import com.google.wave.extensions.emaily.email.MailUtil;

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
  private final MailUtil mailUtil;
  private final EmailyConfig config;
  private final EmailUserProxyEventHandler emailUserProxyEventHandler;

  private MimeEntityConfig mimeEntityConfig = new MimeEntityConfig();

  private final RobotNoProxyEventHandler robotNoProxyEventHandler;

  @Inject
  public EmailyRobotServlet(HostingProvider hostingProvider,
      Provider<HttpServletRequest> reqProvider, PersistenceManagerFactory pmFactory, Logger logger,
      MailUtil mailUtil, EmailyConfig config,
      EmailUserProxyEventHandler emailUserProxyEventHandler,
      RobotNoProxyEventHandler robotNoProxyEventHandler) {
    this.hostingProvider = hostingProvider;
    this.reqProvider = reqProvider;
    this.pmFactory = pmFactory;
    this.logger = logger;
    this.mailUtil = mailUtil;
    this.config = config;
    this.emailUserProxyEventHandler = emailUserProxyEventHandler;
    this.robotNoProxyEventHandler = robotNoProxyEventHandler;
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
      emailUserProxyEventHandler.processEvents(bundle, email);
    } else {
      robotNoProxyEventHandler.processEvents(bundle);
    }
    processIncomingEmails(bundle);
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
        wavelet.setDataDocument("Message-ID", null);

      } else {
        if (!email.getWaveletId().equals(wavelet.getWaveletId())) {
          logger.warning("Wavelet ID mismatch for email with message ID " + messageId);
          logger.warning("Expecting Wavelet ID " + wavelet.getWaveletId()
              + " but email has Wavelet ID " + email.getWaveletId());
        }
      }
    } catch (JDOObjectNotFoundException onf) {
      logger.log(Level.WARNING, "Cannot retrieve PersistentObject", onf);
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

      // Retrieve all the persistent emails referenced by EmailToProcess objects.
      // This requires the referenced objects to exist in the data store.
      @SuppressWarnings( { "unchecked" })
      Collection<PersistentEmail> emails = pm.getObjectsById(ids, true);

      for (PersistentEmail email : emails) {
        EmailToProcess raw = rawMap.get(email.getMessageId());
        try {
          tx.begin();
          if (processIncomingEmail(bundle, pm, raw, email))
            processed.add(raw);
          tx.commit();
        } catch (Exception exn) {
          logger.log(Level.WARNING, "Error during processing incoming email", exn);
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
      // We pick the first reference we find.
      for (String refId : email.getReferences()) {
        try {
          // Retrieve the referenced message. Don't worry about non-existing references.
          PersistentEmail reference = pm.getObjectById(PersistentEmail.class, refId);
          if (reference.getWaveletId() == null)
            continue;
          if (!reference.getWaveletId().isEmpty()) {
            logger.log(Level.FINE, "Found existing Wavelet ID: " + reference.getWaveId() + " "
                + reference.getWaveletId());
            threadWavelet = bundle.getWavelet(reference.getWaveId(), reference.getWaveletId());
            break;
          }
        } catch (JDOObjectNotFoundException nonfe) {
          // This just means we don't know this message ID.
          logger.log(Level.FINE, "Unknown email message ID: " + refId);
        }

      }

      if ((threadWavelet == null) && !email.getReferences().isEmpty()) {
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
        if (processingDelay < config.getLong(PROCESS_EMAIL_MAX_DELAY) * 1000) {
          logger.log(Level.FINE, "Postponing processing of incoming email " + raw.getMessageId());
          return false;
        }
      }
    }

    Message message = new Message(raw.getInputStream(), mimeEntityConfig);
    // Create the blip that will contain the email content.
    Blip emailBlip = null;
    if (threadWavelet == null) {
      // We could not link this message to an existing Wavelet:
      // create a new Wavelet and write message to the root blip.
      logger.log(Level.FINE, "Creating a new Wavelet for message " + raw.getMessageId());
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
    StringBuilder sb = new StringBuilder();
    String author = null;
    List<Range> ranges = new ArrayList<Range>();

    if (message.getFrom() != null) {
      StringBuilder fromSB = new StringBuilder();
      for (Mailbox from : message.getFrom()) {
        if (from == null)
          continue;
        if (author == null)
          author = hostingProvider.getRobotProxyForFromEmailAddress(from.getAddress());
        if (fromSB.length() > 0)
          fromSB.append(", ");
        fromSB.append(from.toString());
      }
      if (fromSB.length() > 0) {
        final String prefix = "From: ";
        ranges.add(new Range(sb.length(), sb.length() + prefix.length()));
        sb.append(prefix).append(fromSB).append('\n');
      }
    }
    if (author == null)
      author = hostingProvider.getRobotWaveId();
    if (message.getTo() != null) {
      StringBuilder toSB = new StringBuilder();
      for (Address recipient : message.getTo()) {
        if (recipient == null)
          continue;
        if (toSB.length() > 0)
          toSB.append(", ");
        toSB.append(recipient.toString());
      }
      if (toSB.length() > 0) {
        final String prefix = "To: ";
        ranges.add(new Range(sb.length(), sb.length() + prefix.length()));
        sb.append(prefix).append(toSB).append('\n');
      }
    }
    if (message.getBody() != null) {
      StringBuilder sbBody = new StringBuilder();
      mailUtil.mimeEntityToText(sbBody, message);
      sb.append(sbBody.toString().trim());
    }

    TextView textView = blip.getDocument();
    textView.setAuthor(author);
    textView.append(sb.toString());

    for (Range range : ranges)
      textView.setAnnotation(range, "styled-text", StyleType.BOLD.toString());
  }

  private void writeMessageToBlipOld(Message message, Blip blip) throws MimeIOException,
      IOException {
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
