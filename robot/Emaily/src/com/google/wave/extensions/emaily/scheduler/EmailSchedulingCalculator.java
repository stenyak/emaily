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
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.extensions.emaily.config.EmailyConfig;
import com.google.wave.extensions.emaily.config.HostingProvider;
import com.google.wave.extensions.emaily.data.BlipData;
import com.google.wave.extensions.emaily.data.WaveletData;

// The idea on the basic sending schedule is the following:
// - The bot should not send emails more frequently than 10 minutes.
// - If a blip is submitted, then the bot should wait for at least one minute before marking the
//   blip "sendable".
// - If a blip is edited by someone, but not changed in the last 5 minutes, then it is considered
//   "sendable".
// - If a blip is constantly edited by multiple people and not sent out for at least one hour,
//   then mark it "sendable".
// - Send an email only when there is at least one blip "sendable", and all of its parents are
//   either "sent", or "sendable" (but not "dirty").
// - If a blip content is not changed, then do not resend the blip as email.
//
// Required concepts for making it happen:
// VaweletData:
// - LastEmailSentTime
// - TimeForSending - calculated, stored
// BlipData:
// - FirstEditedTimestamp
// - LastSubmittedTimestamp
// - LastChangedTimestamp
// - TimeToBecomeSendable - calculated, not stored

/**
 * @author dlux
 */
@Singleton
public class EmailSchedulingCalculator {
  // Property key names for tuning email scheduling. The values are in seconds.
  private static final String SEND_TIME_AFTER_BLIP_SUBMIT = "schedule.send_time_after_blip_submit";
  private static final String SEND_TIME_AFTER_BLIP_NO_EDIT = "schedule.send_time_after_blip_submit_no_edit";
  private static final String SEND_TIME_IF_CONSTANTLY_EDITED = "schedule.send_time_if_constantly_edited";
  private static final String MIN_EMAIL_SEND_TIME = "schedule.min_email_send_time";

  private static String[] requiredLongProperties = { SEND_TIME_AFTER_BLIP_SUBMIT,
      SEND_TIME_AFTER_BLIP_NO_EDIT, SEND_TIME_IF_CONSTANTLY_EDITED, MIN_EMAIL_SEND_TIME };

  // Injected dependencies
  private final EmailyConfig config;
  private final HostingProvider hostingProvider;

  @Inject
  public EmailSchedulingCalculator(EmailyConfig config, HostingProvider hostingProvider) {
    this.config = config;
    this.hostingProvider = hostingProvider;
    config.checkRequiredLongProperties(requiredLongProperties);
  }

  /**
   * Update event times to <i>now</i> in the given BlipData.
   * 
   * @param blipData The blipData to change.
   * @param stillEditing If a user is editing the blip or not.
   * @param manualSendRequest True, if manual send is triggered through the "Send Email" button.
   */
  public void updateBlipDataTimestamps(BlipData blipData, boolean stillEditing,
      boolean manualSendRequest) {
    long time = Calendar.getInstance().getTimeInMillis();
    if (manualSendRequest)
      blipData.setManualSendRequestTimestamp(time);
    if (blipData.getFirstEditedTimestamp() == 0)
      blipData.setFirstEditedTimestamp(time);
    if (!stillEditing)
      blipData.setLastSubmittedTimestamp(time);
    blipData.setLastChangedTimestamp(time);
  }

  /**
   * Calculates the next send operation time of the wavelet and store it into the
   * <code>timeForSending</code> field.
   * 
   * @param waveletData The wavelet to analyze.
   */
  public void calculateWaveletDataNextSendTime(WaveletData waveletData) {
    long nextActionTime = Long.MAX_VALUE;
    for (BlipData blipData : waveletData.getUnsentBlips()) {
      calculateBlipTimeToBecomeSendable(waveletData, blipData);
      nextActionTime = Math.min(nextActionTime, blipData.getTimeToBecomeSendable());
    }
    nextActionTime = Math.max(nextActionTime, waveletData.getLastEmailSentTime()
        + config.getLong(MIN_EMAIL_SEND_TIME) * 1000);
    // TODO(dlux): Add blip parent check (see description at the top) to decide
    // on sending a blip.
    waveletData.setTimeForSending(nextActionTime);
  }

  /**
   * Calculates the next email send time for a BlipData and update the timeToBecomeSendable field
   * with this value.
   * 
   * @param b the blipData to be calculated and updated.
   */
  private void calculateBlipTimeToBecomeSendable(WaveletData waveletData, BlipData b) {
    long sendable;
    switch (waveletData.getSendMode()) {
    case MANUAL:
      // Manual sending mode: we send immediately if the user pressed the "Send" button
      // but won't send at all if he didn't.
      if (b.getManualSendRequestTimestamp() >= b.getLastChangedTimestamp())
        sendable = 1;
      else
        sendable = Long.MAX_VALUE;
      break;
    case AUTOMATIC:
      if (b.getLastChangedTimestamp() > b.getLastSubmittedTimestamp()) {
        // Someone is still editing:
        sendable = b.getLastChangedTimestamp() + config.getLong(SEND_TIME_AFTER_BLIP_NO_EDIT)
            * 1000;
      } else {
        // Submitted by all editing parties:
        sendable = b.getLastSubmittedTimestamp() + config.getLong(SEND_TIME_AFTER_BLIP_SUBMIT)
            * 1000;
      }
      // If constantly edited:
      sendable = Math.min(sendable, b.getFirstEditedTimestamp()
          + config.getLong(SEND_TIME_IF_CONSTANTLY_EDITED) * 1000);
      break;
    default:
      throw new RuntimeException("Invalid Send Mode");
    }
    b.setTimeToBecomeSendable(sendable);
  }

  /**
   * Returns a list of email recipients who are interested in the change of this blip.
   * 
   * The list is generated by getting all the email recipients of the wave and excluding
   * the blip creator.
   * 
   * @param blipCreator the creator of the blip.
   * @param waveParticipants Participants on the wave.
   * @return The list of email addresses that are interested in this wave.
   */
  public List<String> calculateInterestedEmailRecipientsForBlip(String blipCreator,
      List<String> waveParticipants) {
    List<String> interestedEmailRecipiens = new ArrayList<String>();
    for (String participant : waveParticipants) {
      if (blipCreator.equals(participant))
        continue;
      String emailUser = hostingProvider.getEmailAddressFromRobotProxyForWaveId(participant);
      if (emailUser != null) {
        interestedEmailRecipiens.add(emailUser);
      }
    }
    return interestedEmailRecipiens;
  }
}
