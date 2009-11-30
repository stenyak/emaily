package com.google.wave.extensions.emaily.email;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.james.mime4j.field.address.Address;
import org.apache.james.mime4j.field.address.Mailbox;
import org.apache.james.mime4j.message.Message;
import org.apache.james.mime4j.parser.Field;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.extensions.emaily.config.HostingProvider;
import com.google.wave.extensions.emaily.data.EmailToProcess;
import com.google.wave.extensions.emaily.data.PersistentEmail;

/**
 * Processes incoming emails. TODO(taton) Refactor all email processing into a single class.
 * 
 * @author taton
 */
@Singleton
public class IncomingEmailServlet extends HttpServlet {
  private static final long serialVersionUID = -3570174607499608832L;
  public static final String REQUEST_URI_PREFIX = "/_ah/mail/";

/** A regexp that matches fields delimited with '<' and '>'. */
  private static final Pattern markupPattern = Pattern.compile("\\s*<([^>]*)>");

  /**
   * Reads an InputStream into a byte array.
   * 
   * @param is The input stream.
   * @return The byte array.
   * @throws IOException
   */
  public static byte[] readInputStream(InputStream is) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    final int BUFFER_SIZE = 1024;
    byte[] buffer = new byte[BUFFER_SIZE];
    while (true) {
      int bytesRead = is.read(buffer);
      if (bytesRead == -1) // End of file
        break;
      os.write(buffer, 0, bytesRead);
    }
    return os.toByteArray();
  }

  private Logger logger = Logger.getLogger(IncomingEmailServlet.class.getName());

  // Injected dependencies
  private final PersistenceManagerFactory pmFactory;
  private final HostingProvider hostingProvider;

  @Inject
  public IncomingEmailServlet(PersistenceManagerFactory pmFactory, HostingProvider hostingProvider) {
    this.pmFactory = pmFactory;
    this.hostingProvider = hostingProvider;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    final String uri = req.getRequestURI();
    if (uri.startsWith(REQUEST_URI_PREFIX)) {
      try {
        processIncomingRequest(uri, req.getInputStream());
        resp.setStatus(HttpURLConnection.HTTP_OK);

      } catch (Exception exn) {
        exn.printStackTrace();
        resp.setStatus(HttpURLConnection.HTTP_INTERNAL_ERROR);
      }
    } else {
      super.doPost(req, resp);
    }
  }

  /**
   * Processes an HTTP request containing an incoming email.
   * 
   * @param uri The URI the request has been posted to.
   * @param input The request input stream containing the email content.
   */
  public void processIncomingRequest(String uri, InputStream input) throws IOException {
    final byte[] content = readInputStream(input);
    final Message message = new Message(new ByteArrayInputStream(content));
    if (logger.isLoggable(Level.FINER)) {
      logger.finer("Unprocessed email content:\n" + new String(content) + "\nEnd of email\n");
    }

    if (message.getFrom() == null) {
      logger.info("Email has no From header: discarding.");
      return;
    }
    if (message.getFrom().size() != 1) {
      logger.info("Email has " + message.getFrom().size() + " From header(s): discarding.");
      return;
    }

    if (message.getMessageId() == null) {
      logger.info("Email has no message ID: discarding.");
      return;
    }
    Matcher idMatcher = markupPattern.matcher(message.getMessageId());
    if (!idMatcher.find()) {
      logger.warning("Email has invalid Message-ID: " + message.getMessageId());
      return;
    }
    final String messageId = idMatcher.group(1);

    PersistenceManager pm = pmFactory.getPersistenceManager();
    Transaction tx = pm.currentTransaction();
    try {
      tx.begin();
      if (PersistentEmail.lookupByMessageId(pm, messageId) != null) {
        logger.info("Email with message ID: " + messageId + " already exists: ignoring.");
        // This happens with emails with multiple Wave proxied-for email addresses in the To header:
        // an email is sent for each such address to the robot.
        return;
      }
    } finally {
      if (tx.isActive())
        tx.rollback();
      pm.close();
    }
    // This means the message ID is unknown: we need to process this email.

    final String recipient = URLDecoder.decode(uri.substring(REQUEST_URI_PREFIX.length()), "utf8");

    final String temporaryMessageId = hostingProvider
        .getTemporaryMessageIDFromEmailAddress(recipient);
    if (temporaryMessageId != null) {
      updateMessageIdInPersistentEmail(temporaryMessageId, messageId);
      return;
    }

    final String mainWaveRecipient = hostingProvider
        .getWaveParticipantIdFromIncomingEmailAddress(recipient);
    if (mainWaveRecipient == null) {
      logger.info("Discarding email to invalid recipient: " + recipient);
      return;
    }
    logger.info("Incoming message for Wave recipient: " + mainWaveRecipient);

    // Extract the references (i.e. ancestors in the thread).
    Set<String> references = null;
    Field refField = message.getHeader().getField("references");
    if (refField != null) {
      references = new HashSet<String>();
      Matcher matcher = markupPattern.matcher(refField.getBody());
      while (matcher.find())
        references.add(matcher.group(1));
    }

    // Extracts the Wave participants.
    Set<String> waveParticipants = new HashSet<String>();
    waveParticipants.add(mainWaveRecipient);
    waveParticipants.addAll(getWaveParticipants(message.getFrom()));
    waveParticipants.addAll(getWaveParticipants(message.getTo()));
    waveParticipants.addAll(getWaveParticipants(message.getCc()));

    // TODO(taton) We should have a better representation of users.
    final PersistentEmail email = new PersistentEmail(messageId, references, waveParticipants);
    final EmailToProcess rawEmail = new EmailToProcess(messageId, content);
    storeMessage(rawEmail, email);
  }

  /**
   * @param addresses A list of mailbox address.
   * @return The set of Wave participant IDs corresponding to the mailbox list.
   */
  private Set<String> getWaveParticipants(List<? extends Address> addresses) {
    Set<String> participants = new HashSet<String>();
    if (addresses != null)
      for (Address address : addresses)
        if (address instanceof Mailbox)
          participants.add(getWaveParticipant((Mailbox) address));
    return participants;
  }

  /**
   * @param mailbox A mailbox.
   * @return The Wave ID corresponding to the mailbox.
   */
  private String getWaveParticipant(Mailbox mailbox) {
    final String addr = mailbox.getAddress();
    final String waveRecipient = hostingProvider.getWaveParticipantIdFromIncomingEmailAddress(addr);
    if (waveRecipient != null)
      return waveRecipient;
    else
      return hostingProvider.getRobotProxyForFromEmailAddress(addr);
  }

  /**
   * Stores an incoming email message in the data store. TODO(taton) Remove rawEmail when the active
   * API is ready.
   * 
   * @param rawEmail The raw email to be processed the next time the robot is triggered from a Wave
   *          server.
   * @param email The persistent email message to store.
   * @throws IOException
   */
  public void storeMessage(EmailToProcess rawEmail, PersistentEmail email) throws IOException {
    PersistenceManager pm = pmFactory.getPersistenceManager();
    Transaction tx = pm.currentTransaction();
    try {
      logger.info("Storing incoming email with Message-ID: " + email.getMessageId()
          + " for later processing.");
      tx.begin();
      pm.makePersistent(email);
      tx.commit();
      tx.begin();
      pm.makePersistent(rawEmail);
      tx.commit();
    } finally {
      if (tx.isActive())
        tx.rollback();
      pm.close();
    }
  }

  /**
   * Updates the message ID of a PersistentEmail created with a temporary message ID. This happens
   * when we receive an email sent by the robot and BCC'ed to ourself.
   * 
   * @param temporaryMessageId The temporary message ID of the PersistentEmail to update.
   * @param messageId The message ID to update the PersistentEmail with.
   */
  public void updateMessageIdInPersistentEmail(String temporaryMessageId, String messageId) {

    PersistenceManager pm = pmFactory.getPersistenceManager();
    Transaction tx = pm.currentTransaction();
    try {
      logger.info("Updating PersistentEmail with temporary message ID " + temporaryMessageId
          + " with effective Message ID " + messageId);
      tx.begin();
      PersistentEmail email = PersistentEmail.lookupByMessageId(pm, temporaryMessageId);
      if (email == null) {
        logger.warning("Unknown PersistentEmail with temporary message ID: " + temporaryMessageId);
        return;
      }
      email.setMessageId(messageId);
      tx.commit();
    } finally {
      if (tx.isActive())
        tx.rollback();
      pm.close();
    }
  }
}
