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
import javax.jdo.annotations.Key;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.Order;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Data object for a wavelet view. A wavelet view is a wavelet from an email user's perspective.
 * 
 * @author dlux
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class WaveletView {

  @NotPersistent
  private static final Random randomGenerator = new Random();

  /**
   * The ID of this entity consists of two things: the wavelet Id and the email address of the user,
   * separated by a ' '.
   */
  @PrimaryKey
  @Persistent
  private String id;

  @Persistent
  private long version;

  /** A unique token used in group discussion only. */
  @Key
  @Persistent
  private String emailAddressToken;

  /** The title of the Wavelet. */
  @Persistent
  private String title;

  /** The ID of the root blip of the Wavelet. */
  @Persistent
  private String rootBlipId;

  @Persistent(mappedBy = "waveletView")
  @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "firstEditedTimestamp asc"))
  private List<BlipVersionView> unsentBlips;

  // TODO(dlux): once the storage is sorted out, store the sent blips also.
  // @Persistent
  // @Order(extensions = @Extension(vendorName="datanucleus", key="list-ordering",
  // value="firstEditedTimestamp asc"))
  // private List<BlipVersionView> sentBlips = new ArrayList<BlipVersionView>();

  @Persistent
  private long lastEmailSentTime;

  /** nullable: null means infinity. */
  @Persistent
  private Long timeForSending;

  /**
   * Constructor for an empty object with the required fields and default values.
   * 
   * @param waveletId
   * @param email
   */
  public WaveletView(String waveId, String waveletId, String email, String rootBlipId) {
    this.id = buildId(waveId, waveletId, email);
    this.emailAddressToken = generateNewEmailAddressToken();
    this.unsentBlips = new ArrayList<BlipVersionView>();
    // this.sentBlips = new ArrayList<BlipVersionView>();
    this.rootBlipId = rootBlipId;
  }

  /** @return The Wave ID. */
  public String getWaveId() {
    return splitId(id)[0];
  }

  /** @return The Wavelet ID. */
  public String getWaveletId() {
    return splitId(id)[1];
  }

  /** @return The email recipient address this WaveletView is addressed to. */
  public String getEmail() {
    return splitId(id)[2];
  }

  /** @return The synthetic ID of the WaveletView. */
  public String getId() {
    return id;
  }

  /** Builds a synthetic WaveletView ID from the Wavelet Id and email recipient address. */
  public static String buildId(String waveId, String waveletId, String email) {
    return waveId + ' ' + waveletId + ' ' + email;
  }

  /**
   * Splits a synthetic WaveletView ID into its Wavelet ID and its recipient email address.
   * 
   * @return the Wave ID, Wavelet ID and the recipient email address.
   */
  private static String[] splitId(String id) {
    String[] idArray = id.split(" ", 3);
    // Newest key format:
    if (idArray.length == 3) return idArray;
    // Without waveId (temporary support):
    if (idArray.length == 2) {
      String[] newIdArray = new String[3];
      newIdArray[0] = "";
      newIdArray[1] = idArray[0];
      newIdArray[2] = idArray[1];
      return newIdArray;
    }
    // Invalid id:
    throw new IllegalArgumentException("Invalid key");
  }

  /**
   * @return A new unique token to embed in the from or reply-to email address to link back to this
   *         WaveletView.
   */
  private static String generateNewEmailAddressToken() {
    long randomNumber = randomGenerator.nextLong();
    if (randomNumber > 0) {
      return Long.toString(randomNumber);
    } else {
      return Long.toString(-randomNumber) + "x";
    }
  }

  // Accessors for the rest of the fields

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  /** @return The token to use to identify group discussions. */
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
    // return sentBlips;
    return new ArrayList<BlipVersionView>();
  }

  public void setSentBlips(List<BlipVersionView> sentBlips) {
    // this.sentBlips = sentBlips;
  }

  /** @return The time in milliseconds since epoch when the last email was sent. */
  public long getLastEmailSentTime() {
    return lastEmailSentTime;
  }

  public void setLastEmailSentTime(long lastEmailSentTime) {
    this.lastEmailSentTime = lastEmailSentTime;
  }

  /** @return The time in milliseconds since epoch after which a new email is sent. */
  public long getTimeForSending() {
    if (timeForSending == null)
      return Long.MAX_VALUE;
    return timeForSending;
  }

  public void setTimeForSending(long timeForSending) {
    if (timeForSending == Long.MAX_VALUE) {
      this.timeForSending = null;
    } else {
      this.timeForSending = timeForSending;
    }
  }

  /** @return The title of the Wavelet. */
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  /** @return The root blip ID of the Wavelet. */
  public String getRootBlipId() {
    return rootBlipId;
  }

  public void setRootBlipId(String rootBlipId) {
    this.rootBlipId = rootBlipId;
  }
}
