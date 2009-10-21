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