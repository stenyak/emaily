package com.google.wave.extensions.emaily.web;

import java.util.Properties;

import javax.servlet.ServletContextEvent;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
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
    super.contextInitialized(servletContextEvent);
    properties = EmailyConfig.loadProperties(servletContextEvent
        .getServletContext());
  }

  @Override
  protected Injector getInjector() {
    return Guice.createInjector(new ServletModule() {
      @Override
      protected void configureServlets() {
        serve("/_wave/robot/jsonrpc").with(EmailyRobotServlet.class);
        serve("/_wave/robot/profile").with(EmailyProfileServlet.class);
        serve("/_ah/mail/*").with(IncomingEmailServlet.class);
        Names.bindProperties(binder(), properties);
      }
    });
  }

}
