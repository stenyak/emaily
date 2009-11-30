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
import com.google.inject.Singleton;
import com.google.wave.api.ParticipantProfile;
import com.google.wave.api.ProfileServlet;
import com.google.wave.extensions.emaily.config.EmailyConfig;
import com.google.wave.extensions.emaily.config.HostingProvider;

/**
 * Servlet to return the profile information for a robot and users in the robot domain.
 * 
 * @author dlux
 */
@Singleton
public class EmailyProfileServlet extends ProfileServlet {
  // Config properties
  private static final String ROBOT_NAME_PROD = "profile.robot_name.prod";
  private static final String ROBOT_NAME_NONPROD = "profile.robot_name.nonprod";
  private static final String ROBOT_AVATAR_REL_URL = "img/emaily-robot.jpg";
  private static final String EMAIL_PARTICIPANT_AVATAR_REL_URL = "img/unknown-email.jpg";

  private static final String[] requiredProperties = { ROBOT_NAME_PROD, ROBOT_NAME_NONPROD };

  // Injected dependencies
  private final EmailyConfig emailyConfig;
  private final HostingProvider hostingProvider;
  private final Logger logger;

  @Inject
  public EmailyProfileServlet(EmailyConfig emailyConfig, HostingProvider hostingProvider,
      Logger logger) {
    this.emailyConfig = emailyConfig;
    this.hostingProvider = hostingProvider;
    this.logger = logger;
    emailyConfig.checkRequiredProperties(requiredProperties);
  }

  @Override
  public String getRobotAvatarUrl() {
    // TODO(dlux): Find someone who can create an avatar for this project.
    return hostingProvider.getRobotURL() + ROBOT_AVATAR_REL_URL;
  }

  private String getPariticipantAvatarUrl() {
    return hostingProvider.getRobotURL() + EMAIL_PARTICIPANT_AVATAR_REL_URL;
  }

  @Override
  public String getRobotName() {
    return emailyConfig.get(hostingProvider.isProductionVersion() ? ROBOT_NAME_PROD
        : ROBOT_NAME_NONPROD);
  }

  @Override
  public String getRobotProfilePageUrl() {
    return hostingProvider.getRobotURL();
  };

  @Override
  public ParticipantProfile getCustomProfile(String proxyingFor) {
    // Workaround for buggy API.
    proxyingFor = proxyingFor.replaceAll(" ", "+");
    String email = hostingProvider.getEmailAddressFromRobotProxyFor(proxyingFor);
    logger.fine("Profile queried: " + proxyingFor + ", email: " + email);
    return new ParticipantProfile(email + " (" + getRobotName() + ")", getPariticipantAvatarUrl(),
        getRobotProfilePageUrl());
  }
}
