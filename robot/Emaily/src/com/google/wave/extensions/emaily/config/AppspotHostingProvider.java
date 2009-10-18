package com.google.wave.extensions.emaily.config;

import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.google.apphosting.api.ApiProxy;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Hosting provider for AppEngine (appspot.com).
 * 
 * @author dlux
 * 
 */
@Singleton
public class AppspotHostingProvider implements HostingProvider {
  private static final String PROD_VERSION = "hosting.appspot.prod_version";

  private final Logger logger;
  private final EmailyConfig emailyConfig;
  private String appName;
  private String appVersion;

  @Inject
  AppspotHostingProvider(EmailyConfig emailyConfig, Logger logger) {
    this.emailyConfig = emailyConfig;
    this.logger = logger;
    initialize();
  }

  /**
   * Reads the appengine-specific configuration options from appengine-web.xml.
   */
  private void initialize() {
    // Set AppEngine properties from appengine-web.xml
    appName = ApiProxy.getCurrentEnvironment().getAppId();
    StringTokenizer versionTokenizer = new StringTokenizer(ApiProxy.getCurrentEnvironment()
        .getVersionId(), ".", false);
    appVersion = versionTokenizer.nextToken();
    logger.info("AppEngineHostingProvider initialized. AppName: " + appName + ", AppVersion: "
        + appVersion);
  }

  @Override
  public String getEmailAddressForWaveParticipantIdInEmailyDomain(String waveParticipantId) {
    int at = waveParticipantId.lastIndexOf('@');
    if (at < 0) {
      throw new RuntimeException("Invalid wave participant id: " + waveParticipantId);
    }
    StringBuilder email = new StringBuilder();
    email.append(waveParticipantId.substring(0, at)).append('+').append(
        waveParticipantId.substring(at + 1)).append('@');
    if (!appVersion.equals(emailyConfig.get(PROD_VERSION))) {
      // TODO(dlux): this is currently not working...
      // email.append(appVersion).append(".latest.");
    }
    email.append(appName).append(".appspotmail.com");
    return email.toString();
  }

  @Override
  public String getEmailAddressFromRobotProxyFor(String proxyingFor) {
    int at = proxyingFor.lastIndexOf('+');
    if (at < 0) {
      return null;
    }
    return proxyingFor.substring(0, at) + '@' + proxyingFor.substring(at + 1);
  }
}