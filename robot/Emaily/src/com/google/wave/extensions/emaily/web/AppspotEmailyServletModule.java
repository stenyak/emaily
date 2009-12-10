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

import org.apache.james.mime4j.storage.DefaultStorageProvider;
import org.apache.james.mime4j.storage.MemoryStorageProvider;

import com.google.wave.extensions.emaily.config.AppspotHostingProvider;
import com.google.wave.extensions.emaily.config.HostingProvider;
import com.google.wave.extensions.emaily.email.IncomingEmailServlet;
import com.google.wave.extensions.emaily.scheduler.EmailSchedulerServlet;

/**
 * Guice module which installs appspot-specific handlers and adds
 * appspot-specific configuration.
 * 
 * @author dlux
 * 
 */
public class AppspotEmailyServletModule extends EmailyServletModule {
  @Override
  protected void configureServlets() {
    super.configureServlets();
    serve("/_ah/mail/*").with(IncomingEmailServlet.class);
    serve("/cron/email_scheduler*").with(EmailSchedulerServlet.class);
    bind(HostingProvider.class).to(AppspotHostingProvider.class);
    
    // Set up mail4j to use only memory
    DefaultStorageProvider.setInstance(new MemoryStorageProvider());
  }
}
