package com.google.wave.extensions.emaily.data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdGeneratorStrategy;
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

  /**
   * Retrieve a PersistentEmail by its message ID.
   * 
   * @param pm The persistence manager.
   * @param messageId The message ID.
   * @return The PersistentEmail with the given message ID. Null if no such PersistentEmail exists.
   */
  public static PersistentEmail lookupByMessageId(PersistenceManager pm, String messageId) {
    Query query = pm.newQuery(PersistentEmail.class, "messageId == \"" + messageId + "\"");
    @SuppressWarnings("unchecked")
    List<PersistentEmail> list = (List<PersistentEmail>) query.execute();
    if (list.size() == 0)
      return null;
    if (list.size() > 1)
      throw new RuntimeException(
          "Datastore inconsistency: multiple PersistentEmail with message ID: " + messageId);
    return list.get(0);
  }

  @SuppressWarnings("unused")
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  @Extension(vendorName = "datanucleus", key = "gae.encoded-pk", value = "true")
  @PrimaryKey
  private String jdoId;

  /** The Message-ID header. Cannot be a primary key since we update it. */
  @Key
  @Persistent
  private String messageId;

  /** The wavelet ID this message is part of. */
  @Key
  private String waveletId;

  /** The wave ID of this message. */
  @Persistent
  private String waveId;

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

  /** Updates the Message-ID. */
  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  /** @return The ID of the Wavelet containing this message. */
  public String getWaveletId() {
    return this.waveletId;
  }

  /** @return The ID of the Wave containing this message. */
  public String getWaveId() {
    return this.waveId;
  }

  /**
   * Sets the Wave ID and the Wavelet ID this email message is part of.
   * 
   * @param waveId The Wave ID.
   * @param waveletId The Wavelet ID.
   */
  public void setWaveAndWaveletId(String waveId, String waveletId) {
    this.waveId = waveId;
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
