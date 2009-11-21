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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.extensions.emaily.config.HostingProvider;
import com.google.wave.extensions.emaily.data.BlipVersionView;
import com.google.wave.extensions.emaily.data.WaveletView;
import com.google.wave.extensions.emaily.email.EmailSender;
import com.google.wave.extensions.emaily.util.DebugHelper;
import com.google.wave.extensions.emaily.util.StrUtil;

/**
 * @author dlux
 */
@Singleton
public class ScheduledEmailSender {
  // Injected dependencies
  private final HostingProvider hostingProvider;
  private final EmailSender emailSender;
  private final EmailSchedulingCalculator calculator;
  private final DebugHelper debugHelper;
  private final Logger logger;

  @Inject
  public ScheduledEmailSender(HostingProvider hostingProvider, EmailSender emailSender,
      EmailSchedulingCalculator calculator, DebugHelper debugHelper, Logger logger) {
    this.hostingProvider = hostingProvider;
    this.emailSender = emailSender;
    this.calculator = calculator;
    this.debugHelper = debugHelper;
    this.logger = logger;
  }

  /**
   * Send an email form a WaveletView.
   * @param waveletView A waveletView to generate the email from.
   */
  public void SendScheduledEmail(WaveletView waveletView) {
    long now = Calendar.getInstance().getTimeInMillis();

    // Calculate the next send time
    calculator.calculateWaveletViewNextSendTime(waveletView);

    // Make sure that we have to send the email.
    if (waveletView.getTimeForSending() >= now) {
      return;
    }

    // Collect the sendable blips and the senders.
    ArrayList<BlipVersionView> blips = new ArrayList<BlipVersionView>();
    HashMap<String, BlipVersionView> blipsToMoveToSent = new HashMap<String, BlipVersionView>();
    HashSet<String> contributors = new HashSet<String>();
    for (BlipVersionView b : waveletView.getUnsentBlips()) {
      if (b.getTimeToBecomeSendable() <= now) {
        blips.add(b);
        blipsToMoveToSent.put(b.getBlipId(), b);
        for (String participant : b.getParticipants()) {
          contributors.add(participant);
        }
      }
    }

    // Determine the sender address
    String from;
    if (contributors.size() == 1) {
      from = hostingProvider.getEmailAddressForWaveParticipantIdInEmailyDomain(contributors
          .iterator().next());
    } else {
      from = hostingProvider.getWaveletEmailAddress(waveletView.getEmailAddressToken());
    }

    // Set the subject
    String subject = waveletView.getTitle();
    if (subject.isEmpty()) subject = "(no subject)";

    // Build the body
    StringBuilder body = new StringBuilder();
    boolean first_blip = true;
    for (BlipVersionView b : blips) {
      if (b.getParticipants().size() == 1 && contributors.size() == 1
          && b.getParticipants().iterator().next().equals(contributors.iterator().next())) {
        if (!first_blip) {
          body.append("==\n");
        }
      } else {
        body.append("== From: ");
        StrUtil.join(body, b.getParticipants(), ", ");
        body.append('\n');
      }
      first_blip = false;
      body.append(b.getContent()).append('\n');
    }

    // Do the administration:
    // - Replace the already sent blips with unsent new versions.
    for (ListIterator<BlipVersionView> iter = waveletView.getSentBlips().listIterator(); iter
        .hasNext();) {
      BlipVersionView current = iter.next();
      if (blipsToMoveToSent.containsKey(current.getBlipId())) {
        BlipVersionView b = blipsToMoveToSent.remove(current.getBlipId());
        iter.set(b);
        waveletView.getUnsentBlips().remove(b);
      }
    }

    // - The new sent blips are appended to the sent blips.
    for (BlipVersionView b : blipsToMoveToSent.values()) {
      waveletView.getSentBlips().add(b);
      waveletView.getUnsentBlips().remove(b);
    }

    // - Recalculate the next send time
    calculator.calculateWaveletViewNextSendTime(waveletView);

    // Send the email
    emailSender.sendTextEmail(from, waveletView.getEmail(), subject, body.toString(), waveletView.
        getEmailAddressToken());

    // Debug info
    if (logger.isLoggable(Level.INFO)) {
      StringBuilder sb = new StringBuilder();
      sb.append("Email sent. Text: ");
      sb.append(body);
      sb.append("\nWavelet View:");
      debugHelper.printWaveletViewInfo(sb, waveletView);
      logger.info(sb.toString());
    }
  }
}
