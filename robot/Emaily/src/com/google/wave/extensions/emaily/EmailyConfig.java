package com.google.wave.extensions.emaily;

import java.util.Properties;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Class for configuring Emaily.
 * 
 * @author dlux
 * 
 */
@Singleton
public class EmailyConfig {
  // Constants for property names
  public static final String APPENGINE_APP_NAME = "appengine.app_name";
  public static final String APPENGINE_APP_VERSION = "appengine.app_version";

  private Properties properties;
  
  @Inject
  EmailyConfig(@Named("ServletProperties") Properties properties) {
    this.properties = properties;
  }
  
  public String get(String key) {
    return properties.getProperty(key);
  }
  
  public String get(String key, String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }
}
