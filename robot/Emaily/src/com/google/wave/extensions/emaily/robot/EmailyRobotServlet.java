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
      emailUserProxyEventHandler.processEvents(bundle);
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

  private final static String CREATOR_MESSAGE_ID = "Creator-Message-ID";

  /**
   * Updates the wavelet ID reference for the email representing this wavelet.
   * 
   * @param bundle The Wave events to process.
   */
  private void updateWaveletIdForEmail(RobotMessageBundle bundle) {
    final Wavelet wavelet = bundle.getWavelet();
    if (!wavelet.hasDataDocument(CREATOR_MESSAGE_ID))
      return;
    PersistenceManager pm = pmFactory.getPersistenceManager();
    Transaction tx = pm.currentTransaction();
    try {
      tx.begin();
      String messageId = wavelet.getDataDocument(CREATOR_MESSAGE_ID);
      logger.info("Adding reference from email with Message-ID: " + messageId + " to "
          + wavelet.getWaveId() + " " + wavelet.getWaveletId());
      PersistentEmail email = PersistentEmail.lookupByMessageId(pm, messageId);
      if (email == null) {
        logger.log(Level.WARNING, "Missing PersistentEmail with message ID: " + messageId);
        return;
      }
      if (email.getWaveletId() == null) {
        // Attach the Wavelet ID to the email.
        email.setWaveAndWaveletId(wavelet.getWaveId(), wavelet.getWaveletId());
        wavelet.setDataDocument(CREATOR_MESSAGE_ID, null);

      } else {
        if (!email.getWaveletId().equals(wavelet.getWaveletId())) {
          logger.warning("Wavelet ID mismatch for email with message ID " + messageId);
          logger.warning("Expecting Wavelet ID " + wavelet.getWaveletId()
              + " but email has Wavelet ID " + email.getWaveletId());
        }
      }
      tx.commit();
    } finally {
      if (tx.isActive())
        tx.rollback();
    }
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
      // List of the emails that are successfully processed.
      List<EmailToProcess> processed = new ArrayList<EmailToProcess>();

      for (EmailToProcess rawEmail : emailsToProcess) {
        try {
          PersistentEmail email = PersistentEmail.lookupByMessageId(pm, rawEmail.getMessageId());
          if (email == null) {
            logger.warning("PersistentEmail missing for EmailToProcess with message ID: "
                + rawEmail.getMessageId());
            continue;
          }
          if (processIncomingEmail(bundle, pm, rawEmail, email))
            processed.add(rawEmail);
        } catch (Exception exn) {
          logger.log(Level.WARNING, "Error during processing incoming email", exn);
        }
      }

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
   * for the thread the email belongs to, otherwise creates a new Wavelet. TODO(taton) Refactor all
   * email processing into a single class.
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
        // Retrieve the referenced message. Don't worry about non-existing references.
        PersistentEmail reference = PersistentEmail.lookupByMessageId(pm, refId);
        if (reference == null) {
          // This just means we don't know this message ID.
          logger.finer("Unknown email message ID: " + refId);
          continue;
        }
        if (reference.getWaveletId() == null) {
          // We know this message ID, but it has not been linked to a Wavelet yet.
          logger.finer("Email message ID: " + refId + " has not been linked to Wavelet.");
          continue;
        }
        if (!reference.getWaveletId().isEmpty()) {
          logger.finer("Found existing Wavelet ID: " + reference.getWaveId() + " "
              + reference.getWaveletId());
          threadWavelet = bundle.getWavelet(reference.getWaveId(), reference.getWaveletId());
          break;
        }
      }

      if ((threadWavelet == null) && !email.getReferences().isEmpty()) {
        // This email belongs to a thread, has references, but we could not look the corresponding
        // wavelet ID up in the referenced emails.
        // Postpone the processing a bit, until the references are processed and associated to a
        // Wavelet ID.
        if (raw.getProcessingDate() == null) {
          Transaction tx = pm.currentTransaction();
          tx.begin();
          try {
            raw.setProcessingDate(Calendar.getInstance().getTimeInMillis());
            tx.commit();
          } finally {
            if (tx.isActive())
              tx.rollback();
          }
        }

        // If the processing delay gets too long, we give up and create a new wave.
        long processingDelay = Calendar.getInstance().getTimeInMillis() - raw.getProcessingDate();
        if (processingDelay < config.getLong(PROCESS_EMAIL_MAX_DELAY) * 1000) {
          logger.finer("Postponing processing of incoming email " + raw.getMessageId());
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
      logger.finer("Creating a new Wavelet for message " + raw.getMessageId());
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
   * Formats an email into a Blip. TODO(taton) Refactor all email processing into a single class.
   * 
   * @param message The incoming email to format.
   * @param blip The blip to write into.
   */
  private void writeMessageToBlip(Message message, Blip blip) throws MimeIOException, IOException {
    String author = null;

    if (message.getFrom() != null) {
      for (Mailbox from : message.getFrom()) {
        if (from == null)
          continue;
        if (author == null)
          author = hostingProvider.getRobotProxyForFromEmailAddress(from.getAddress());
        if (author != null)
          break;
      }
    }
    if (author == null) {
      // Discard message instead?
      author = hostingProvider.getRobotWaveId();
    }

    StringBuilder sb = new StringBuilder();
    if (message.getBody() != null) {
      StringBuilder sbBody = new StringBuilder();
      mailUtil.mimeEntityToText(sbBody, message);
      sb.append(sbBody.toString().trim());
    }

    TextView textView = blip.getDocument();
    textView.delete();
    textView.setAuthor(author);
    textView.append(sb.toString());
  }
}
