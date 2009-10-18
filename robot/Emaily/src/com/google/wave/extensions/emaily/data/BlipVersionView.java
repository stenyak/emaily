package com.google.wave.extensions.emaily.data;

import java.util.List;

import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdentityType;
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
  private String blipId;

  @Persistent
  private long version;
  
  @Persistent
  private boolean isLive;
  
  @Persistent
  private List<String> participants;
  
  @Persistent
  private String content;
  
  @Persistent
  private long timestamp;

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

  public boolean isLive() {
    return isLive;
  }

  public void setLive(boolean isLive) {
    this.isLive = isLive;
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

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }
}
