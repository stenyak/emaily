package com.google.wave.extensions.emaily.robot;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.wave.api.AbstractRobotServlet;
import com.google.wave.api.Annotation;
import com.google.wave.api.Blip;
import com.google.wave.api.Event;
import com.google.wave.api.EventType;
import com.google.wave.api.Range;
import com.google.wave.api.RobotMessageBundle;
import com.google.wave.api.TextView;
import com.google.wave.extensions.emaily.config.HostingProvider;
import com.google.wave.extensions.emaily.email.EmailAddressUtil;
import com.google.wave.extensions.emaily.email.EmailSender;
import com.google.wave.extensions.emaily.util.WaveUtil;

@Singleton
public class EmailyRobotServlet extends AbstractRobotServlet {
  private static final long serialVersionUID = 8878209094937861353L;

  // Injected dependencies
  private final EmailAddressUtil emailAddressUtil;
  private final EmailSender emailSender;
  private HostingProvider hostingProvider;
  private final Provider<HttpServletRequest> reqProvider;
  private final WaveUtil waveUtil;

  @Inject
  public EmailyRobotServlet(EmailAddressUtil emailAddressUtil, EmailSender emailSender,
      HostingProvider hostingProvider, Provider<HttpServletRequest> reqProvider, WaveUtil waveUtil) {
    this.emailAddressUtil = emailAddressUtil;
    this.emailSender = emailSender;
    this.hostingProvider = hostingProvider;
    this.reqProvider = reqProvider;
    this.waveUtil = waveUtil;
  }

  /**
   * Handles all robot events.
   */
  @Override
  public void processEvents(RobotMessageBundle bundle) {
    for (Event event : bundle.getEvents()) {
      if (event.getType() == EventType.BLIP_SUBMITTED) {
        handleBlipSubmitted(bundle, event);
      }
    }
  }

  /**
   * Handle when a blip is submitted. Currenlty it sends the blip in email
   * immediately.
   * 
   * @param bundle RobotMessageBundle received from the Wave Server.
   * @param event The BLIP_SUBMITTED event.
   */
  private void handleBlipSubmitted(RobotMessageBundle bundle, Event event) {
    // Find Email Subject
    Blip rootBlip = bundle.getWavelet().getRootBlip();
    TextView rootTextView = rootBlip.getDocument();
    String emailSubject;
    String rootBody;
    List<Annotation> titleAnnotations = rootTextView.getAnnotations("conv/title");
    if (titleAnnotations.size() > 0) {
      Range subjectRange = titleAnnotations.get(0).getRange();
      emailSubject = rootTextView.getText().substring(0, subjectRange.getEnd());
      rootBody = rootTextView.getText().substring(subjectRange.getEnd());
    } else {
      emailSubject = "";
      rootBody = rootTextView.getText();
    }
    boolean isRootBlip = event.getBlip().getBlipId().equals(bundle.getWavelet().getRootBlipId());
    String emailBody;
    if (isRootBlip) {
      emailBody = rootBody;
    } else {
      emailBody = event.getBlip().getDocument().getText();
    }

    // Get sender email addresses
    String senderEmail = hostingProvider.getEmailAddressForWaveParticipantIdInEmailyDomain(event
        .getBlip().getCreator());

    // Get recipient address
    JSONObject json = waveUtil.getJsonObjectFromRequest(reqProvider.get());
    try {
      String proxyingFor = json.getString("proxyingFor");
      String recipient = hostingProvider.getEmailAddressFromRobotProxyFor(proxyingFor);
      emailSender.simpleSendTextEmail(senderEmail, recipient, emailSubject, emailBody);
    } catch (JSONException e) {
      throw new RuntimeException("JSON error", e);
    }
  }

  
}
