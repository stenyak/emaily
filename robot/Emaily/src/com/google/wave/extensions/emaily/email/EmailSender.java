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
import java.util.HashSet;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;

import com.google.appengine.api.mail.MailService;
import com.google.appengine.api.mail.MailServiceFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.extensions.emaily.config.HostingProvider;
import com.google.wave.extensions.emaily.data.PersistentEmail;
import com.google.wave.extensions.emaily.data.WaveletData;

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

  /**
   * When BCC'ing an outgoing email to ourself, the recipient starts with this prefix concatenated
   * with the generated email address token.
   */
  public static final String OUTGOING_EMAIL_PREFIX = "outgoing_email+";

  // Injected dependencies
  private final HostingProvider hostingProvider;
  private final MailService mailService = MailServiceFactory.getMailService();
  private final PersistenceManagerFactory pmFactory;

  @Inject
  public EmailSender(HostingProvider hostingProvider, PersistenceManagerFactory pmFactory) {
    this.hostingProvider = hostingProvider;
    this.pmFactory = pmFactory;
  }

  /**
   * Sends a text email. Stores a matching PersistentEmail with a temporary Message ID to be filled
   * when we receive the BCC'ed email.
   * 
   * @param from The sender email address.
   * @param recipient The email recipient.
   * @param subject The subject of the email.
   * @param body The text body of the email.
   * @param waveletData The wavelet this email is sent from.
   */
  public void sendTextEmail(String from, String recipient, String subject, String body,
      WaveletData waveletData) {
    final String temporaryMessageId = OUTGOING_EMAIL_PREFIX + waveletData.getEmailAddressToken();

    MailService.Message message = new MailService.Message();
    message.setSender(from);
    message.setTo(recipient);
    message.setBcc(hostingProvider.getWaveletEmailAddress(temporaryMessageId));
    message.setSubject(subject);
    message.setTextBody(body);

    PersistentEmail email = new PersistentEmail(temporaryMessageId, new HashSet<String>(),
        new HashSet<String>(waveletData.getParticipants()));
    email.setWaveAndWaveletId(waveletData.getWaveId(), waveletData.getWaveletId());

    PersistenceManager pm = pmFactory.getPersistenceManager();
    Transaction tx = pm.currentTransaction();
    try {
      mailService.send(message);
      tx.begin();
      pm.makePersistent(email);
      tx.commit();
    } catch (IOException ioe) {
      throw new EmailSendingException("Cannot send email from: " + from + ", to:" + recipient, ioe);
    } finally {
      if (tx.isActive())
        tx.rollback();
      pm.close();
    }
  }
}