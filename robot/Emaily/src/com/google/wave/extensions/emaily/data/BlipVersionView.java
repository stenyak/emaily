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

/**
 * A blip version object, which stores the current state of a blip in a wavelet view.
 * 
 * @author dlux
 * 
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class BlipVersionView {
  // Id of the blip view: a generated id.
  @SuppressWarnings("unused")
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  @Extension(vendorName = "datanucleus", key = "gae.encoded-pk", value = "true")
  @PrimaryKey
  private String id;

  @Persistent(mappedBy = "unsentBlips")
  private WaveletView waveletView;

  // Other fields
  @Persistent
  private String blipId;

  @Persistent
  private long version;

  @Persistent
  private List<String> participants;

  @Persistent
  private String content;

  @Persistent
  private long firstEditedTimestamp;

  @Persistent
  private long lastSubmittedTimestamp;

  @Persistent
  private long lastChangedTimestamp;
  
  // This field is calculated when necessary. See EmailScheduler.java
  @NotPersistent
  private long timeToBecomeSendable;

  public BlipVersionView(WaveletView waveletView, String blipId) {
    this.waveletView = waveletView;
    this.blipId = blipId;
  }

  @SuppressWarnings("unused")
  private BlipVersionView() {
  }

  // Accessors
  public WaveletView getWaveletView() {
    return waveletView;
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

  public List<String> getParticipants() {
    return participants;
  }

  public void setParticipants(List<String> participants) {
    this.participants = participants;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
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
  
  public long getTimeToBecomeSendable() {
    return timeToBecomeSendable;
  }

  public void setTimeToBecomeSendable(long timeToBecomeSendable) {
    this.timeToBecomeSendable = timeToBecomeSendable;
  }
}