package com.google.wave.extensions.emaily.data;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class DataAccessModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(DataAccess.class).to(JDODataAccess.class);
  }

  // PersistenceManagerFactory for JDO
  @Provides @Singleton
  public PersistenceManagerFactory getPersistenceManagerFactory() {
    return JDOHelper.getPersistenceManagerFactory("transactions-optional");
  }


}
