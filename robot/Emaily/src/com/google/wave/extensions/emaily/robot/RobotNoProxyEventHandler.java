package com.google.wave.extensions.emaily.robot;

import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.wave.api.Blip;
import com.google.wave.api.Event;
import com.google.wave.api.EventType;
import com.google.wave.api.Gadget;
import com.google.wave.api.RobotMessageBundle;
import com.google.wave.extensions.emaily.config.HostingProvider;
import com.google.wave.extensions.emaily.data.BlipData;
import com.google.wave.extensions.emaily.data.DataAccess;
import com.google.wave.extensions.emaily.data.WaveletData;
import com.google.wave.extensions.emaily.data.WaveletData.SendMode;
import com.google.wave.extensions.emaily.scheduler.EmailSchedulingCalculator;
import com.google.wave.extensions.emaily.util.DebugHelper;

/**
 * Event handler for no-proxy robot instances. <br>
 * This class handles events for robot instances where the robot is not proxying for anything.
 * 
 * @author dlux
 * 
 */
@Singleton
public class RobotNoProxyEventHandler {
  private static final String SEND_MODE_KEY = "send_mode";
  private static final String EMAIL_GADGET_PREFIX = "email:";
  private static final String SEND_BUTTON_KEY = "send";
  private final Logger logger;
  private HostingProvider hostingProvider;

  private static final String introText = "Emaily helps you sending emails from wave.\n"
      + "Type an email address below, and click 'Add' to add it as an email recipient.\n";
  private final EmailSchedulingCalculator emailSchedulingCalculator;
  private final Provider<DataAccess> dataAccessProvider;
  private final DebugHelper debugHelper;

  @Inject
  public RobotNoProxyEventHandler(Logger logger, HostingProvider hostingProvider,
      EmailSchedulingCalculator emailSchedulingCalculator, Provider<DataAccess> dataAccessProvider,
      DebugHelper debugHelper) {
    this.logger = logger;
    this.hostingProvider = hostingProvider;
    this.emailSchedulingCalculator = emailSchedulingCalculator;
    this.dataAccessProvider = dataAccessProvider;
    this.debugHelper = debugHelper;
  }

  /**
   * Process modifications for the wavelet changes and schedule sending.
   * 
   * @param bundle The robot message bundle.
   * @param email The email of the user.
   */
  void processEvents(RobotMessageBundle bundle) {
    try {
      String waveId = bundle.getWavelet().getWaveId();
      String waveletId = bundle.getWavelet().getWaveletId();

      // Get or create the wavelet data for the wavelet and the current user.
      WaveletData waveletData = dataAccessProvider.get().getWaveletData(waveId, waveletId);
      if (waveletData == null) {
        waveletData = new WaveletData(waveId, waveletId, bundle.getWavelet().getRootBlipId());
        dataAccessProvider.get().persistWaveletData(waveletData);
      }

      updateWaveletData(waveletData, bundle);

      // Process the events
      for (Event e : bundle.getEvents()) {
        processEvent(waveletData, bundle, e);
      }

      // Calculate the next send time
      emailSchedulingCalculator.calculateWaveletDataNextSendTime(waveletData);

      // Prints the debug info
      logger.info(debugHelper.printWaveletDataInfo(waveletData));
      dataAccessProvider.get().commit();
    } finally {
      dataAccessProvider.get().close();
    }
  }

  /**
   * Process one event in the incoming robot event bundle.
   * 
   * @param waveletData The wavelet data stored in our database.
   * @param bundle The robot message bundle received from the wave server.
   * @param e The event to handle.
   */
  private void processEvent(WaveletData waveletData, RobotMessageBundle bundle, Event e) {
    switch (e.getType()) {
    case WAVELET_SELF_ADDED:
      processRobotAdded(bundle, e);
      return;
    case BLIP_DELETED:
      processBlipDelete(waveletData, e);
      return;
    }

    // The following event handlers will need a blip
    Blip blip = e.getBlip();
    if (blip == null)
      return;

    // Process control gadget state change (if the blip has a control gadget).
    processControlGadgetStateChange(waveletData, bundle, e, blip);

    // Process content update event (if applicable)
    processBlipContentUpdate(waveletData, e, blip);

    // Add/remove send
  }

  /**
   * Handle when the robot is added as a participant. An introduction blip is added to the wave if
   * robot is added as a participant without "proxyingFor" argument, i.e., adding
   * emaily-wave@appspot.com will trigger a blip, but emaily-wave+abc+xyz.com@appspot.com will not.
   * It also adds a form which makes it easy to add new email participants to the wave.
   * 
   * @param bundle RobotMessageBundle received from the Wave Server.
   */
  private void processRobotAdded(RobotMessageBundle bundle, Event e) {
    logger.info("Robot added to participants");
    // bundle.getWavelet().appendBlip().getDocument().append(introText) will
    // display the text twice due to
    // http://code.google.com/p/google-wave-resources/issues/detail?id=354

    Blip newBlip = bundle.getWavelet().appendBlip();
    newBlip.getDocument().delete();
    newBlip.getDocument().append(introText);
    newBlip.getDocument().appendElement(new Gadget(getAddEmailAddressGadgetUrl()));
  }

  /**
   * Process delete blip event.
   * 
   * @param waveletData The wavelet data stored in our database.
   * @param e The delete event.
   */
  private void processBlipDelete(WaveletData waveletData, Event e) {
    logger.fine("Blip deleted. Finding the blip and deleting it.");
    for (BlipData b : waveletData.getUnsentBlips()) {
      if (e.getProperty("blipId").equals(b.getBlipId())) {
        waveletData.getUnsentBlips().remove(b);
        break;
      }
    }
  }

  /**
   * Process content update in a blip. This method update the email sending times according to
   * content change in the event which is related to a blip. It also adds or removes the
   * "Send Email" button if the email sending mode is set to manual.
   * 
   * @param waveletData The wavelet data object of the current event.
   * @param e The wave event.
   */
  private void processBlipContentUpdate(WaveletData waveletData, Event e, Blip blip) {
    // If the robot is a contributor, then we don't process the blip:
    if (blip.getCreator().contains(hostingProvider.getRobotWaveId()))
      return;

    // Extract the content
    logger.finer("Extracting content");
    String blipContent = blip.getDocument().getText();
    if (blip.getBlipId().equals(waveletData.getRootBlipId())) {
      // Remove the Wave Title from the root blip content.
      blipContent = blipContent.substring(waveletData.getTitle().length()).trim();
    }
    blipContent = blipContent.trim();

    // Check if there is at least one email user in the wavelet who is NOT the creator of the blip.
    // If we don't have such a user, then we don't process the blip.
    if (emailSchedulingCalculator.calculateInterestedEmailRecipientsForBlip(blip.getCreator(),
        waveletData.getParticipants()).size() == 0) {
      logger.finer("No interested participants");
      return;
    }

    // Find the corresponding blip in the unsent ones
    logger.finer("Find the blip if it was already in the unsent list");
    BlipData blipData = null;
    for (BlipData b : waveletData.getUnsentBlips()) {
      if (blip.getBlipId().equals(b.getBlipId())) {
        blipData = b;
        break;
      }
    }

    // If it did not exist yet, create one:
    logger.finer("Create one if not exist yet");
    if (blipData == null) {
      if (blipContent.isEmpty()) {
        logger.fine("The blip content is empty, we don't create a new blip");
        return;
      }
      if (e.getType() == EventType.BLIP_SUBMITTED) {
        logger.fine("Blip submitted without edit: don't create a new version of it");
        return;
      }
      logger.finer("Create new blip");
      // Otherwise, create a new BlipVersionWiew
      blipData = new BlipData(waveletData, blip.getBlipId(), blip.getCreator());
      waveletData.getUnsentBlips().add(blipData);
    }

    logger.finer("Update new blip data");

    // Update the blip version to the latest
    blipData.setVersion(blip.getVersion());

    // Update blip contributors (and remove the robot if it was there)
    blipData.setContributors(blip.getContributors());
    blipData.getContributors().remove(hostingProvider.getRobotWaveId());

    // If the user pressed the "Send" button, then make it immediately sendable
    Gadget sendGadget = blip.getDocument().getGadgetView().getGadget(getSendGadgetUrl());
    boolean manualSendRequest = sendGadget != null
        && sendGadget.getProperty(SEND_BUTTON_KEY) != null;

    boolean still_editing = e.getType() != EventType.BLIP_SUBMITTED;

    logger.finer("Manual send requested: " + Boolean.toString(manualSendRequest)
        + ", still editing: " + Boolean.toString(still_editing));

    if (still_editing && blipContent.equals(blipData.getContent())) {
      logger.finer("Still editing, content is not changed - no update");
      // We don't change any timestamp if the content is not changed.
      return;
    }
    blipData.setContent(blipContent);

    emailSchedulingCalculator.updateBlipDataTimestamps(blipData, still_editing, manualSendRequest);

    if (waveletData.getSendMode() == SendMode.MANUAL) {
      if (manualSendRequest)
        removeSendButton(blip);
      else
        addSendButton(blip);
    }
  }

  private void addSendButton(Blip blip) {
    logger.finer("Adding send button (if it is not there already)");
    if (blip.getDocument().getGadgetView().getGadget(getSendGadgetUrl()) != null)
      return;
    int position = 0;
    if (blip.getWavelet().getRootBlipId().equals(blip.getBlipId()))
      position = blip.getWavelet().getTitle().length() + 1;  // Keep the new line, too.
    logger.finer(String.format("Adding gadget to position: %d", position));
    blip.getDocument().insertElement(position, new Gadget(getSendGadgetUrl()));
  }

  private void removeSendButton(Blip blip) {
    logger.finer("Removing send button (if it is there)");
    blip.getDocument().getGadgetView().delete(getSendGadgetUrl());
  }

  private void processControlGadgetStateChange(WaveletData waveletData, RobotMessageBundle bundle,
      Event e, Blip blip) {
    Gadget gadget;
    try {
      gadget = blip.getDocument().getGadgetView().getGadget(getAddEmailAddressGadgetUrl());
    } catch (NullPointerException npe) {
      // The API throws NPE if the blip has no content, we ignore it here.
      return;
    }
    if (gadget == null)
      return;

    // Update the wavelet data send mode if it changed.
    SendMode sendMode;
    try {
      sendMode = SendMode.valueOf(gadget.getProperty(SEND_MODE_KEY, ""));
    } catch (IllegalArgumentException ex) {
      sendMode = SendMode.AUTOMATIC;
    }
    if (waveletData.getSendMode() != sendMode)
      waveletData.setSendMode(sendMode);

    // Add other email recipients.
    Map<String, String> properties = gadget.getProperties();
    HashSet<String> waveletParticipants = new HashSet<String>(bundle.getWavelet().getParticipants());
    for (String key : new HashSet<String>(properties.keySet())) {
      if (!key.startsWith(EMAIL_GADGET_PREFIX))
        continue;
      String emailAddress = key.substring(EMAIL_GADGET_PREFIX.length());
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
   * Updates the wavelet data from the information received by the robot. Currently it just updates
   * the title and the recipient list.
   * 
   * @param waveletData The wavelet data to update.
   * @param bundle The message bundle received by the robot.
   */
  private void updateWaveletData(WaveletData waveletData, RobotMessageBundle bundle) {
    waveletData.setTitle(bundle.getWavelet().getTitle());
    waveletData.setParticipants(bundle.getWavelet().getParticipants());
  }

  private String getAddEmailAddressGadgetUrl() {
    return hostingProvider.getRobotURL() + "gadgets/add-email-address-gadget.xml";
  }

  private String getSendGadgetUrl() {
    return hostingProvider.getRobotURL() + "gadgets/send-gadget.xml";
  }
}