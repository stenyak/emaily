package com.google.wave.extensions.emaily.robot;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.james.mime4j.message.Message;

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

  private Logger logger;

  /**
   * Accumulates the emails until we get an opportunity to send them to the Wave
   * server. This is a workaround until we get the active Wave API.
   */
  private List<Message> emails = new ArrayList<Message>();

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
    byte[] data = PersistentEmail.readInputStream(req.getInputStream());
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      PersistentEmail email = new PersistentEmail(data);
      pm.makePersistent(email);
    } finally {
      pm.close();
    }
  }

}
