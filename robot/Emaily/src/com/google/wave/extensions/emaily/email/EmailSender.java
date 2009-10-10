package com.google.wave.extensions.emaily.email;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import com.google.inject.Singleton;

@Singleton
public class EmailSender {
  /**
   * Send simple text email.
   * 
   * @param from The recipient email address. Currently it is ignored and
   *   used only as a string representation of the email address.
   * @param recipient List of recipients.
   * @param subject The subject of the email.
   * @param body The body of the email.
   */
  public void simpleSendTextEmail(String from, String recipient,
      String subject, String body) {
    // Initialize JavaMail
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);

    // Build message
    Message msg = new MimeMessage(session);
    try {
      msg.setFrom(new InternetAddress(from));
      msg.addRecipient(RecipientType.TO, new InternetAddress(recipient));
      msg.setSubject(subject);
      msg.setText(body);
      Transport.send(msg);
    } catch (MessagingException e) {
      // TODO(dlux): Need to add better error handling here.
      throw new RuntimeException("Cannot send email from: " + from + ", to:" + recipient, e);
    }
  }
}