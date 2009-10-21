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

import java.util.Calendar;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.extensions.emaily.config.EmailyConfig;
import com.google.wave.extensions.emaily.data.BlipVersionView;
import com.google.wave.extensions.emaily.data.WaveletView;

// The basic idea on the basic sending schedule is the following:
// - The bot should not send emails more frequently than 10 minutes.
// - If a blip is submitted, then the bot should wait for at least one
// minute before marking the blip "sendable".
// - If a blip is edited by someone, but not changed in the last 5 minutes,
// then it is considered "sendable".
// - If a blip is constantly edited by multiple people and not sent out for at
// least
// one hour, then mark it "sendable".
// - Send an email only when there is at least one blip "sendable", and all of
// its
// parents are either "sent", or "sendable" (but not "dirty").
//
// Required concepts for making it happen:
// VaweletView:
// - LastEmailSentTime
// - TimeForSending - calculated, stored
// BlipVersionView:
// - FirstEditedTimestamp
// - LastSubmittedTimestamp
// - LastChangedTimestamp
// - TimeToBecomeSendable - calculated, not stored

/**
 * @author dlux
 */
@Singleton
public class EmailScheduler {
  // Property key names
  public static final String SEND_TIME_AFTER_BLIP_SUBMIT = "schedule.send_time_after_blip_submit";
  public static final String SEND_TIME_AFTER_BLIP_NO_EDIT = "schedule.send_time_after_blip_submit_no_edit";
  public static final String SEND_TIME_IF_CONSTANTLY_EDITED = "schedule.send_time_if_constantly_edited";
  public static final String MIN_EMAIL_SEND_TIME = "schedule.min_email_send_time";

  private static String[] requiredLongProperties = { SEND_TIME_AFTER_BLIP_SUBMIT,
      SEND_TIME_AFTER_BLIP_NO_EDIT, SEND_TIME_IF_CONSTANTLY_EDITED, MIN_EMAIL_SEND_TIME };

  private final EmailyConfig config;

  @Inject
  public EmailScheduler(EmailyConfig config) {
    this.config = config;
    config.checkRequiredLongProperties(requiredLongProperties);
  }

  /**
   * Marks event times in the given blipVersionView.
   * 
   * @param blipVersionView The blipVersionView to change.
   * @param still_editing If a user is editing the blip or not.
   */
  public void updateBlipViewTimestamps(BlipVersionView blipVersionView, Boolean still_editing) {
    long time = Calendar.getInstance().getTimeInMillis();
    if (blipVersionView.getFirstEditedTimestamp() == 0) {
      blipVersionView.setFirstEditedTimestamp(time);
    }
    if (!still_editing) {
      blipVersionView.setLastSubmittedTimestamp(time);
    }
    blipVersionView.setLastChangedTimestamp(time);
  }

  /**
   * Calculates the next send operation time of the wavelet.
   * 
   * @param waveletView The wavelet to analyze.
   */
  public void calculateWaveletViewNextSendTime(WaveletView waveletView) {
    long nextActionTime = Long.MAX_VALUE;
    for (BlipVersionView blipVersionView : waveletView.getUnsentBlips()) {
      calculateBlipTimeToBecomeSendable(blipVersionView);
      nextActionTime = Math.min(nextActionTime, blipVersionView.getTimeToBecomeSendable());
    }
    nextActionTime = Math.max(nextActionTime, waveletView.getLastEmailSentTime()
        + config.getLong(MIN_EMAIL_SEND_TIME) * 1000);
    // TODO(dlux): Add blip parent check (see description at the top) to decide
    // on sending a blip.
    waveletView.setTimeForSending(nextActionTime);
  }

  /**
   * Calculates the next email send time for a blipVersionView.
   * 
   * @param b the blipVersionView to change
   */
  private void calculateBlipTimeToBecomeSendable(BlipVersionView b) {
    long sendable;
    if (b.getLastChangedTimestamp() > b.getLastSubmittedTimestamp()) {
      // Someone is still editing:
      sendable = b.getLastChangedTimestamp() + config.getLong(SEND_TIME_AFTER_BLIP_NO_EDIT) * 1000;
    } else {
      // Submitted by all editing parties:
      sendable = b.getLastSubmittedTimestamp() + config.getLong(SEND_TIME_AFTER_BLIP_SUBMIT) * 1000;
    }
    // If constantly edited:
    sendable = Math.min(sendable, b.getFirstEditedTimestamp()
        + config.getLong(SEND_TIME_IF_CONSTANTLY_EDITED) * 1000);
    b.setTimeToBecomeSendable(sendable);
  }

}
