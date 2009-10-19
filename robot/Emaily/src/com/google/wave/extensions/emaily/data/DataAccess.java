package com.google.wave.extensions.emaily.data;

public interface DataAccess {

  /**
   * Returns an existing WaveletView from the database.
   * 
   * @param waveletId The wavelet Id
   * @param email The email of the user.
   * @return The existing WaveletView, or <code>null</code> if it does not
   *         exist.
   */
  public WaveletView getWaveletView(String waveletId, String email);

}