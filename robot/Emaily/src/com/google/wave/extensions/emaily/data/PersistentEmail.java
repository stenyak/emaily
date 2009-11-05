package com.google.wave.extensions.emaily.data;

import java.io.Serializable;
import java.util.Set;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Key;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Container for long-term email messages storage. We only keep a few elements from the incoming
 * emails.
 * 
 * @author taton
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class PersistentEmail implements Serializable {

  private static final long serialVersionUID = 8472461757525198617L;

  /** The Message-ID header. */
  @PrimaryKey
  @Persistent
  private String messageId;

  /** The wavelet ID this message is part of. */
  @Key
  private String waveletId;

  /** The message IDs of the ancestors to this email in the thread. */
  @Persistent
  private Set<String> references;

  @Persistent
  private Set<String> waveParticipants;

  /**
   * Creates a new Persistent email with the given byte array.
   * 
   * @param messageId This message ID.
   * @param references The message IDs of the ancestors in the thread this message belongs to.
   * @param waveParticipants The wave participant IDs this message is sent to.
   */
  public PersistentEmail(String messageId, Set<String> references, Set<String> waveParticipants) {
    this.messageId = messageId;
    this.references = references;
    this.waveParticipants = waveParticipants;
  }

  /** @return The email Message-ID. */
  public String getMessageId() {
    return this.messageId;
  }

  /** @return The ID of the Wavelet containing this message. */
  public String getWaveletId() {
    return this.waveletId;
  }

  /**
   * Sets the ID of the Wavelet containing this message.
   * 
   * @param waveletId The Wavelet ID.
   */
  public void setWaveletId(String waveletId) {
    this.waveletId = waveletId;
  }

  /** @return The IDs of the Wave participants included in this message. */
  public Set<String> getWaveParticipants() {
    return this.waveParticipants;
  }

  /** @return The message IDs referenced by this message (i.e. the ancestors in the thread). */
  public Set<String> getReferences() {
    return this.references;
  }

}
