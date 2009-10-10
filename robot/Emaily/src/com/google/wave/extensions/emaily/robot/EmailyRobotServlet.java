package com.google.wave.extensions.emaily.robot;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.wave.api.AbstractRobotServlet;
import com.google.wave.api.Annotation;
import com.google.wave.api.Event;
import com.google.wave.api.EventType;
import com.google.wave.api.Range;
import com.google.wave.api.RobotMessageBundle;
import com.google.wave.api.TextView;
import com.google.wave.extensions.emaily.config.HostingProvider;
import com.google.wave.extensions.emaily.email.EmailSender;

@SuppressWarnings("serial")
@Singleton
public class EmailyRobotServlet extends AbstractRobotServlet {
  // Injected dependencies
  private final EmailSender emailSender;
  private HostingProvider hostingProvider;
  private final Provider<HttpServletRequest> reqProvider;

  @Inject
  public EmailyRobotServlet(EmailSender emailSender, HostingProvider hostingProvider,
      Provider<HttpServletRequest> reqProvider) {
    this.emailSender = emailSender;
    this.hostingProvider = hostingProvider;
    this.reqProvider = reqProvider;
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
    // Get subject
    String emailSubject = bundle.getWavelet().getTitle();

    // Get body
    String emailBody;
    TextView textView = event.getBlip().getDocument();
    boolean isRootBlip = event.getBlip().getBlipId().equals(bundle.getWavelet().getRootBlipId());
    if (isRootBlip) {
      List<Annotation> titleAnnotations = textView.getAnnotations("conv/title");
      if (titleAnnotations.size() > 0) {
        Range subjectRange = titleAnnotations.get(0).getRange();
        emailBody = textView.getText().substring(subjectRange.getEnd());
      } else {
        emailBody = textView.getText();
      }
    } else {
      emailBody = textView.getText();
    }

    // Get sender email addresses
    String senderEmail = hostingProvider.getEmailAddressForWaveParticipantIdInEmailyDomain(event
        .getBlip().getCreator());

    // Get recipient address
    JSONObject json = (JSONObject) reqProvider.get().getAttribute("jsonObject");
    try {
      String proxyingFor = json.getString("proxyingFor");
      String recipient = hostingProvider.getEmailAddressFromRobotProxyFor(proxyingFor);
      emailSender.simpleSendTextEmail(senderEmail, recipient, emailSubject, emailBody);
    } catch (JSONException e) {
      throw new RuntimeException("JSON error", e);
    }
  }
}
