package com.google.wave.extensions.emaily.robot;

import static com.google.wave.extensions.emaily.robot.EmailyProfileServlet.APPSPOT_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.james.mime4j.field.address.Address;
import org.apache.james.mime4j.field.address.Mailbox;
import org.apache.james.mime4j.message.Message;

import com.google.inject.Inject;
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
import com.google.wave.extensions.emaily.email.EmailAddressUtil;
import com.google.wave.extensions.emaily.email.EmailSender;
import com.google.wave.extensions.emaily.util.MailUtil;

@Singleton
public class EmailyRobotServlet extends AbstractRobotServlet {
  private static final long serialVersionUID = 8878209094937861353L;

  // Injected dependencies
  private final EmailAddressUtil emailAddressUtil;
  private final EmailSender emailSender;
  private final IncomingEmailServlet incomingEmailServlet;
  private DebugHelper debugHelper;
  private Logger logger = Logger.getLogger(EmailyRobotServlet.class.getName());

  @Inject
  public EmailyRobotServlet(EmailAddressUtil emailAddressUtil,
      EmailSender emailSender, IncomingEmailServlet incomingEmailServlet) {
    this.emailAddressUtil = emailAddressUtil;
    this.emailSender = emailSender;
    this.incomingEmailServlet = incomingEmailServlet;
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
    List<Annotation> titleAnnotations = rootTextView
        .getAnnotations("conv/title");
    if (titleAnnotations.size() > 0) {
      Range subjectRange = titleAnnotations.get(0).getRange();
      emailSubject = rootTextView.getText().substring(0, subjectRange.getEnd());
      rootBody = rootTextView.getText().substring(subjectRange.getEnd());
    } else {
      emailSubject = "";
      rootBody = rootTextView.getText();
    }
    boolean isRootBlip = event.getBlip().getBlipId().equals(
        bundle.getWavelet().getRootBlipId());
    String emailBody;
    if (isRootBlip) {
      emailBody = rootBody;
    } else {
      emailBody = event.getBlip().getDocument().getText();
    }

    // Get sender email addresses.
    String senderEmail = emailAddressUtil.encodeToEmailyDomain(event.getBlip()
        .getCreator());

    // Get recipients
    List<String> recipients = new ArrayList<String>();
    for (String participant : bundle.getWavelet().getParticipants()) {
      String emailAddress = emailAddressUtil
          .decodeFromEmailyDomain(participant);
      if (emailAddress != null) {
        recipients.add(emailAddress);
      }
    }
    if (recipients.size() > 0) {
      emailSender.simpleSendTextEmail(senderEmail, recipients, emailSubject,
          emailBody);
    }
  }

  /**
   * Processes the incoming emails accumulated so far: creates one new wave per
   * email.
   * 
   * @param wavelet Handle to create new wavelets from.
   */
  private void sendEmails(Wavelet wavelet) {
    List<Message> emails = incomingEmailServlet.getIncomingEmails();
    for (Message message : emails) {
      List<String> participants = new ArrayList<String>();
      participants.add(APPSPOT_ID + "@appspot.com");
      if (message.getTo() != null) {
        for (Address to : message.getTo()) {
          if (to instanceof Mailbox) {
            Mailbox mailbox = (Mailbox) to;
            String waveAddress = emailAddressUtil
                .decodeFromEmailyDomain(mailbox.getAddress());
            if (waveAddress != null)
              participants.add(waveAddress);
          }
          // TODO(taton) Handle groups of addresses.
        }
      }
      if (participants.size() == 1) {
        logger.warning("Incoming email has no valid wave destination.");
        // For debugging, redirect the message to us.
        participants.add("christophe.taton@wavesandbox.com");
      }
      Wavelet newWavelet = wavelet.createWavelet(participants, null);
      newWavelet.setTitle(message.getSubject());
      Blip blip = newWavelet.getRootBlip();
      TextView textView = blip.getDocument();
      textView.setAuthor(APPSPOT_ID + "@appspot.com");

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
    emails.clear();
  }

  @Inject
  public void setDebugHelper(DebugHelper debugHelper) {
    this.debugHelper = debugHelper;
  }
}
