package com.google.wave.extensions.emaily.web;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.google.wave.extensions.emaily.robot.EmailyProfileServlet;
import com.google.wave.extensions.emaily.robot.EmailyRobotServlet;
import com.google.wave.extensions.emaily.robot.IncomingEmailServlet;

public class EmailyServletContextListener extends GuiceServletContextListener {

  @Override
  protected Injector getInjector() {
    return Guice.createInjector(new ServletModule() {
      @Override
      protected void configureServlets() {
        serve("/_wave/robot/jsonrpc").with(EmailyRobotServlet.class);
        serve("/_wave/robot/profile").with(EmailyProfileServlet.class);
        serve("/_ah/mail/*").with(IncomingEmailServlet.class);
      }

      // PersistenceManager
      @SuppressWarnings("unused")
      @Provides @Singleton
      public PersistenceManagerFactory getPersistenceManagerFactory() {
        return JDOHelper.getPersistenceManagerFactory("transactions-optional");
      }
    });
  }

}
