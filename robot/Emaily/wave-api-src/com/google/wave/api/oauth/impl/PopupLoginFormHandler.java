// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.wave.api.oauth.impl;

import com.google.wave.api.Gadget;
import com.google.wave.api.GadgetView;
import com.google.wave.api.TextView;
import com.google.wave.api.Wavelet;
import com.google.wave.api.oauth.LoginFormHandler;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Logger;

/**
 * Adds a gadget that automatically opens a new browser window directed
 * to the service provider authorization url. 
 * 
 * @author elizabethford@google.com (Elizabeth Ford)
 * @author kimwhite@google.com (Kimberly White)
 */
public class PopupLoginFormHandler implements LoginFormHandler {

  private static final Logger LOG = Logger.getLogger(SimpleLoginFormHandler.class.getName());
  
  /** The URL of the gadget we add to handle the popup. */ 
  private static final String GADGET_PATH = "/popup.xml";
  
  //TODO: Have this be passed in by TweetyServlet.
  /** Character encoding used to encode query params. */
  private static final String CHARACTER_ENCODING = "UTF-8";
  
  /** Robot address. */
  private final String remoteHost;
  
  /**
   * Constructor
   * 
   * @param remoteHost The robot address.
   */
  public PopupLoginFormHandler(String remoteHost) {
    this.remoteHost = remoteHost;
  }

  @Override
  public void renderLogin(String userRecordKey, Wavelet wavelet) {
    // Clear login form.
    TextView document = wavelet.getRootBlip().getDocument();
    document.delete();

    // TODO (elizabethford): Eventually have buildUrl from within gadget with gadget fetching
    // request key from datastore.
    // Add the gadget.
    GadgetView gadgetView = document.getGadgetView();
    String gadgetString = "";
    try {
      String gadgetUrl = "http://" + remoteHost + GADGET_PATH;
      gadgetString = gadgetUrl + "?" + URLEncoder.encode("key", CHARACTER_ENCODING) + "=" 
          + URLEncoder.encode(userRecordKey, CHARACTER_ENCODING);
    } catch (UnsupportedEncodingException e) {
      LOG.warning(e.toString());
    }
    Gadget gadget = new Gadget(gadgetString);
    LOG.info(gadgetString);
    gadgetView.append(gadget);
  }
}
