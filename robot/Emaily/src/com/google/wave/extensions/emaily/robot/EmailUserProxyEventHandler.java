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

import java.util.logging.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.wave.api.Blip;
import com.google.wave.api.Event;
import com.google.wave.api.EventType;
import com.google.wave.api.RobotMessageBundle;
import com.google.wave.extensions.emaily.config.HostingProvider;
import com.google.wave.extensions.emaily.data.BlipVersionView;
import com.google.wave.extensions.emaily.data.DataAccess;
import com.google.wave.extensions.emaily.data.WaveletView;
import com.google.wave.extensions.emaily.scheduler.EmailSchedulingCalculator;
import com.google.wave.extensions.emaily.util.DebugHelper;

/**
 * Class that encapsulates methods which handle events for the email user proxy instance of the
 * robot.
 * <br>
 * This class handles events for robot instances where the robot is proxying for an email user. 
 * 
 * @author dlux
 * 
 */
@Singleton
public class EmailUserProxyEventHandler {
  private final Provider<DataAccess> dataAccessProvider;
  private final EmailSchedulingCalculator emailSchedulingCalculator;
  private final Logger logger;
  private final DebugHelper debugHelper;
  private final HostingProvider hostingProvider;

  @Inject
  public EmailUserProxyEventHandler(Provider<DataAccess> dataAccessProvider,
      EmailSchedulingCalculator emailSchedulingCalculator, Logger logger, DebugHelper debugHelper,
      HostingProvider hostingProvider) {
    this.dataAccessProvider = dataAccessProvider;
    this.emailSchedulingCalculator = emailSchedulingCalculator;
    this.logger = logger;
    this.debugHelper = debugHelper;
    this.hostingProvider = hostingProvider;
  }
  
  /**
   * Process modifications for the wavelet changes and schedule sending.
   * 
   * @param bundle The robot message bundle.
   * @param email The email of the user.
   */
  void processEvents(RobotMessageBundle bundle, String email) {
    try {
      String waveId = bundle.getWavelet().getWaveId();
      String waveletId = bundle.getWavelet().getWaveletId();

      // Get or create the WaveletView for the wavelet and the current user.
      WaveletView waveletView = dataAccessProvider.get().getWaveletView(waveId, waveletId, email);
      if (waveletView == null) {
        waveletView = new WaveletView(waveId, waveletId, email, bundle.getWavelet().getRootBlipId());
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
}
