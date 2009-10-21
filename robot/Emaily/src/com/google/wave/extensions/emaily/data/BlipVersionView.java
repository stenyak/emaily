package com.google.wave.extensions.emaily.data;

import java.util.List;

import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class BlipVersionView {
  // Id of the blip view: it is composed of the parent WaveletView id, plus the blip id, plus the version.
  @SuppressWarnings("unused")
  @PrimaryKey
  @Persistent
  @Extension(vendorName="datanucleus", key="gae.encoded-pk", value="true")
  private String id;
  
  @Persistent
  @Extension(vendorName="datanucleus", key="gae.parent-pk", value="true")
  private String parentId;

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
  
  @NotPersistent
  private long timeToBecomeSendable;
  
  public String getParentId() {
    return parentId;
  }
  
  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public String getBlipId() {
    return blipId;
  }

  public void setBlipId(String blipId) {
    this.blipId = blipId;
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
