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
   * Returns an existing WaveletData from the database.
   * 
   * @param id The wavelet data id.
   * @return A wavelet data object.
   */
  public WaveletData getWaveletData(String id);

  /**
   * Returns an existing WaveletData from the database.
   * 
   * @param waveId The wave Id
   * @param waveletId The wavelet Id
   * @return The existing WaveletData, or <code>null</code> if it does not exist.
   */
  public WaveletData getWaveletData(String waveId, String waveletId);

  /**
   * Persists a given waveletData. Call this only for non-persisted objects. After persisting, the
   * object can be modified until the end of the transation.
   * 
   * @param waveletData Wavelet data object to persist.
   */
  public void persistWaveletData(WaveletData waveletData);

  /**
   * Rolls back the current transaction if it was not committed, and closes it. Following operations
   * will open a new transaction.
   */
  public void close();

  /**
   * Commits changes in the current transaction and closes it. Following operations will open a new
   * transaction.
   */
  public void commit();

  /**
   * @return The list of wavelet Ids that are scheduled to be sent.
   */
  public List<String> getWaveletIdsToSend();

}