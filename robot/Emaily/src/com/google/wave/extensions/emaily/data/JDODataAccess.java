package com.google.wave.extensions.emaily.data;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;

@RequestScoped
public class JDODataAccess implements DataAccess {
  private final PersistenceManagerFactory pmf;
  private PersistenceManager pm = null;

  @Inject
  public JDODataAccess(PersistenceManagerFactory pmf) {
    this.pmf = pmf;
  }

  /**
   * {@inheritDoc}
   */
  public WaveletView getWaveletView(String waveletId, String email) {
    return getPm().getObjectById(WaveletView.class, WaveletView.buildId(waveletId, email));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void persistWaveletView(WaveletView waveletView) {
    getPm().makePersistent(waveletView);
  }

  /**
   * Get the persistence manager.
   * @return The persistence manager. If it is not exist, it creates one.
   */
  private PersistenceManager getPm() {
    if (pm == null) {
      pm = pmf.getPersistenceManager();
    }
    return pm;
  }

  /**
   * Closes the persistence manager.
   */
  @Override
  public void close() {
    if (pm != null) {
      pm.close();
    }
    pm = null;
  }
}
