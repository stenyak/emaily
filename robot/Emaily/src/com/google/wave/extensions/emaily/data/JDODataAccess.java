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

import java.util.Calendar;
import java.util.List;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;

import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;

/**
 * Data access object for JDO.
 * 
 * @author dlux
 * 
 */
@RequestScoped
public class JDODataAccess implements DataAccess {
  // Current persistence manager and transaction
  private PersistenceManager pm = null;
  private Transaction tx = null;

  // Injected dependencies
  private final PersistenceManagerFactory pmf;

  @Inject
  public JDODataAccess(PersistenceManagerFactory pmf) {
    this.pmf = pmf;
  }

  @Override
  public WaveletView getWaveletView(String id) {
    try {
      return getPm().getObjectById(WaveletView.class, id);
    } catch (JDOObjectNotFoundException e) {
      return null;
    }
  }

  public WaveletView getWaveletView(String waveletId, String email) {
    return getWaveletView(WaveletView.buildId(waveletId, email));
  }

  @Override
  public void persistWaveletView(WaveletView waveletView) {
    getPm().makePersistent(waveletView);
  }

  /**
   * Get the persistence manager.
   * 
   * @return The persistence manager. If it is not exist, it creates one, and creates a new
   *         transaction also.
   */
  private PersistenceManager getPm() {
    if (pm == null) {
      pm = pmf.getPersistenceManager();
      tx = pm.currentTransaction();
      tx.begin();
    }
    return pm;
  }

  @Override
  public void commit() {
    if (tx != null && tx.isActive()) {
      tx.commit();
    }
    close();
  }

  @Override
  public void rollback() {
    if (tx != null && tx.isActive()) {
      tx.rollback();
    }
    close();
  }

  /**
   * Delete the transaction and closes the persistence manager.
   */
  private void close() {
    tx = null;

    if (pm != null) {
      pm.close();
    }
    pm = null;
  }

  @Override
  public List<?> getWaveletIdsToSend() {
    Query query = getPm().newQuery(
        "select id from " + WaveletView.class.getName() + " where timeForSending < :now ");
    return (List<?>) query.execute(Calendar.getInstance().getTimeInMillis());
  }
}