package com.google.wave.extensions.emaily.email;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.HashSet;
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
 * Processes incoming emails.
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
    final String mainEmailRecipient = URLDecoder.decode(uri.substring(REQUEST_URI_PREFIX.length()),
        "utf8");
    final String mainWaveRecipient = hostingProvider
        .getWaveParticipantIdFromIncomingEmailAddress(mainEmailRecipient);
    if (mainWaveRecipient == null) {
      logger.info("Discarding incoming email to invalid recipient: " + mainEmailRecipient);
      return;
    }
    logger.info("Incoming message for Wave recipient: " + mainWaveRecipient);
    final byte[] content = readInputStream(input);
    final Message message = new Message(new ByteArrayInputStream(content));
    if (logger.isLoggable(Level.FINE)) {
      logger.fine("Unprocessed email content:\n" + new String(content) + "\nEnd of email\n");
    }

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
    if (message.getTo() != null) {
      for (Address to : message.getTo()) {
        if (to instanceof Mailbox) {
          final Mailbox emailRecipient = (Mailbox) to;
          final String waveRecipient = hostingProvider
              .getWaveParticipantIdFromIncomingEmailAddress(emailRecipient.getAddress());
          if (waveRecipient != null)
            waveParticipants.add(waveRecipient);
          else
            waveParticipants.add(hostingProvider.getRobotProxyForFromEmailAddress(emailRecipient
                .getAddress()));
        }
        // TODO(taton) Handle groups of addresses.
      }
    }
    // TODO(taton) Handle cc'ed addresses as well.

    String messageId = null;
    if (message.getMessageId() == null) {
      logger.warning("Incoming email has no Message-ID:\n" + new String(content));
      // TODO(taton) Generate a message ID based on a hash of the email content?
      return;
    } else {
      Matcher matcher = markupPattern.matcher(message.getMessageId());
      if (!matcher.find()) {
        logger.warning("Incoming email has invalid Message-ID: " + message.getMessageId());
        return;
      }
      messageId = matcher.group(1);
    }

    final PersistentEmail pe = new PersistentEmail(messageId, references, waveParticipants);
    final EmailToProcess etp = new EmailToProcess(messageId, content);
    storeMessage(etp, pe);
  }

  /**
   * Stores an incoming email message in the data store.
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
      tx.begin();
      pm.makePersistent(email);
      tx.commit();
      tx.begin();
      pm.makePersistent(rawEmail);
      tx.commit();
      logger.info("Incoming email stored with ID " + pm.getObjectId(email));
    } finally {
      if (tx.isActive())
        tx.rollback();
      pm.close();
    }
  }
}
