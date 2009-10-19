package com.google.wave.extensions.emaily.email;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Set;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Key;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Blob;

/**
 * Persistent class to store incoming emails until we can process them and
 * create Waves.
 * 
 * @author taton
 */
@PersistenceCapable(identityType = IdentityType.DATASTORE)
public class PersistentEmail implements Serializable {

  private static final long serialVersionUID = 8472461757525198617L;

  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  private Long id;

  /** The Message-ID header. */
  @Key
  @Persistent
  private String messageId;

  @Persistent
  private Set<String> waveParticipants;

  /** The unprocessed email bytes stream. */
  @Persistent
  private Blob blob;

  /**
   * Creates a new Persistent email with the given byte array.
   * 
   * @param data The email content as a byte array.
   */
  public PersistentEmail(String messageId, Set<String> waveParticipants, byte[] data) {
    this.messageId = messageId;
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

  public Set<String> getWaveParticipants() {
    return this.waveParticipants;
  }

}
