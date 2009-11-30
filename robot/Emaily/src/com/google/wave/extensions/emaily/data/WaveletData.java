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

import java.math.BigInteger;
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
 * Data object for a wavelet. This contains information about a wavelet
 * 
 * @author dlux
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class WaveletData {

  private static final Random randomGenerator = new Random();

  /** Builds a synthetic WaveletData ID from the wave id and wavelet id. */
  public static String buildId(String waveId, String waveletId) {
    return waveId + ' ' + waveletId;
  }

  /**
   * Splits a synthetic WaveletData ID into its wave id and wavelet id and writes them into their
   * respective non-persistent fields.
   * 
   * @return the Wave ID, Wavelet ID.
   */
  private void splitId() {
    String[] idparts = id.split(" ", 2);
    waveId = idparts[0];
    waveletId = idparts[1];
  }

  /**
   * @return A new unique token to embed in the from or reply-to email address to link back to this
   *         wavelet.
   */
  private static String generateNewEmailAddressToken() {
    return new BigInteger(130, randomGenerator).toString(32);
  }

  /**
   * The ID of this entity consists of two things: the wavelet Id and the email address of the user,
   * separated by a ' '.
   */
  @PrimaryKey
  @Persistent
  private String id;

  @NotPersistent
  private String waveId;

  @NotPersistent
  private String waveletId;

  /**
   * The version number of the wavelet.
   */
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

  /**
   * Participants on the wave.
   */
  @Persistent
  private List<String> participants;

  /**
   * List of unsent blips on the wave.
   */
  @Persistent(mappedBy = "waveletData")
  @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "firstEditedTimestamp asc"))
  private List<BlipData> unsentBlips;

  // TODO(dlux): once the storage is sorted out, store the sent blips also.
  // @Persistent
  // @Order(extensions = @Extension(vendorName="datanucleus", key="list-ordering",
  // value="firstEditedTimestamp asc"))
  // private List<BlipData> sentBlips = new ArrayList<BlipData>();

  /**
   * The timestamp when the last email was sent from this wavelet.
   */
  @Persistent
  private long lastEmailSentTime;

  /**
   * Time for the next email sending from this wavelet.
   * 
   * Nullable: null means infinity: no email is scheduled.
   * */
  @Persistent
  private Long timeForSending;

  public enum SendMode {
    AUTOMATIC, MANUAL
  };

  /**
   * Whether this wavelet is automatic or manually sent.
   */
  @Persistent
  private SendMode sendMode;

  /**
   * Constructor for an empty object with the required fields and default values.
   * 
   * @param waveId The wave id
   * @param waveletId The wavelet id
   * @param rootBlipId The blip id of the root blip
   */
  public WaveletData(String waveId, String waveletId, String rootBlipId) {
    this.id = buildId(waveId, waveletId);
    this.emailAddressToken = generateNewEmailAddressToken();
    this.unsentBlips = new ArrayList<BlipData>();
    // this.sentBlips = new ArrayList<BlipData>();
    this.rootBlipId = rootBlipId;
    this.participants = new ArrayList<String>();
    this.sendMode = SendMode.AUTOMATIC;
  }

  // Accessors for the composite primary key

  /** @return The Wave ID. */
  public String getWaveId() {
    if (waveId == null)
      splitId();
    return waveId;
  }

  /** @return The Wavelet ID. */
  public String getWaveletId() {
    if (waveletId == null)
      splitId();
    return waveletId;
  }

  /** @return The synthetic ID of the wavelet data. */
  public String getId() {
    return id;
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

  public List<BlipData> getUnsentBlips() {
    return unsentBlips;
  }

  public void setUnsentBlips(List<BlipData> unsentBlips) {
    this.unsentBlips = unsentBlips;
  }

  public List<BlipData> getSentBlips() {
    // return sentBlips;
    return new ArrayList<BlipData>();
  }

  public void setSentBlips(List<BlipData> sentBlips) {
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

  public List<String> getParticipants() {
    return participants;
  }

  public void setParticipants(List<String> participants) {
    this.participants = participants;
  }

  public SendMode getSendMode() {
    return sendMode;
  }

  public void setSendMode(SendMode sendMode) {
    this.sendMode = sendMode;
  }

  @Override
  public String toString() {
    return "WaveletData(id:" + getId() + ")";
  }
}
