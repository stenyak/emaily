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
import java.util.List;
import java.util.logging.Logger;

import javax.jdo.Extent;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
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
import com.google.wave.api.RobotMessageBundle;
import com.google.wave.api.StyleType;
import com.google.wave.api.StyledText;
import com.google.wave.api.TextView;
import com.google.wave.api.Wavelet;
import com.google.wave.extensions.emaily.config.HostingProvider;
import com.google.wave.extensions.emaily.data.BlipVersionView;
import com.google.wave.extensions.emaily.data.DataAccess;
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

  // Injected dependencies
  private final HostingProvider hostingProvider;
  private final Provider<HttpServletRequest> reqProvider;
  private final PersistenceManagerFactory pmFactory;
  private final Logger logger;
  private final EmailSchedulingCalculator emailSchedulingCalculator;
  private final DebugHelper debugHelper;
  private final Provider<DataAccess> dataAccessProvider;
  private final MailUtil mailUtil;

  private static final String dummyEmailId = "xyz@abc.com";
  private static final String introText
      = "Send messages to your friend with email %s, by adding %s " 
        + "as a participant to this wave.";

  private MimeEntityConfig mimeEntityConfig = new MimeEntityConfig();

  @Inject
  public EmailyRobotServlet(HostingProvider hostingProvider,
      Provider<HttpServletRequest> reqProvider, PersistenceManagerFactory pmFactory, Logger logger,
      EmailSchedulingCalculator emailSchedulingCalculator, DebugHelper debugHelper,
      Provider<DataAccess> dataAccessProvider, MailUtil mailUtil) {
    this.hostingProvider = hostingProvider;
    this.reqProvider = reqProvider;
    this.pmFactory = pmFactory;
    this.logger = logger;
    this.emailSchedulingCalculator = emailSchedulingCalculator;
    this.debugHelper = debugHelper;
    this.dataAccessProvider = dataAccessProvider;
    this.mailUtil = mailUtil;
  }

  /**
   * Handles all robot events.
   */
  @Override
  public void processEvents(RobotMessageBundle bundle) {
    // New behavior:
    String proxyingFor = getProxyingFor();
    if (!proxyingFor.isEmpty()) {
      String email = hostingProvider.getEmailAddressFromRobotProxyFor(proxyingFor);
      processWaveletViewModifications(bundle, email);
    } else if (bundle.wasSelfAdded()) {
      // TODO(karan): uncomment the call to handleRobotAdded() when the form to
      // allow email recipients is implemented.  
      // handleRobotAdded(bundle);
    }
    processIncomingEmails(bundle.getWavelet());
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
   * Handle when the robot is added as a participant.
   * An introduction blip is added to the wave if robot is added 
   * as a participant without sub address, i.e., adding emaily-wave@appspot.com
   * will trigger a blip, but emaily-wave+abc+xyz.com@appspot.com will not.
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
    newBlip.getDocument().append(String.format(introText, dummyEmailId, 
            hostingProvider.getRobotProxyForFromEmailAddress(dummyEmailId)));      
  }
  
  /**
   * Processes the incoming emails accumulated so far: creates one new wave per email.
   * 
   * TODO(taton) When an email has many wave recipients, this currently duplicates the Wave.
   * 
   * @param wavelet Handle to create new waves.
   */
  private void processIncomingEmails(Wavelet wavelet) {
    PersistenceManager pm = pmFactory.getPersistenceManager();
    Transaction tx = pm.currentTransaction();

    try {
      Extent<PersistentEmail> extent = pm.getExtent(PersistentEmail.class, false);
      Query query = pm.newQuery(extent);
      @SuppressWarnings( { "unchecked" })
      List<PersistentEmail> emails = (List<PersistentEmail>) query.execute();
      for (PersistentEmail email : emails) {
        try {
          tx.begin();
          createWaveFromMessage(wavelet, email);
          // TODO(taton) We should not delete the email here, but instead associate it with the Wave
          // created for it somehow.
          pm.deletePersistent(email);
          tx.commit();
        } catch (Exception exn) {
          exn.printStackTrace();
        } finally {
          if (tx.isActive())
            tx.rollback();
        }
      } // For loop

    } finally {
      pm.close();
    }
  }

  /**
   * Creates the Wave representing an email.
   * 
   * @param wavelet Handle to create new Waves.
   * @param message The incoming email to create the wave of.
   */
  private void createWaveFromMessage(Wavelet wavelet, PersistentEmail email)
      throws MimeIOException, IOException {
    Message message = new Message(email.getInputStream(), mimeEntityConfig);
    List<String> participants = new ArrayList<String>();
    participants.addAll(email.getWaveParticipants());
    Wavelet newWavelet = wavelet.createWavelet(participants, null);
    newWavelet.setTitle(message.getSubject());
    Blip blip = newWavelet.getRootBlip();
    TextView textView = blip.getDocument();
    // textView.setAuthor(APPSPOT_ID + "@appspot.com");
    if (message.getFrom() != null) {
      // Note: appendStyledText has a bug: the style does not apply to the last character.
      textView.appendStyledText(new StyledText("From: ", StyleType.BOLD));
      StringBuilder sb = new StringBuilder();
      for (Mailbox from : message.getFrom()) {
        if (from == null)
          continue;
        if (sb.length() > 0)
          sb.append(", ");
        sb.append(from.toString());
      }
      textView.append(sb.toString());
      textView.appendMarkup("<br/>\n");
    }
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
