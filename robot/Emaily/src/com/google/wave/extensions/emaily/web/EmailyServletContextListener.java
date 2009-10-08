package com.google.wave.extensions.emaily.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.google.wave.extensions.emaily.EmailyConfig;
import com.google.wave.extensions.emaily.robot.EmailyProfileServlet;
import com.google.wave.extensions.emaily.robot.EmailyRobotServlet;
import com.google.wave.extensions.emaily.robot.IncomingEmailServlet;

public class EmailyServletContextListener extends GuiceServletContextListener {
  // Properties loaded
  private Properties properties;

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    properties = loadProperties(servletContextEvent.getServletContext());
    super.contextInitialized(servletContextEvent);
  }

  @Override
  protected Injector getInjector() {
    return Guice.createInjector(new ServletModule() {
      @Override
      protected void configureServlets() {
        serve("/_wave/robot/jsonrpc").with(EmailyRobotServlet.class);
        serve("/_wave/robot/profile").with(EmailyProfileServlet.class);
        serve("/_ah/mail/*").with(IncomingEmailServlet.class);
      }
      
      @SuppressWarnings("unused")
      @Provides @Named("ServletProperties")
      public Properties getEmailyProperties() {
        return properties;
      }
           
    });
  }

  /**
   * Loads properties from the global and local properties files and returns
   * them as a properties object.
   * 
   * @param servletContext
   * @return
   */
  public Properties loadProperties(ServletContext servletContext) {
    // Load the global emaily.properties file
    Properties props = new Properties();
    InputStream globalPropInputStream = servletContext
        .getResourceAsStream("/WEB-INF/emaily.properties");
    try {
      props.load(globalPropInputStream);
    } catch (IOException e) {
      throw new RuntimeException("Cannot load emaily.properties", e);
    }
    // Load the local (optional) properties file (to override global properties)
    InputStream localPropInputStream = servletContext
        .getResourceAsStream("/WEB-INF/emaily-local.properties");
    if (localPropInputStream != null) {
      try {
        props.load(localPropInputStream);
      } catch (IOException e) {
        throw new RuntimeException("Cannot load emaily-local.properties");
      }
    }
    // Set AppEngine properties from appengine-web.xml
    InputStream appengineXmlInputStream = servletContext
        .getResourceAsStream("/WEB-INF/appengine-web.xml");
    if (appengineXmlInputStream != null) {
      try {
        Document appengineXml = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder().parse(appengineXmlInputStream);
        XPath xPath = XPathFactory.newInstance().newXPath();
        Object appName = xPath.compile("//*[local_name()='application']")
            .evaluate(appengineXml, XPathConstants.STRING);
        props.setProperty(EmailyConfig.APPENGINE_APP_NAME, (String) appName);
        Object appVersion = xPath.compile("//*[local_nam,e()='version']")
            .evaluate(appengineXml, XPathConstants.STRING);
        props.setProperty(EmailyConfig.APPENGINE_APP_VERSION, (String) appVersion);
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
    return props;
  }
}
