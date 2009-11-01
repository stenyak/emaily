package com.google.wave.extensions.emaily.data;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Set;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Key;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Blob;

/**
 * Persistent class to store incoming emails.
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

  /** The unprocessed email bytes stream. */
  @Persistent
  private Blob blob;

  /**
   * Creates a new Persistent email with the given byte array.
   * 
   * @param messageId This message ID.
   * @param references The message IDs of the ancestors in the thread this message belongs to.
   * @param waveParticipants The wave participant IDs this message is sent to.
   * @param data The email content as a byte array.
   */
  public PersistentEmail(String messageId, Set<String> references, Set<String> waveParticipants,
      byte[] data) {
    this.messageId = messageId;
    this.references = references;
    this.waveParticipants = waveParticipants;
    this.blob = new Blob(data);
  }

  /** @return An InputStream for the email content. */
  public InputStream getInputStream() {
    return new ByteArrayInputStream(blob.getBytes());
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
