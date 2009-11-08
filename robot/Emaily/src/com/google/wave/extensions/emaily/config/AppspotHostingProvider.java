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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.james.mime4j.field.address.Mailbox;

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
  private final String appName;
  private final String appVersion;

  /**
   * Pattern used to decode a Wave participant ID encoded in the recipient address of an
   * incoming email.
   */
  private final Pattern incomingEmailAddressPattern;

  // Injected dependencies
  private final EmailyConfig emailyConfig;

  @Inject
  AppspotHostingProvider(EmailyConfig emailyConfig, Logger logger) {
    // Initialize properties
    this.emailyConfig = emailyConfig;
    emailyConfig.checkRequiredProperties(requiredProperties);
    
    // Set AppEngine properties from the environment.
    appName = ApiProxy.getCurrentEnvironment().getAppId();
    StringTokenizer versionTokenizer = new StringTokenizer(ApiProxy.getCurrentEnvironment()
        .getVersionId(), ".", false);
    appVersion = versionTokenizer.nextToken();
    logger.info("AppEngineHostingProvider initialized. AppName: " + appName + ", AppVersion: "
        + appVersion);

    // This pattern recognizes "anything+anything@app-id.appspotmail.com".
    // For now, it seems AppEngine only routes incoming email sent to these address forms.
    incomingEmailAddressPattern = Pattern.compile("(.*)\\+(.*)@" + appName + ".appspotmail.com");
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
  public String getWaveParticipantIdFromIncomingEmailAddress(String address) {
    Matcher m = incomingEmailAddressPattern.matcher(address);
    if (!m.matches())
      return null;
    return m.group(1) + "@" + m.group(2);
  }

  @Override
  public String getRobotProxyForFromEmailAddress(String address) {
    Mailbox mailbox = Mailbox.parse(address);
    return String.format("%s+%s+%s@appspot.com", appName, mailbox.getLocalPart(), mailbox
        .getDomain());
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

  @Override
  public String getWaveletEmailAddress(String emailAddressToken) {
    return emailAddressToken + "@" + appName + ".appspotmail.com";
  }

  @Override
  public String getRobotURL() {
    // TODO(dlux): add "x.latest" handling.
    return "http://" + appName + ".appspot.com/";
  }

  @Override
  public String getRobotWaveParticipantId() {
    // TODO(dlux): add "x.latest" handling.
    return appName + "@appspot.com";
  }

}