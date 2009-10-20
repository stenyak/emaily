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
  private Properties properties;

  @Inject
  EmailyConfig(@Named("ServletProperties") Properties properties) {
    this.properties = properties;
  }

  public void checkRequiredProperties(String[] propertyNames) {
    for (String propertyName : propertyNames) {
      if (!properties.keySet().contains(propertyName)) {
        throw new RuntimeException("Property '" + propertyName + "' is required and not defined");
      }
    }
  }

  public void checkRequiredLongProperties(String[] propertyNames) {
    checkRequiredProperties(propertyNames);
    for (String propertyName : propertyNames) {
      try {
        Long.parseLong(properties.getProperty(propertyName));
      } catch (NumberFormatException e) {
        throw new RuntimeException("Property '" + propertyName + "' is not a valid long value", e);
      }
    }
  }

  public String get(String key) {
    return properties.getProperty(key);
  }

  public long getLong(String key) {
    return Long.parseLong(properties.getProperty(key));
  }

  public String get(String key, String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }
}
