package com.google.wave.extensions.emaily.robot;

import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.api.Blip;
import com.google.wave.api.Event;
import com.google.wave.api.Gadget;
import com.google.wave.api.RobotMessageBundle;
import com.google.wave.extensions.emaily.config.HostingProvider;

/**
 * Event handler for no-proxy robot instances.
 * <br>
 * This class handles events for robot instances where the robot is not proxying for anything. 
 
 * @author dlux
 *
 */
@Singleton
public class RobotNoProxyEventHandler {
  private static final String emailGadgetPrefix = "email:";
  private final Logger logger;
  private HostingProvider hostingProvider;

  private static final String introText = "Emaily helps you sending emails from wave.\n"
    + "Type an email address below, and click 'Add' to add it as an email recipient.\n";

  @Inject
  public RobotNoProxyEventHandler(Logger logger, HostingProvider hostingProvider) {
    this.logger = logger;
    this.hostingProvider = hostingProvider;
  }

  void processEvents(RobotMessageBundle bundle) {
    // Process the events
    for (Event e : bundle.getEvents()) {
      processRobotBlipEvent(bundle, e);
    }
  }

  private void processRobotBlipEvent(RobotMessageBundle bundle, Event e) {
    switch (e.getType()) {
    case WAVELET_SELF_ADDED:
      handleRobotAdded(bundle);
      break;
    case BLIP_VERSION_CHANGED:
      handleBlipChanged(bundle, e);
      break;
    }
  }

  private void handleBlipChanged(RobotMessageBundle bundle, Event e) {
    Blip blip = e.getBlip();
    Gadget gadget = blip.getDocument().getGadgetView().getGadget(getAddEmailAddressGadgetUrl());
    if (gadget == null)
      return;
    Map<String, String> properties = gadget.getProperties();
    HashSet<String> waveletParticipants = new HashSet<String>(bundle.getWavelet().getParticipants());
    for (String key : new HashSet<String>(properties.keySet())) {
      if (!key.startsWith(emailGadgetPrefix))
        continue;
      String emailAddress = key.substring(emailGadgetPrefix.length());
      logger.info("User added email address: " + emailAddress);
      gadget.deleteField(key);
      String newParticipant;
      try {
        newParticipant = hostingProvider.getRobotProxyForFromEmailAddress(emailAddress);
      } catch (IllegalArgumentException ex) {
        // If it is not a valid email address, then we don't add it.
        logger.log(Level.WARNING, "Invalid user input for email address: " + emailAddress + "\n",
            ex);
        continue;
      }
      if (waveletParticipants.contains(newParticipant))
        continue;
      bundle.getWavelet().addParticipant(newParticipant);
    }
  }

  /**
   * Handle when the robot is added as a participant. An introduction blip is added to the wave if
   * robot is added as a participant without "proxyingFor" argument, i.e., adding
   * emaily-wave@appspot.com will trigger a blip, but emaily-wave+abc+xyz.com@appspot.com will not.
   * It also adds a form which makes it easy to add new email participants to the wave.
   * 
   * @param bundle RobotMessageBundle received from the Wave Server.
   */
  private void handleRobotAdded(RobotMessageBundle bundle) {
    logger.info("Robot added to participants");
    // bundle.getWavelet().appendBlip().getDocument().append(introText) will
    // display the text twice due to
    // http://code.google.com/p/google-wave-resources/issues/detail?id=354
    // TODO(karan) Change this once the above Wave API issue is fixed.

    Blip newBlip = bundle.getWavelet().appendBlip();
    newBlip.getDocument().delete();
    newBlip.getDocument().append(introText);
    newBlip.getDocument().appendElement(new Gadget(getAddEmailAddressGadgetUrl()));
  }

  private String getAddEmailAddressGadgetUrl() {
    return hostingProvider.getRobotURL() + "gadgets/add-email-address-gadget.xml";
  }

}
