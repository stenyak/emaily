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
package com.google.wave.extensions.emaily.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.wave.extensions.emaily.data.JDODataAccessModule;

public class EmailyServletContextListener extends GuiceServletContextListener {
  private Properties properties;

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    properties = loadProperties(servletContextEvent.getServletContext());
    super.contextInitialized(servletContextEvent);
  }

  @Override
  protected Injector getInjector() {
    EmailyServletModule servletModule = new AppspotEmailyServletModule();
    servletModule.setServletProperties(properties);
    return Guice.createInjector(new JDODataAccessModule(), servletModule);
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
}
