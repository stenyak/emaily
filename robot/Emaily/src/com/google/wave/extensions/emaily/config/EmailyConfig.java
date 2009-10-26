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
package com.google.wave.extensions.emaily.config;

import java.util.Properties;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Class for configuring Emaily. This class provides a mechanism for retrieving configuration
 * properties.
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

  /**
   * Checks whether the given property names are defined in the configuration or not. It can be used
   * to check early (at binding time) whether a configuration value exist or not.
   * 
   * @param propertyNames The array of property names to check.
   */
  public void checkRequiredProperties(String[] propertyNames) {
    for (String propertyName : propertyNames) {
      if (!properties.keySet().contains(propertyName)) {
        throw new RuntimeException("Property '" + propertyName + "' is required and not defined");
      }
    }
  }

  /**
   * Checks whether the given property names are defined and can be interpreted as a long value. It
   * can be used to check the proper
   * configuration early.
   * 
   * @param propertyNames
   */
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

  /**
   * Retuns a configuration property as a string.
   * 
   * @param key The key of the property.
   * @return The value of the property.
   */
  public String get(String key) {
    return properties.getProperty(key);
  }

  /**
   * Returns a configuration property as a long value.
   * 
   * @param key The key of the property.
   * @return The value of the property converted to long.
   */
  public long getLong(String key) {
    return Long.parseLong(properties.getProperty(key));
  }

  /**
   * Returns a configuration property with a default value.
   * 
   * @param key The key of the property.
   * @param defaultValue The default value.
   * @return The value of the key or the default value if it does not exist.
   */
  public String get(String key, String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }
}
