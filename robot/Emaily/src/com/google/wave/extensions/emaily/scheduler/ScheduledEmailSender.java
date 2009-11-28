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

package com.google.wave.extensions.emaily.scheduler;

import static com.google.wave.extensions.emaily.config.AppspotHostingProvider.OUTGOING_EMAIL_PREFIX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.extensions.emaily.config.HostingProvider;
import com.google.wave.extensions.emaily.data.BlipData;
import com.google.wave.extensions.emaily.data.PersistentEmail;
import com.google.wave.extensions.emaily.data.WaveletData;
import com.google.wave.extensions.emaily.email.EmailSender;
import com.google.wave.extensions.emaily.email.EmailSender.EmailSendingException;
import com.google.wave.extensions.emaily.util.DebugHelper;
import com.google.wave.extensions.emaily.util.StrUtil;

/**
 * @author dlux
 */
@Singleton
public class ScheduledEmailSender {
  // Injected dependencies
  private final DebugHelper debugHelper;
  private final EmailSender emailSender;
  private final EmailSchedulingCalculator calculator;
  private final HostingProvider hostingProvider;
  private final Logger logger;
  private final PersistenceManagerFactory pmFactory;

  @Inject
  public ScheduledEmailSender(HostingProvider hostingProvider, EmailSender emailSender,
      EmailSchedulingCalculator calculator, DebugHelper debugHelper, Logger logger,
      PersistenceManagerFactory pmFactory) {
    this.hostingProvider = hostingProvider;
    this.emailSender = emailSender;
    this.calculator = calculator;
    this.debugHelper = debugHelper;
    this.logger = logger;
    this.pmFactory = pmFactory;
  }

  /**
   * Send email(s) from a WaveletData.
   * 
   * @param waveletData A waveletData object to generate the emails.
   */
  public void sendScheduledEmails(WaveletData waveletData) {
    long now = Calendar.getInstance().getTimeInMillis();

    logger.info(String.format("Sending email from wavelet: %s %s", waveletData.getWaveId(),
        waveletData.getWaveletId()));
    if (logger.isLoggable(Level.FINER))
      logger.finer(debugHelper.printWaveletDataInfo(waveletData));

    // Calculate the next send time
    calculator.calculateWaveletDataNextSendTime(waveletData);

    // Make sure that we have to send the email.
    if (waveletData.getTimeForSending() >= now) {
      logger.fine("Email sending time is not reached.");
      return;
    }

    // Set the subject
    String subject = waveletData.getTitle();
    if (subject.isEmpty())
      subject = "(no subject)";

    HashMap<String, List<BlipData>> sendableBlipsByEmail = new HashMap<String, List<BlipData>>();
    List<BlipData> sendableBlipList = new ArrayList<BlipData>();
    collectSendableBlips(waveletData, now, sendableBlipsByEmail, sendableBlipList);

    // Print debug info:
    if (logger.isLoggable(Level.FINER)) {
      StringBuilder sb = new StringBuilder("Sendable blips by email:");
      for (String email : sendableBlipsByEmail.keySet()) {
        sb.append("\n - ").append(email).append(": ");
        StrUtil.join(sb, sendableBlipsByEmail.get(email), ", ");
      }
      sb.append("\nSendable blip list: ");
      StrUtil.join(sb, sendableBlipList, ",");
      logger.finer(sb.toString());
    }

    // Assemble the emails for each email recipient.
    for (String email : sendableBlipsByEmail.keySet()) {
      assembleAndSendEmail(email, subject, waveletData, sendableBlipsByEmail);
    }

    // Do the administration:
    // - The sent blips are removed from the sendable blips.
    for (BlipData b : sendableBlipList) {
      waveletData.getUnsentBlips().remove(b);
    }

    // - Recalculate the next send time.
    calculator.calculateWaveletDataNextSendTime(waveletData);
  }

  /**
   * Collect sendable blips from a WaveletData.
   * 
   * @param waveletData The wavelet data object to collect form.
   * @param currentTime Current time in ms.
   * @param sendableBlipsByEmail A map which contains the sendable blips by email address. The key
   *          is the email address to send to, the value is a list of blips to send to that email
   *          address.
   * @param sendableBlipList A list, that contains all sendable blips.
   */
  private void collectSendableBlips(WaveletData waveletData, long currentTime,
      Map<String, List<BlipData>> sendableBlipsByEmail, List<BlipData> sendableBlipList) {
    // Collecting all the blips to send out by email address.
    for (BlipData b : waveletData.getUnsentBlips()) {
      if (b.getTimeToBecomeSendable() >= currentTime)
        continue;
      boolean isBlipSendable = false;
      for (String email : calculator.calculateInterestedEmailRecipientsForBlip(b.getCreator(),
          waveletData.getParticipants())) {
        if (!sendableBlipsByEmail.containsKey(email))
          sendableBlipsByEmail.put(email, new ArrayList<BlipData>());
        sendableBlipsByEmail.get(email).add(b);
        isBlipSendable = true;
      }
      if (isBlipSendable == false)
        throw new IllegalArgumentException("Blip should be sendable, but not: " + b.getBlipId());
      sendableBlipList.add(b);
    }
  }

  /**
   * Assemble and send one email from a wavelet to an email recipient.
   * 
   * @param email The email address of the user.
   * @param subject The subject of the email.
   * @param waveletData The wavelet data object.
   * @param sendableBlipsByEmail The list of the blips to send out.
   */
  private void assembleAndSendEmail(String email, String subject, WaveletData waveletData,
      HashMap<String, List<BlipData>> sendableBlipsByEmail) {
    logger.fine(String.format("Assembling email to: %s", email));

    List<BlipData> blipList = sendableBlipsByEmail.get(email);

    // Collect the contributors.
    HashSet<String> contributors = new HashSet<String>();
    for (BlipData b : blipList)
      for (String contributor : b.getContributors())
        contributors.add(contributor);
    logger.finer("Contributors: " + StrUtil.join(contributors, ","));

    // Determine the sender address
    String from;
    if (contributors.size() == 1) {
      from = hostingProvider.getEmailAddressForWaveParticipantIdInEmailyDomain(contributors
          .iterator().next());
    } else {
      from = hostingProvider.getWaveletEmailAddress(waveletData.getEmailAddressToken());
    }
    logger.finer("Sender address: " + from);

    // Build the body
    StringBuilder body = new StringBuilder();
    boolean firstBlip = true;
    for (BlipData b : blipList) {
      if (b.getContributors().size() == 1 && contributors.size() == 1
          && b.getContributors().iterator().next().equals(contributors.iterator().next())) {
        if (!firstBlip) {
          body.append("==\n");
        }
      } else {
        body.append("== From: ");
        StrUtil.join(body, b.getContributors(), ", ");
        body.append('\n');
      }
      firstBlip = false;
      body.append(b.getContent()).append('\n');

    }

    // Send the email
    sendEmail(from, email, subject, body.toString(), waveletData);

    // Debug info
    if (logger.isLoggable(Level.INFO)) {
      StringBuilder sb = new StringBuilder();
      sb.append("Email sent. To:").append(email);
      sb.append("\nText: ");
      sb.append(body);
      sb.append("\nWavelet Data:");
      debugHelper.printWaveletDataInfo(sb, waveletData);
      logger.info(sb.toString());
    }
  }

  /**
   * Sends an email and stores a matching PersistentEmail with a temporary Message ID to be filled
   * when we receive the BCC'ed email.
   */
  private void sendEmail(String from, String recipient, String subject, String body,
      WaveletData waveletData) {
    final String temporaryMessageId = OUTGOING_EMAIL_PREFIX + waveletData.getEmailAddressToken();

    PersistentEmail email = new PersistentEmail(temporaryMessageId, new HashSet<String>(),
        new HashSet<String>(waveletData.getParticipants()));
    email.setWaveAndWaveletId(waveletData.getWaveId(), waveletData.getWaveletId());

    List<String> recipients = new ArrayList<String>();
    recipients.add(recipient);

    List<String> bcc = new ArrayList<String>();
    bcc.add(hostingProvider.getWaveletEmailAddress(temporaryMessageId));

    PersistenceManager pm = pmFactory.getPersistenceManager();
    Transaction tx = pm.currentTransaction();
    try {
      emailSender.sendTextEmail(from, recipients, bcc, subject, body);
      tx.begin();
      pm.makePersistent(email);
      tx.commit();
    } finally {
      if (tx.isActive())
        tx.rollback();
      pm.close();
    }
  }

}
