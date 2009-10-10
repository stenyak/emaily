package com.google.wave.extensions.emaily.email;

import static com.google.wave.extensions.emaily.robot.EmailyProfileServlet.APPSPOT_ID;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Singleton;

@Singleton
public class EmailAddressUtil {
  // TODO(dlux): Make the email format parts injectable.
  // TODO(dlux): Use the something@emaily-wave.appspot.com email format if wave
  // and appengine will support them.

  private final Pattern decodeFromEmailyDomainPattern = Pattern
      .compile("(?:(?:\\d+)\\.latest\\.)?" + APPSPOT_ID
          + "\\+(.*)\\+(.*)\\@appspot.com");

  /**
   * Returns the address (email or wave) which was encoded in emaily domain.
   * <p>
   * Example:
   * <code>
   *   emaily-wave+test1+example.com@appspot.com -> test1.example.com
   * </code>
   * 
   * @param encoded The encoded email address or wave participant id.
   * @return The decoded email address or wave id. It returns null if the
   *         original email or wave address was not in emaily domain.
   */
  public String decodeFromEmailyDomain(String encoded) {
    Matcher m = decodeFromEmailyDomainPattern.matcher(encoded);
    if (!m.matches()) {
      return null;
    }
    return m.group(1) + "@" + m.group(2);
  }

  /**
   * Encodes a wave id or email address to emaily domain.
   * <p>
   * Example:
   * <code>
   *   test1.example.com -> emaily-wave+test1+example.com@appspot.com
   * </code>
   * 
   * @param data The email address or wave id to encode. It requires to be a
   *          full email address or a full wave id with a domain, and must not
   *          contain any more information (name, etc.), besides the pure
   *          address.
   * @return A wave participant Id.
   */
  public String encodeToEmailyDomain(String data) {
    int at = data.lastIndexOf('@');
    if (at < 0) {
      throw new RuntimeException("Invalid email address: " + data);
    }
    return APPSPOT_ID + "+" + data.substring(0, at) + "+"
        + data.substring(at + 1) + "@appspot.com";
  }
}
