package com.google.wave.extensions.emaily.web;

import com.google.wave.extensions.emaily.config.AppspotHostingProvider;
import com.google.wave.extensions.emaily.config.HostingProvider;
import com.google.wave.extensions.emaily.email.IncomingEmailServlet;

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
    bind(HostingProvider.class).to(AppspotHostingProvider.class);
  }
}
