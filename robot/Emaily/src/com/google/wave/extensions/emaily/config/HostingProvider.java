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
}