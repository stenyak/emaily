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

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;

import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;

@RequestScoped
public class JDODataAccess implements DataAccess {
  private final PersistenceManagerFactory pmf;
  private PersistenceManager pm = null;
  private Transaction tx = null;

  @Inject
  public JDODataAccess(PersistenceManagerFactory pmf) {
    this.pmf = pmf;
  }

  /**
   * {@inheritDoc}
   */
  public WaveletView getWaveletView(String waveletId, String email) {
    try {
      return getPm().getObjectById(WaveletView.class, WaveletView.buildId(waveletId, email));
    } catch (JDOObjectNotFoundException e) {
      return null;
    }
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
   * 
   * @return The persistence manager. If it is not exist, it creates one, and creates a new
   *         transaction for that.
   */
  private PersistenceManager getPm() {
    if (pm == null) {
      createPersistenceManager();
    }
    return pm;
  }

  private void createPersistenceManager() {
    pm = pmf.getPersistenceManager();
    tx = pm.currentTransaction();
    tx.begin();
  }

  /**
   * Closes the persistence manager.
   */
  @Override
  public void close() {
    if (tx != null && tx.isActive()) {
      tx.commit();
    }
    tx = null;

    if (pm != null) {
      pm.close();
    }
    pm = null;
  }

  /**
   * Rolls back the transaction and closes the persistence manager.
   */
  @Override
  public void rollback() {
    if (tx != null && tx.isActive()) {
      tx.rollback();
    }
    tx = null;
    close();
  }
}
