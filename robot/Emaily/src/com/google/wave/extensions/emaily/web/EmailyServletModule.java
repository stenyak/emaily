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

import java.util.Properties;

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
  @Singleton
  @Named("ServletProperties")
  public Properties getServletProperties() {
    return servletProperties;
  }
  
}
