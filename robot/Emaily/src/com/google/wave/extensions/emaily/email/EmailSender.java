/* Copyright (c) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

/**
 * Method(s) to send email.
 * 
 * @author dlux
 */
@Singleton
public class EmailSender {
  @SuppressWarnings("serial")
  public static class EmailSendingException extends RuntimeException {
    public EmailSendingException() {
      super();
    }

    public EmailSendingException(String message, Throwable cause) {
      super(message, cause);
    }

    public EmailSendingException(String message) {
      super(message);
    }

    public EmailSendingException(Throwable cause) {
      super(cause);
    }
  }

  private Properties props = new Properties();
  private Session session = Session.getDefaultInstance(props, null);

  /**
   * Sends a simple text email.
   * 
   * @param from The sender email address.
   * @param recipient The email recipient.
   * @param subject The subject of the email.
   * @param body The text body of the email.
   */
  public void simpleSendTextEmail(String from, String recipient, String subject, String body) {
    // Build message
    Message msg = new MimeMessage(session);
    try {
      msg.setFrom(new InternetAddress(from));
      msg.addRecipient(RecipientType.TO, new InternetAddress(recipient));
      msg.setSubject(subject);
      msg.setText(body);
      Transport.send(msg);
    } catch (MessagingException e) {
      throw new EmailSendingException("Cannot send email from: " + from + ", to:" + recipient, e);
    }
  }
}