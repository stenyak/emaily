package com.google.wave.extensions.emaily.email;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.extensions.emaily.config.HostingProvider;
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

  /**
   * Reads an InputStream into a byte array.
   * 
   * @param is The input stream.
   * @return The byte array.
   * @throws IOException
   */
  public static byte[] readInputStream(InputStream is) throws IOException {
    final int BUFFER_SIZE = 1024;
    List<byte[]> buffers = new ArrayList<byte[]>();
    int size = 0;
    // First read the input buffer into a list a fixed size buffers.
    while (true) {
      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead = is.read(buffer);
      if (bytesRead == -1) // End of file
        break;
      size += bytesRead;
      buffers.add(buffer);
    }
    // Then concatenate the buffers into a single complete byte array.
    byte[] data = new byte[size];
    int offset = 0;
    int remaining = size;
    for (byte[] buffer : buffers) {
      int bytesToCopy = Math.min(BUFFER_SIZE, remaining);
      System.arraycopy(buffer, 0, data, offset, bytesToCopy);
      remaining -= bytesToCopy;
      offset += bytesToCopy;
    }
    return data;
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

    final PersistentEmail email = new PersistentEmail(message.getMessageId(), waveParticipants,
        content);
    storeMessage(email);
  }

  /**
   * Stores a message in the datastore.
   * 
   * @param email The message to store.
   * @throws IOException
   */
  public void storeMessage(PersistentEmail email) throws IOException {
    PersistenceManager pm = pmFactory.getPersistenceManager();
    Transaction tx = pm.currentTransaction();
    try {
      tx.begin();
      pm.makePersistent(email);
      tx.commit();
    } finally {
      if (tx.isActive())
        tx.rollback();
      pm.close();
    }
  }
}
