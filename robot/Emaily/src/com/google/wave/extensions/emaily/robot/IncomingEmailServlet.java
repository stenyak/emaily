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
