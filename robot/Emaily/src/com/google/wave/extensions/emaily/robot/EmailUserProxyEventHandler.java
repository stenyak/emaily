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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.api.RobotMessageBundle;
import com.google.wave.extensions.emaily.config.HostingProvider;

/**
 * Class that encapsulates methods which handle events for the email user proxy instance of the
 * robot. <br>
 * This class handles events for robot instances where the robot is proxying for an email user.
 * 
 * @author dlux
 * 
 */
@Singleton
public class EmailUserProxyEventHandler {
  private final HostingProvider hostingProvider;

  @Inject
  public EmailUserProxyEventHandler(HostingProvider hostingProvider) {
    this.hostingProvider = hostingProvider;
  }

  public void processEvents(RobotMessageBundle bundle) {
    // The only thing which this participant does is to add the robot itself if it is not among
    // the participants.
    if (!bundle.getWavelet().getParticipants().contains(hostingProvider.getRobotWaveId()))
      bundle.getWavelet().addParticipant(hostingProvider.getRobotWaveId());
  }
}
