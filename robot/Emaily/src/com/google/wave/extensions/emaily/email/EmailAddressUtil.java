package com.google.wave.extensions.emaily.email;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Singleton;

@Singleton
public class EmailAddressUtil {
  // TODO(dlux): Make the email format parts injectable.
  // TODO(dlux): Use the something@emaily-wave.appspot.com email format if wave
  //   and appengine will support them.

  private final Pattern decodeFromEmailyDomainPattern = Pattern.compile(
      "(?:(?:\\d+)\\.latest\\.)?emaily-wave\\+(.*)\\+(.*)\\@appspot.com");

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
   *     original email or wave address was not in emaily domain.
   */
  public String decodeFromEmailyDomain(String encoded) {
    Matcher m = decodeFromEmailyDomainPattern.matcher(encoded);
    if (!m.matches()) {
      return null;
    }
    return m.group(1) + "@" + m.group(2);
  }
}
