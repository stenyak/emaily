package com.google.wave.extensions.emaily.util;

import java.io.IOException;

import org.apache.james.mime4j.field.address.Mailbox;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.Entity;
import org.apache.james.mime4j.message.Multipart;
import org.apache.james.mime4j.message.TextBody;

/**
 * Mail utilities.
 * 
 * @author taton
 */
public class MailUtil {

  private final static String EMAILY_MAILBOX_DOMAIN = "emaily.appspotmail.com";

  /**
   * Parses the wave address (participant ID) encoded in an emaily address.
   * 
   * @param mailbox The emaily mailbox address encoded as
   * @return The wave participant ID. Null if the email address is malformed.
   */
  public static String getWaveAddress(Mailbox mailbox) {
    if (!mailbox.getDomain().equals(EMAILY_MAILBOX_DOMAIN))
      return null;
    String localPart = mailbox.getLocalPart();
    int plus = localPart.lastIndexOf("+");
    if (plus == -1)
      return null;
    return localPart.substring(0, plus) + "@" + localPart.substring(plus + 1);
  }

  /**
   * Formats a MIME message into a text-only message.
   * 
   * @param sb The StringBuilder to append the text content to.
   * @param entity The mail entity to format as a text-only content.
   * @return True if the entity could be formatted as a text content, false
   *         otherwise (e.g. if this is an HTML document, an image or a base64
   *         encoded binary content).
   */
  public static boolean mimeEntityToText(StringBuilder sb, Entity entity) {
    if (entity.isMultipart()) {
      Multipart multipart = (Multipart) entity.getBody();
      // TODO(taton) Figure out how to handle the message preamble and epilogue?
      if (entity.getMimeType().equals("multipart/alternative")) {
        for (BodyPart part : multipart.getBodyParts()) {
          StringBuilder subsb = new StringBuilder();
          if (mimeEntityToText(subsb, part)) {
            sb.append(subsb.toString());
            return true;
          }
        }
        sb.append("No suitable formatting alternative for this email content");
        return false;
      } else {
        for (BodyPart part : multipart.getBodyParts()) {
          mimeEntityToText(sb, part); // Ignore result...
        }
        return true;
      }
    } else if (entity.getBody() instanceof TextBody) {
      TextBody body = (TextBody) entity.getBody();
      try {
        sb.append(StrUtil.readContent(body.getReader()));
        return true;
      } catch (IOException ioe) {
        sb.append(ioe.toString());
        return false;
      }
    } else {
      sb.append("Unknown content type: [").append(entity.getClass().getName())
          .append("]\n");
      return false;
    }
  }

  // There are no instance of this class.
  private MailUtil() {
  }
}
