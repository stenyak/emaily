package com.google.wave.extensions.emaily.robot;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.api.ParticipantProfile;
import com.google.wave.api.ProfileServlet;
import com.google.wave.extensions.emaily.config.EmailyConfig;
import com.google.wave.extensions.emaily.config.HostingProvider;

/**
 * Servlet to return the profile information for the robot.
 * 
 * @author dlux
 */
@Singleton
public class EmailyProfileServlet extends ProfileServlet {
  // Config parameters
  private static final String LOGO_URL = "profile.logo_url";
  private static final String ROBOT_NAME_PROD = "profile.robot_name.prod";
  private static final String ROBOT_NAME_NONPROD = "profile.robot_name.nonprod";

  private static final String[] requiredProperties = { LOGO_URL, ROBOT_NAME_PROD,
      ROBOT_NAME_NONPROD };

  private final EmailyConfig emailyConfig;
  private final HostingProvider hostingProvider;

  @Inject
  public EmailyProfileServlet(EmailyConfig emailyConfig, HostingProvider hostingProvider) {
    this.emailyConfig = emailyConfig;
    this.hostingProvider = hostingProvider;
    emailyConfig.checkRequiredProperties(requiredProperties);
  }

  @Override
  public String getRobotAvatarUrl() {
    // TODO(dlux): Find someone who can create an avatar for this project.
    return emailyConfig.get(LOGO_URL);
  }

  @Override
  public String getRobotName() {
    return emailyConfig.get(hostingProvider.isProductionVersion() ? ROBOT_NAME_PROD
        : ROBOT_NAME_NONPROD);
  }

  @Override
  public ParticipantProfile getCustomProfile(String proxyingFor) {
    String email = hostingProvider.getEmailAddressFromRobotProxyFor(proxyingFor);
    return new ParticipantProfile(email + " (" + getRobotName() + ")", getRobotAvatarUrl(),
        getRobotProfilePageUrl());
  }
}
