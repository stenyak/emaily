package com.google.wave.extensions.emaily.config;

/**
 * This interface encaptulates hosting specific functions and configuration.
 * 
 * @author dlux
 */
public interface HostingProvider {
  /**
   * Returns an email address of a wave participant in the Emaily domain. This
   * is used as an outgoing email address and this email address can receive
   * emails from the outside.
   * 
   * @param waveParticipantId
   * @return
   */
  public String getEmailAddressForWaveParticipantIdInEmailyDomain(String waveParticipantId);

  /**
   * Returns an email address from the "proxying for" argument of the robot
   * query.
   * 
   * @param proxyingFor The "proxying for" argument of the robot json call. If
   *          it is an email address, then it is in the "user+host" format.
   * @return The email address which belongs to the "proxying for argument. It
   *         returns null if it is not an email address.
   */
  public String getEmailAddressFromRobotProxyFor(String proxyingFor);

  /**
   * Decodes and returns the Wave participant ID from the recipient address of an incoming email.
   * For example:
   * "user_id+domain.com@app_id.appspotmail.com" will be decoded as "user_id@domain.com"
   * 
   * @param address The recipient address of the incoming email.
   * @return The Wave participant ID.
   */
  public String getWaveParticipantIdFromIncomingEmailAddress(String address);

  /**
   * Encodes the address of an Email participant as a proxyingFor Wave participant ID.
   * For example:
   * "user_id@domain.com" will be encoded as "app_id+user_id+domain.com@appspot.com".
   * 
   * @param address The email participant address.
   * @return The Wave participant ID proxying for the email participant.
   */
  public String getRobotProxyForFromEmailAddress(String address);
  
}