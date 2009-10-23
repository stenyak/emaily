package com.google.wave.extensions.emaily.robot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jdo.Extent;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.servlet.http.HttpServletRequest;

import org.apache.james.mime4j.MimeIOException;
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
import com.google.wave.api.Blip;
import com.google.wave.api.Event;
import com.google.wave.api.EventType;
import com.google.wave.api.RobotMessageBundle;
import com.google.wave.api.StyleType;
import com.google.wave.api.StyledText;
import com.google.wave.api.TextView;
import com.google.wave.api.Wavelet;
import com.google.wave.extensions.emaily.config.HostingProvider;
import com.google.wave.extensions.emaily.data.PersistentEmail;
import com.google.wave.extensions.emaily.email.EmailSender;
import com.google.wave.extensions.emaily.email.MailUtil;

@Singleton
public class EmailyRobotServlet extends AbstractRobotServlet {
  private static final long serialVersionUID = 8878209094937861353L;

  // Injected dependencies
  private final EmailSender emailSender;
  private final HostingProvider hostingProvider;
  private final Provider<HttpServletRequest> reqProvider;
  private final PersistenceManagerFactory pmFactory;

  private MimeEntityConfig mimeEntityConfig = new MimeEntityConfig();

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
    processIncomingEmails(bundle.getWavelet());
  }

  /**
   * Handle when a blip is submitted. Currently it sends the blip in email immediately.
   * 
   * @param bundle RobotMessageBundle received from the Wave Server.
   * @param event The BLIP_SUBMITTED event.
   */
  private void handleBlipSubmitted(RobotMessageBundle bundle, Event event) {
    // Do not try to send an email if we are not proxying for anyone.
    JSONObject json = (JSONObject) reqProvider.get().getAttribute("jsonObject");
    if (!json.has("proxyingFor")) {
      return;
    }
    String recipient;
    try {
      String proxyingFor = json.getString("proxyingFor");
      recipient = hostingProvider.getEmailAddressFromRobotProxyFor(proxyingFor);
    } catch (JSONException e) {
      throw new RuntimeException("JSON error", e);
    }
    if (recipient == null) {
      return;
    }
    final Wavelet wavelet = bundle.getWavelet();
    final Blip blip = event.getBlip();
    final String emailSubject = wavelet.getTitle();
    String emailBody = blip.getDocument().getText();
    if (blip.getBlipId().equals(wavelet.getRootBlipId())) {
      // Remove the Wave Title from the root blip content.
      emailBody = emailBody.substring(emailSubject.length()).trim();
    }
    final String senderEmail = hostingProvider
        .getEmailAddressForWaveParticipantIdInEmailyDomain(blip.getCreator());

    emailSender.simpleSendTextEmail(senderEmail, recipient, emailSubject, emailBody);
  }

  /**
   * Processes the incoming emails accumulated so far: creates one new wave per
   * email.
   * 
   * TODO(taton) When an email has many wave recipients, this currently duplicates the Wave.
   * 
   * @param wavelet Handle to create new waves.
   */
  private void processIncomingEmails(Wavelet wavelet) {
    PersistenceManager pm = pmFactory.getPersistenceManager();
    Transaction tx = pm.currentTransaction();

    try {
      Extent<PersistentEmail> extent = pm.getExtent(PersistentEmail.class, false);
      Query query = pm.newQuery(extent);
      @SuppressWarnings("unchecked")
      List<PersistentEmail> emails = (List<PersistentEmail>) query.execute();
      for (PersistentEmail email : emails) {
        try {
          tx.begin();
          createWaveFromMessage(wavelet, email);
          // TODO(taton) We should not delete the email here, but instead associate it with the
          // Wave created for it somehow.
          pm.deletePersistent(email);
          tx.commit();
        } catch (Exception exn) {
          exn.printStackTrace();
        } finally {
          if (tx.isActive())
            tx.rollback();
        }
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
  private void createWaveFromMessage(Wavelet wavelet, PersistentEmail email)
      throws MimeIOException, IOException {
    Message message = new Message(email.getInputStream(), mimeEntityConfig);
    List<String> participants = new ArrayList<String>();
    participants.addAll(email.getWaveParticipants());
    Wavelet newWavelet = wavelet.createWavelet(participants, null);
    newWavelet.setTitle(message.getSubject());
    Blip blip = newWavelet.getRootBlip();
    TextView textView = blip.getDocument();
    // textView.setAuthor(APPSPOT_ID + "@appspot.com");
    if (message.getFrom() != null) {
      // Note: appendStyledText has a bug: the style does not apply to the last character.
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
      // TODO(taton) The Wave robot Java API is buggy when processing end-of-lines.
      // This still does not work properly.
      StringBuilder sb = new StringBuilder();
      sb.append("\n");
      MailUtil.mimeEntityToText(sb, message);
      sb.append("\n");
      textView.append(sb.toString());
    }
  }

}
