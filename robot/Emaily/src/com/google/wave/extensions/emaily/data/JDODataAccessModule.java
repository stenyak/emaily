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

import java.util.logging.Logger;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class JDODataAccessModule extends AbstractModule {
  private static final Logger logger = Logger.getLogger(JDODataAccessModule.class.getSimpleName());

  @Override
  protected void configure() {
    bind(DataAccess.class).to(JDODataAccess.class);
  }

  // PersistenceManagerFactory for JDO
  @Singleton
  @Provides
  public PersistenceManagerFactory getPersistenceManagerFactory() {
    logger.info("Creating a new persistenceManagerFactory");
    System.setProperty("appengine.orm.disable.duplicate.pmf.exception", "true");
    return JDOHelper.getPersistenceManagerFactory("transactions-optional");
  }

}
