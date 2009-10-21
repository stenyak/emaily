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
  // Configuration properties
  private static final String PROD_VERSION = "hosting.appspot.prod_version";
  private static final String[] requiredProperties = { PROD_VERSION };

  // Application name and version
  private String appName;
  private String appVersion;

  // Injected dependencies
  private final Logger logger;
  private final EmailyConfig emailyConfig;

  @Inject
  AppspotHostingProvider(EmailyConfig emailyConfig, Logger logger) {
    this.emailyConfig = emailyConfig;
    this.logger = logger;
    initialize();
  }

  /**
   * Initializes the appengine hosting provider: fills the application name version from the API.
   */
  private void initialize() {
    emailyConfig.checkRequiredProperties(requiredProperties);
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
    if (!isProductionVersion()) {
      // TODO(dlux): this is currently not working...
      // email.append(appVersion).append(".latest.");
    }
    email.append(appName).append(".appspotmail.com");
    return email.toString();
  }

  @Override
  public String getEmailAddressFromRobotProxyFor(String proxyingFor) {
    if (proxyingFor == null) {
      return null;
    }
    int at = proxyingFor.lastIndexOf('+');
    if (at < 0) {
      return null;
    }
    return proxyingFor.substring(0, at) + '@' + proxyingFor.substring(at + 1);
  }

  @Override
  public String getRobotWaveParticipantIdFromEmailAddress(String email) {
    int at = email.lastIndexOf('@');
    if (at < 0) {
      throw new RuntimeException("Invalid email address: " + email);
    }
    StringBuilder waveParticipantId = new StringBuilder();
    waveParticipantId.append(email.substring(0, at)).append('+').append(email.substring(at + 1))
        .append('@');
    if (!isProductionVersion()) {
      waveParticipantId.append(appVersion).append(".latest.");
    }
    waveParticipantId.append(appName).append(".appspot.com");
    return waveParticipantId.toString();
  }

  @Override
  public boolean isProductionVersion() {
    return appVersion.equals(emailyConfig.get(PROD_VERSION));
  }

}