package com.google.wave.extensions.emaily.robot;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Singleton;
import com.google.wave.extensions.emaily.util.StrUtil;

/**
 * Processes incoming emails.
 * 
 * @author taton
 */
@SuppressWarnings("serial")
@Singleton
public class IncomingEmailServlet extends HttpServlet {

  private Logger logger;

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String path = req.getServletPath();
    if (path.startsWith("/_ah/mail")) {
      try {
        processIncomingEmail(req, resp);
        resp.setStatus(HttpURLConnection.HTTP_OK);
      } catch (Exception e) {
        resp.setStatus(HttpURLConnection.HTTP_INTERNAL_ERROR);
        return;
      }
    } else {
      super.doPost(req, resp);
    }
  }

  public void processIncomingEmail(HttpServletRequest req,
      HttpServletResponse resp) throws IOException {
    logger.info(StrUtil.readContent(req.getInputStream()));
  }

}
