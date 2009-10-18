package com.google.wave.extensions.emaily.web;

import java.util.Properties;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.servlet.ServletModule;
import com.google.wave.extensions.emaily.robot.EmailyProfileServlet;
import com.google.wave.extensions.emaily.robot.EmailyRobotServlet;

/**
 * Guice Servlet module for Emaily. This class contains the basic providers and
 * service definitions for Emaily. It does not contain hosting specific code.
 * 
 * @author dlux
 * 
 */
public class EmailyServletModule extends ServletModule {
  /**
   * Servlet properties which are set by the servlet context listener and read
   * by by EmailyConfig. This is not injected, since the data in properties can
   * be used for injection configuration.
   */
  private Properties servletProperties;

  @Override
  protected void configureServlets() {
    serve("/_wave/robot/jsonrpc").with(EmailyRobotServlet.class);
    serve("/_wave/robot/profile").with(EmailyProfileServlet.class);
  }

  void setServletProperties(Properties servletProperties) {
    this.servletProperties = servletProperties;
  }

  @Provides
  @Named("ServletProperties")
  public Properties getServletProperties() {
    return servletProperties;
  }

  @Provides @Singleton
  public PersistenceManagerFactory getPersistenceManagerFactory() {
    return JDOHelper.getPersistenceManagerFactory("transactions-optional");
  }
}
