package com.google.wave.extensions.emaily.data;

import java.util.List;

import javax.jdo.annotations.IdentityType;
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
  // The ID of this entity consists of two things: the wavelet Id and the email
  // address of the user, separated by a ' '.
  @PrimaryKey
  @Persistent
  private String id; 
  
  @Persistent
  private long version;
  
  @Persistent
  private String emailAddressToken;
  
  @Persistent
  private List<BlipVersionView> unsentBlips;
  
  @Persistent
  private List<BlipVersionView> sentBlips;
  
  @Persistent
  private Long nextSendTimestamp;  // nullable

  // Accessors for the composite primary key
  public String getWaveletId() {
    return splitId()[0];
  }
  
  public String getEmail() {
    return splitId()[1];
  }
  
  public void setWaveletId(String waveletId) {
    setId(waveletId, getEmail());
  }
  
  public void setEmail(String email) {
    setId(getWaveletId(), email);
  }
  
  public void setId(String waveletId, String email) {
    id = waveletId + ' ' + email;
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

  public Long getNextSendTimestamp() {
    return nextSendTimestamp;
  }

  public void setNextSendTimestamp(Long nextSendTimestamp) {
    this.nextSendTimestamp = nextSendTimestamp;
  }

  // utility function: split id into two parts
  private String[] splitId() {
    if (id == null) {
      return new String[] { "", "" };
    }
    int at = id.indexOf(' ');
    if (at < 0) {
      return new String[] { id, "" };
    }
    return new String[] { id.substring(0, at), id.substring(at + 1) };
  }


}
