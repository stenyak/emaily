package com.google.wave.extensions.emaily.scheduler;

import com.google.inject.Singleton;
import com.google.wave.extensions.emaily.data.BlipVersionView;
import com.google.wave.extensions.emaily.data.WaveletView;

@Singleton
public class EmailScheduler {
  /**
   * Schedule the next send time.
   * 
   * @param waveletView The wavelet view to modify.
   * @param blipVersionView The blip version view, which has an event.
   * @param still_editing Whether or not someone is currently editing the given
   *          blip or not.
   */
  public void ScheduleNextSend(WaveletView waveletView, BlipVersionView blipVersionView,
      Boolean still_editing) {
    // The basic idea on the basic sending schedule is the following:
    // - The bot should not send emails more frequently than 10 minutes.
    // - If a blip is submitted, then the bot should wait for at least one
    //   minute before marking the blip "sendable".
    // - If a blip is edited by someone, but not changed in the last 5 minutes,
    //   then it is considered "sendable".
    // - If a blip is constantly edited by multiple people and not sent out for at least
    //   one hour, then mark it "sendable".
    // - Send an email only when there is at least one blip "sendable", and all of its
    //   parents are either "sent", or "sendable" (but not "dirty").
    //
    // Required concepts for making it happen:
    // VaweletView:
    // - LastEmailSentTime
    // - TimeForSending - calculated
    // BlipVersionView:
    // - FirstEditedTimestamp
    // - LastSubmittedTimestamp
    // - LastChangedTimestamp
    // - TimeToBecomeSendable - calculated
  }
}
