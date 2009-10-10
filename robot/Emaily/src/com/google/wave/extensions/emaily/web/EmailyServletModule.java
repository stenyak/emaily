package com.google.wave.extensions.emaily.web;

import java.util.Properties;

import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.servlet.ServletModule;
import com.google.wave.extensions.emaily.robot.EmailyProfileServlet;
import com.google.wave.extensions.emaily.robot.EmailyRobotServlet;

public class EmailyServletModule extends ServletModule {
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

}
