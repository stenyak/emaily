// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.wave.api.oauth.impl;

import com.google.wave.api.ElementType;
import com.google.wave.api.Event;
import com.google.wave.api.EventType;
import com.google.wave.api.FormElement;
import com.google.wave.api.Range;
import com.google.wave.api.TextView;
import com.google.wave.api.Wavelet;
import com.google.wave.api.oauth.LoginFormHandler;

import java.util.List;
import java.util.logging.Logger;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;

/**
 * Generates a login form in Wavelet to direct user to OAuth service login.
 * Includes a link to the service provider authorization page that opens the 
 * page in a new window.
 * 
 * @author elizabethford@google.com (Elizabeth Ford)
 * @author kimwhite@google.com (Kimberly White)
 */
public class SimpleLoginFormHandler implements LoginFormHandler {

  private static final Logger LOG = Logger.getLogger(SimpleLoginFormHandler.class.getName());

  /** The key of a link annotation. */
  private static final String LINK_ANNOTATION_KEY = "link/manual";

  /** Text for the link to the authorization page. */
  private static final String LOGIN_LINK_TEXT = "Authorization Required";

  /** The caption of the button element in the table of contents wave. */
  private static final String LOGIN_BUTTON_CAPTION = "Continue";

  /** The id of the button element. */
  private static final String LOGIN_BUTTON_ID = "successButton";

  public void renderLogin(String userRecordKey, Wavelet wavelet) {
    // Clear login form.
    TextView document = wavelet.getRootBlip().getDocument();
    document.delete();
    
    PersistenceManager pm = SingletonPersistenceManagerFactory.get().getPersistenceManager();
    OAuthUser userProfile = null;
    try {
      userProfile = pm.getObjectById(OAuthUser.class, userRecordKey);
    } catch (JDOObjectNotFoundException objectNotFound) {
      LOG.severe("Error fetching object from datastore with key: " + userRecordKey);
    } finally {
      pm.close();
    }
    String url = userProfile.getAuthUrl();

    // Add authentication prompt and insert link to service provider log-in page
    // to wavelet.
    document.delete();
    StringBuilder b = new StringBuilder();
    b.append("\n");
    int startIndex = b.length();
    b.append(LOGIN_LINK_TEXT + "\n\n");
    document.append(b.toString());

    // Add button to click when authentication is complete.
    document.appendElement(new FormElement(ElementType.BUTTON, LOGIN_BUTTON_ID,
        LOGIN_BUTTON_CAPTION));

    // Linkify the authorization link.
    document.setAnnotation(new Range(startIndex, startIndex + LOGIN_LINK_TEXT.length()),
        LINK_ANNOTATION_KEY, url);
  }

  /**
   * Checks whether a button in a blip was clicked or not. This method will
   * reset the state of the button to be "unclicked" at the end of the method
   * call.
   * 
   * @param events A list of events received from Google Wave that needs to be
   *     checked whether it contains form button clicked event or not.
   * @return true If the user just clicked on the button.
   */
  public boolean isButtonClicked(List<Event> events) {
    for (Event event : events) {
      if (event.getType() == EventType.FORM_BUTTON_CLICKED
          && LOGIN_BUTTON_ID.equals(event.getButtonName())) {
        return true;
      }
    }
    return false;
  }
}
