package com.google.wave.extensions.emaily.robot;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

/**
 * The Persistence manager factory used by Emaily.
 * 
 * @author taton
 */
public final class PMF {
  private static final PersistenceManagerFactory pmfInstance = JDOHelper
      .getPersistenceManagerFactory("transactions-optional");

  public static PersistenceManagerFactory get() {
    return pmfInstance;
  }

  // There is no instance of this class.
  private PMF() {
  }
}
