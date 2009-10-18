package com.google.wave.extensions.emaily.robot;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.servlet.http.HttpServletRequest;

import org.apache.james.mime4j.field.address.Address;
import org.apache.james.mime4j.field.address.Mailbox;
import org.apache.james.mime4j.message.Message;
import org.apache.james.mime4j.parser.MimeEntityConfig;
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
import com.google.wave.api.StyleType;
import com.google.wave.api.StyledText;
import com.google.wave.api.TextView;
import com.google.wave.api.Wavelet;
import com.google.wave.extensions.emaily.config.HostingProvider;
import com.google.wave.extensions.emaily.email.EmailSender;
import com.google.wave.extensions.emaily.email.MailUtil;
import com.google.wave.extensions.emaily.email.PersistentEmail;

@SuppressWarnings("serial")
@Singleton
public class EmailyRobotServlet extends AbstractRobotServlet {
  // Injected dependencies
  private final EmailSender emailSender;
  private HostingProvider hostingProvider;
  private final Provider<HttpServletRequest> reqProvider;
  private final PersistenceManagerFactory pmFactory;

  private Logger logger;

  @Inject
  public EmailyRobotServlet(EmailSender emailSender, HostingProvider hostingProvider,
      Provider<HttpServletRequest> reqProvider, PersistenceManagerFactory pmFactory) {
    this.emailSender = emailSender;
    this.hostingProvider = hostingProvider;
    this.reqProvider = reqProvider;
    this.pmFactory = pmFactory;
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

  /**
   * Processes the incoming emails accumulated so far: creates one new wave per
   * email.
   * 
   * @param wavelet Handle to create new waves.
   */
  // TODO(taton): remove these SuppressWarnings
  @SuppressWarnings({ "unused", "unchecked" })
  private void processIncomingEmails(Wavelet wavelet) {
    PersistenceManager pm = pmFactory.getPersistenceManager();
    try {
      Query query = pm.newQuery(PersistentEmail.class);
      List<PersistentEmail> emails = (List<PersistentEmail>) query.execute();
      for (PersistentEmail email : emails) {
        try {
          MimeEntityConfig config = new MimeEntityConfig();
          Message message = new Message(email.getInputStream(), config);
          createWaveFromMessage(wavelet, message);
        } catch (Exception exn) {
          logger.warning(exn.toString());
        }
        pm.deletePersistent(email);
      } // For loop

    } finally {
      pm.close();
    }
  }

  /**
   * Creates the Wave representing an email.
   * 
   * @param wavelet Handle to create new Waves.
   * @param message The incoming email to create the wave of.
   */
  private void createWaveFromMessage(Wavelet wavelet, Message message) {
    List<String> participants = new ArrayList<String>();
    // TODO(taton): this has to be solved some other way anyway
    participants.add("emaily-wave@appspot.com");
    if (message.getTo() != null) {
      // TODO(taton) The recipient should be inferred from the HTTP url.
      // for (Address to : message.getTo()) {
        // TODO(taton): It has to be done some other way anyway
        // if (to instanceof Mailbox) {
          // Mailbox mailbox = (Mailbox) to;
          // String waveAddress = emailAddressUtil.decodeFromEmailyDomain(mailbox
          //     .getAddress());
          // if (waveAddress != null)
          //  participants.add(waveAddress);
        // }
        // TODO(taton) Handle groups of addresses.
      // }
    }
    if (participants.size() == 1) {
      logger.warning("Incoming email has no valid wave destination.");
      // For debugging, redirect the message to us.
      // TODO(taton) Remove this before the release.
      participants.add("christophe.taton@wavesandbox.com");
    }
    Wavelet newWavelet = wavelet.createWavelet(participants, null);
    newWavelet.setTitle(message.getSubject());
    Blip blip = newWavelet.getRootBlip();
    TextView textView = blip.getDocument();
    // TODO(taton) Use the proper Emaily address.
    textView.setAuthor("emaily-wave@appspot.com");

    if (message.getFrom() != null) {
      // Note: appendStyledText has a bug: the style does not apply to the
      // last character.
      textView.appendStyledText(new StyledText("From: ", StyleType.BOLD));
      StringBuilder sb = new StringBuilder();
      for (Mailbox from : message.getFrom()) {
        if (from == null)
          continue;
        if (sb.length() > 0)
          sb.append(", ");
        sb.append(from.toString());
      }
      textView.append(sb.toString());
      textView.appendMarkup("<br/>\n");
    }
    if (message.getTo() != null) {
      textView.appendStyledText(new StyledText("To: ", StyleType.BOLD));
      StringBuilder sb = new StringBuilder();
      for (Address recipient : message.getTo()) {
        if (recipient == null)
          continue;
        if (sb.length() > 0)
          sb.append(", ");
        sb.append(recipient.toString());
      }
      textView.append(sb.toString());
      textView.appendMarkup("<br/>\n");
    }
    if (message.getBody() != null) {
      // TODO(taton) The Wave robot Java API is buggy when processing
      // end-of-lines. This still does not work properly.
      StringBuilder sb = new StringBuilder();
      sb.append("\n");
      MailUtil.mimeEntityToText(sb, message);
      sb.append("\n");
      textView.append(sb.toString());
    }
  }

  @Inject
  public void setDebugHelper(DebugHelper debugHelper) {
  }
}
