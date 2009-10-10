package com.google.wave.extensions.emaily.config;

import com.google.inject.servlet.ServletModule;
import com.google.wave.extensions.emaily.robot.IncomingEmailServlet;

/**
 * Guice module which installs appspot-specific handlers and adds
 * appspot-specific configuration.
 * 
 * @author dlux
 * 
 */
public class AppspotHostingProviderModule extends ServletModule {
  @Override
  protected void configureServlets() {
    serve("/_ah/mail/*").with(IncomingEmailServlet.class);
    bind(HostingProvider.class).to(AppspotHostingProvider.class);
  }
}
