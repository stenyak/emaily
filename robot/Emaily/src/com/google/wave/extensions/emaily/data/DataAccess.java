package com.google.wave.extensions.emaily.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface DataAccess {

  /**
   * Returns an existing WaveletView from the database.
   * 
   * @param waveletId The wavelet Id
   * @param email The email of the user.
   * @return The existing WaveletView, or <code>null</code> if it does not exist.
   */
  public WaveletView getWaveletView(String waveletId, String email);

  /**
   * Persists a given waveletView.
   * 
   * @param waveletView Wavelet to persist.
   */
  public void persistWaveletView(WaveletView waveletView);

  /**
   * Rolls back the current transaction.
   */
  public void rollback();

  /**
   * Close data access session. It commits the transaction if it is still active.
   * It has to be called at the end of a request processing if the
   * request manipulated any data.
   */
  public void close();

}