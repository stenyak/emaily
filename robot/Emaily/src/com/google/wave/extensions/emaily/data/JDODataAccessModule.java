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
package com.google.wave.extensions.emaily.data;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Guice Module for configuring data access.
 * 
 * @author dlux
 * 
 */
public class JDODataAccessModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(DataAccess.class).to(JDODataAccess.class);
  }

  // PersistenceManagerFactory for JDO
  @Singleton
  @Provides
  public PersistenceManagerFactory getPersistenceManagerFactory() {
    return JDOHelper.getPersistenceManagerFactory("transactions-optional");
  }

}
