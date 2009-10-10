package com.google.wave.extensions.emaily.config;

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
  public static final String HOSTING_PROVIDER_MODULE = "hosting.provider_module";

  private Properties properties;

  @Inject
  EmailyConfig(@Named("ServletProperties") Properties properties) {
    this.properties = properties;
  }

  // Get properties
  public String get(String key) {
    return properties.getProperty(key);
  }

  public String get(String key, String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }
}
