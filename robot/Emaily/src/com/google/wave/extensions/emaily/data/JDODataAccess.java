package com.google.wave.extensions.emaily.data;

import javax.jdo.PersistenceManager;

import com.google.inject.Singleton;

@Singleton
public class JDODataAccess implements DataAccess {
  private final PersistenceManager pm;

  public JDODataAccess(PersistenceManager pm) {
    this.pm = pm;
  }

  /**
   * {@inheritDoc}
   */
  public WaveletView getWaveletView(String waveletId, String email) {
    return pm.getObjectById(WaveletView.class, WaveletView.buildId(waveletId, email));
  }
}
