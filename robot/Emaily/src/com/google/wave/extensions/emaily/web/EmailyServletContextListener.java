package com.google.wave.extensions.emaily.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.google.wave.extensions.emaily.config.EmailyConfig;
import com.google.wave.extensions.emaily.robot.EmailyProfileServlet;
import com.google.wave.extensions.emaily.robot.EmailyRobotServlet;

public class EmailyServletContextListener extends GuiceServletContextListener {
  private Properties properties;
  private AbstractModule hostingProviderModule;

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    properties = loadProperties(servletContextEvent.getServletContext());
    hostingProviderModule = getHostingProviderModule(properties);
    super.contextInitialized(servletContextEvent);
  }

  @Override
  protected Injector getInjector() {
    return Guice.createInjector(hostingProviderModule, new ServletModule() {
      @Override
      protected void configureServlets() {
        serve("/_wave/robot/jsonrpc").with(EmailyRobotServlet.class);
        serve("/_wave/robot/profile").with(EmailyProfileServlet.class);
      }

      @SuppressWarnings("unused")
      @Provides
      @Named("ServletProperties")
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
  public static Properties loadProperties(ServletContext servletContext) {
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
    return props;
  }

  private static AbstractModule getHostingProviderModule(Properties properties) {
    try {
      return (AbstractModule) Class.forName(
          properties.getProperty(EmailyConfig.HOSTING_PROVIDER_MODULE)).newInstance();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Cannot load hosting provider", e);
    } catch (InstantiationException e) {
      throw new RuntimeException("Cannot instantiate hosting provider module", e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Cannot access constructor of hosting module", e);
    }
  }

}
