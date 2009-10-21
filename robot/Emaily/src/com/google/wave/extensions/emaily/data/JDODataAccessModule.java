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
