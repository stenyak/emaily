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
package com.google.wave.extensions.emaily.robot;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.extensions.emaily.email.PersistentEmail;

/**
 * Processes incoming emails.
 * 
 * @author taton
 */
@SuppressWarnings("serial")
@Singleton
public class IncomingEmailServlet extends HttpServlet {

  // Injected dependencies
  private final PersistenceManagerFactory pmFactory;

  private Logger logger = Logger
      .getLogger(IncomingEmailServlet.class.getName());

  @Inject
  public IncomingEmailServlet(PersistenceManagerFactory pmFactory) {
    this.pmFactory = pmFactory;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String path = req.getServletPath();
    if (path.startsWith("/_ah/mail")) {
      try {
        processIncomingEmail(req);
        resp.setStatus(HttpURLConnection.HTTP_OK);
      } catch (Exception e) {
        resp.setStatus(HttpURLConnection.HTTP_INTERNAL_ERROR);
        return;
      }
    } else {
      super.doPost(req, resp);
    }
  }

  /**
   * Processes a HTTP request for an incoming email.
   * 
   * @param req The HTTP request.
   * @throws IOException
   */
  public void processIncomingEmail(HttpServletRequest req) throws IOException {
    logger.info("Receiving incoming email");
    byte[] data = PersistentEmail.readInputStream(req.getInputStream());
    PersistenceManager pm = pmFactory.getPersistenceManager();
    try {
      PersistentEmail email = new PersistentEmail(data);
      pm.makePersistent(email);
    } finally {
      pm.close();
    }
  }

}
