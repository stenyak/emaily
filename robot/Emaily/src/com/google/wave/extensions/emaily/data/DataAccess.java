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

import java.util.List;

/**
 * Data access interface for emaily data objects.
 * 
 * @author dlux
 * 
 */
public interface DataAccess {

  /**
   * Returns an existing WaveletView from the database (detached).
   * 
   * @param id The wavelet view id.
   * @return A wavelet view.
   */
  public WaveletView getWaveletView(String id);

  /**
   * Returns an existing WaveletView from the database (detached).
   * 
   * @param waveletId The wavelet Id
   * @param email The email of the user
   * @return The existing WaveletView, or <code>null</code> if it does not exist.
   */
  public WaveletView getWaveletView(String waveletId, String email);

  /**
   * Save a given waveletView and its siblings to the database. Call this only before the end of the
   * transaction, don't use the waveletView object afterwards.
   * 
   * @param waveletView Wavelet to save.
   */
  public void saveWaveletView(WaveletView waveletView);

  /**
   * Rolls back the current transaction. Following operations will open a new transaction.
   */
  public void rollback();

  /**
   * Commits changes in the current transaction. Following operations will open a new transaction.
   */
  public void commit();

  /**
   * @return The list of wavelet Ids that are scheduled to be sent.
   */
  public List<?> getWaveletIdsToSend();

}