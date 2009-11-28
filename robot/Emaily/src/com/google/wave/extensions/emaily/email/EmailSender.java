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

import java.io.IOException;
import java.util.Collection;

import com.google.appengine.api.mail.MailService;
import com.google.appengine.api.mail.MailServiceFactory;
import com.google.inject.Singleton;
import com.google.wave.extensions.emaily.util.StrUtil;

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

  private final MailService mailService = MailServiceFactory.getMailService();

  /**
   * Sends a text email.
   * 
   * @param from The sender email address.
   * @param recipients The email recipients.
   * @param bcc The email BCC'd recipients.
   * @param subject The subject of the email.
   * @param body The text body of the email.
   */
  public void sendTextEmail(String from, Collection<String> recipients, Collection<String> bcc,
      String subject, String body) {
    try {
      MailService.Message message = new MailService.Message();
      message.setSender(from);
      message.setTo(recipients);
      message.setBcc(bcc);
      message.setSubject(subject);
      message.setTextBody(body);
      mailService.send(message);
    } catch (IOException ioe) {
      throw new EmailSendingException("Cannot send email from: " + from + ", to:"
          + StrUtil.join(recipients, ",") + " with subject:" + subject, ioe);
    }
  }
}