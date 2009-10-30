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
  
  /**
   * Returns a Robot wave Id to proxy for a given email address.
   * 
   * @param email The email address.
   * @return The wave Id which is proxying for that email address.
   */
  public String getRobotWaveParticipantIdFromEmailAddress(String email);
  
  /**
   * Returns true if the robot what is running is a production version.
   * 
   * @return True if the currently running robot is a production version
   */
  public boolean isProductionVersion();

  /**
   * Returns a reply-to email address of a wavelet.
   * 
   * @param emailAddressToken The generated email address token for the wavelet view.
   * @return The email address which can be used as an outgoing email address, and which
   *         can be replied to.
   */
  public String getWaveletEmailAddress(String emailAddressToken);
}