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

import java.util.List;

import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Text;

/**
 * A blip object, which stores the current state of a blip in a wavelet.
 * 
 * @author dlux
 * 
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class BlipData {
  /**
   * Id of the blip data: a generated id.
   */
  @SuppressWarnings("unused")
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  @Extension(vendorName = "datanucleus", key = "gae.encoded-pk", value = "true")
  @PrimaryKey
  private String id;

  @Persistent(mappedBy = "unsentBlips")
  private WaveletData waveletData;

  // Other fields
  /**
   * Id of the blip.
   */
  @Persistent
  private String blipId;

  /**
   * Version number of the blip.
   */
  @Persistent
  private long version;

  /**
   * The wave id of the blip creator.
   */
  @Persistent
  private String creator;

  /**
   * List of contributors to the blip.
   */
  @Persistent
  private List<String> contributors;

  /**
   * Content of the blip.
   */
  @Persistent
  private Text content;

  /**
   * The timestamp when this blip is first edited.
   */
  @Persistent
  private long firstEditedTimestamp;

  /**
   * Latest submit time of the blip.
   */
  @Persistent
  private long lastSubmittedTimestamp;

  /**
   * Timestamp of the last change in the blip.
   */
  @Persistent
  private long lastChangedTimestamp;

  /**
   * Timestamp when the user pressed the "Send" button to request a manual send.
   */
  @Persistent
  private long manualSendRequestTimestamp;

  // The following fields are calculated when necessary. See EmailSchedulingCalculator.java
  @NotPersistent
  private long timeToBecomeSendable;

  public BlipData(WaveletData waveletData, String blipId, String creator) {
    this.waveletData = waveletData;
    this.blipId = blipId;
    this.creator = creator;
  }

  // Accessors
  public WaveletData getWaveletData() {
    return waveletData;
  }

  public String getBlipId() {
    return blipId;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public List<String> getContributors() {
    return contributors;
  }

  public void setContributors(List<String> contributors) {
    this.contributors = contributors;
  }

  public String getContent() {
    if (content == null)
      return null;
    return content.getValue();
  }

  public void setContent(String content) {
    this.content = new Text(content);
  }

  public long getFirstEditedTimestamp() {
    return firstEditedTimestamp;
  }

  public void setFirstEditedTimestamp(long firstEditedTimestamp) {
    this.firstEditedTimestamp = firstEditedTimestamp;
  }

  public long getLastSubmittedTimestamp() {
    return lastSubmittedTimestamp;
  }

  public void setLastSubmittedTimestamp(long lastSubmittedTimestamp) {
    this.lastSubmittedTimestamp = lastSubmittedTimestamp;
  }

  public long getLastChangedTimestamp() {
    return lastChangedTimestamp;
  }

  public void setLastChangedTimestamp(long lastChangedTimestamp) {
    this.lastChangedTimestamp = lastChangedTimestamp;
  }

  public long getManualSendRequestTimestamp() {
    return manualSendRequestTimestamp;
  }

  public void setManualSendRequestTimestamp(long manualSendRequestTimestamp) {
    this.manualSendRequestTimestamp = manualSendRequestTimestamp;
  }

  public long getTimeToBecomeSendable() {
    return timeToBecomeSendable;
  }

  public void setTimeToBecomeSendable(long timeToBecomeSendable) {
    this.timeToBecomeSendable = timeToBecomeSendable;
  }

  @Override
  public String toString() {
    return "BlipData(id:" + getBlipId() + ")";
  }
}