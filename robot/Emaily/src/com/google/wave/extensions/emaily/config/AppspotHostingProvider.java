package com.google.wave.extensions.emaily.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

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
  private final ServletContext servletContext;
  private final EmailyConfig emailyConfig;
  private String appName;
  private String appVersion;

  @Inject
  AppspotHostingProvider(ServletContext servletContext, EmailyConfig emailyConfig, Logger logger) {
    this.servletContext = servletContext;
    this.emailyConfig = emailyConfig;
    this.logger = logger;
    readConfiguration();
  }

  /**
   * Reads the appengine-specific configuration options from appengine-web.xml.
   */
  private void readConfiguration() {
    // Set AppEngine properties from appengine-web.xml
    InputStream appengineXmlInputStream = servletContext
        .getResourceAsStream("/WEB-INF/appengine-web.xml");
    try {
      Document appengineXml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
          appengineXmlInputStream);
      XPath xPath = XPathFactory.newInstance().newXPath();
      appName = (String) xPath.compile("//*[local-name()='application']").evaluate(appengineXml,
          XPathConstants.STRING);
      appVersion = (String) xPath.compile("//*[local-name()='version']").evaluate(appengineXml,
          XPathConstants.STRING);
      logger.info("AppEngineHostingProvider initialized. AppName: " + appName + ", AppVersion: "
          + appVersion);
    } catch (SAXException e) {
      throw new RuntimeException("Cannot parse appengine-web.xml", e);
    } catch (IOException e) {
      throw new RuntimeException("Error reading appengine-web.xml", e);
    } catch (ParserConfigurationException e) {
      throw new RuntimeException("Error during XML parsing", e);
    } catch (XPathExpressionException e) {
      throw new RuntimeException("Error during XML parsing", e);
    }
  }

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

  public String getEmailAddressFromRobotProxyFor(String proxyingFor) {
    int at = proxyingFor.lastIndexOf('+');
    if (at < 0) {
      return null;
    }
    return proxyingFor.substring(0, at) + '@' + proxyingFor.substring(at + 1);
  }

  public String getRobotWaveParticipantIdFromEmailAddress(String email) {
    int at = email.lastIndexOf('@');
    if (at < 0) {
      throw new RuntimeException("Invalid email address: " + email);
    }
    StringBuilder waveParticipantId = new StringBuilder();
    waveParticipantId.append(email.substring(0, at)).append('+').append(email.substring(at + 1))
        .append('@');
    if (!appVersion.equals(emailyConfig.get(PROD_VERSION))) {
      waveParticipantId.append(appVersion).append(".latest.");
    }
    waveParticipantId.append(appName).append(".appspot.com");
    return waveParticipantId.toString();
  }
}