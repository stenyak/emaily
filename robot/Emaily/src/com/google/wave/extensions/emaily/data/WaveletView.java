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
package com.google.wave.extensions.emaily.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.Order;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Data object for a wavelet view. A wavelet view is a wavelet from an email
 * user's perspective.
 * 
 * @author dlux
 * 
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class WaveletView {
  
  @NotPersistent
  private static final Random randomGenerator = new Random();
  
  // The ID of this entity consists of two things: the wavelet Id and the email
  // address of the user, separated by a ' '.
  @PrimaryKey
  @Persistent
  private String id; 
  
  // Fields
  @Persistent
  private long version;
  
  @Persistent
  private String emailAddressToken;
  
  @Persistent
  @Order(extensions = @Extension(vendorName="datanucleus", key="list-ordering", value="id asc"))
  private List<BlipVersionView> unsentBlips;
  
  @Persistent
  @Order(extensions = @Extension(vendorName="datanucleus", key="list-ordering", value="id asc"))
  private List<BlipVersionView> sentBlips;
  
  @Persistent
  private long lastEmailSentTime;

  @Persistent
  private Long timeForSending;  // nullable, null means: infinity
  
  /**
   * Constructor for an empty object with a key and default values.
   * 
   * @param waveletId
   * @param email
   */
  public WaveletView(String waveletId, String email) {
    this();
    id = buildId(waveletId, email);
    version = 0;
    emailAddressToken = generateNewEmailAddressToken();
    unsentBlips = new ArrayList<BlipVersionView>();
    sentBlips = new ArrayList<BlipVersionView>();
    lastEmailSentTime = 0;
    timeForSending = null;
  }

  private WaveletView() {
  }

  // Accessors for the composite primary key
  public String getWaveletId() {
    return splitId(id)[0];
  }
  
  public String getEmail() {
    return splitId(id)[1];
  }
  
  public void setWaveletId(String waveletId) {
    setId(waveletId, getEmail());
  }
  
  public void setEmail(String email) {
    setId(getWaveletId(), email);
  }
  
  public String getId() {
    return id;
  }
  
  private void setId(String waveletId, String email) {
    id = buildId(waveletId, email);
  }
  
  // utility functions  
  public static String buildId(String waveletId, String email) {
    return waveletId + ' ' + email;
  }

  private static String[] splitId(String id) {
    if (id == null) {
      return new String[] { "", "" };
    }
    int at = id.indexOf(' ');
    if (at < 0) {
      return new String[] { id, "" };
    }
    return new String[] { id.substring(0, at), id.substring(at + 1) };
  }

  private static String generateNewEmailAddressToken() {
    return Long.toString(randomGenerator.nextLong());
  }
  
  // Accessors for the rest of the fields
  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public String getEmailAddressToken() {
    return emailAddressToken;
  }

  public void setEmailAddressToken(String emailAddressToken) {
    this.emailAddressToken = emailAddressToken;
  }

  public List<BlipVersionView> getUnsentBlips() {
    return unsentBlips;
  }

  public void setUnsentBlips(List<BlipVersionView> unsentBlips) {
    this.unsentBlips = unsentBlips;
  }

  public List<BlipVersionView> getSentBlips() {
    return sentBlips;
  }

  public void setSentBlips(List<BlipVersionView> sentBlips) {
    this.sentBlips = sentBlips;
  }

  public long getLastEmailSentTime() {
    return lastEmailSentTime;
  }

  public void setLastEmailSentTime(long lastEmailSentTime) {
    this.lastEmailSentTime = lastEmailSentTime;
  }

  public Long getTimeForSending() {
    if (timeForSending == null) {
      return Long.MAX_VALUE;
    }
    return timeForSending;
  }
  
  public void setTimeForSending(Long timeForSending) {
    if (timeForSending == Long.MAX_VALUE) {
      this.timeForSending = null;
    } else {
      this.timeForSending = timeForSending;
    }
  }
}
