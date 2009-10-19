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
  private static final String PROD_VERSION = "hosting.appspot.prod_version";

  private final Logger logger;
  private final EmailyConfig emailyConfig;
  private final String appName;
  private final String appVersion;

  /**
   * Pattern used to decode a Wave participant ID encoded in the recipient address of an
   * incoming email.
   */
  private final Pattern incomingEmailAddressPattern;

  @Inject
  AppspotHostingProvider(EmailyConfig emailyConfig, Logger logger) {
    this.emailyConfig = emailyConfig;
    this.logger = logger;

    // Set AppEngine properties from the environment.
    appName = ApiProxy.getCurrentEnvironment().getAppId();
    StringTokenizer versionTokenizer = new StringTokenizer(ApiProxy.getCurrentEnvironment()
        .getVersionId(), ".", false);
    appVersion = versionTokenizer.nextToken();
    logger.info("AppEngineHostingProvider initialized. AppName: " + appName + ", AppVersion: "
        + appVersion);

    // For now, it seems AppEngine only routes incoming email sent to
    // "anything@app-id.appspotmail.com".
    // This pattern recognizes "anything+anything@app-id.appspotmail.com".
    // The '+' can be escaped for HTML/HTTP as "%2b".
    incomingEmailAddressPattern = Pattern.compile("(.*)(?:\\+|%2[Bb])(.*)@" + appName
        + ".appspotmail.com");
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

  @Override
  public String decodeIncomingEmailAddress(String address) {
    Matcher m = incomingEmailAddressPattern.matcher(address);
    if (!m.matches())
      return null;
    return m.group(1) + "@" + m.group(2);
  }

  @Override
  public String encodeEmailParticipantAsWaveParticipantId(String address) {
    Mailbox mailbox = Mailbox.parse(address);
    return String.format("%s+%s+%s@appspot.com", appName, mailbox.getLocalPart(), mailbox
        .getDomain());
  }

}