package com.google.wave.extensions.emaily.data;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Blob;

/**
 * Container for short-term storage of raw unprocessed incoming email messages. This object is
 * deleted from the data store once the email has been processed and converted to a Wave.
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class EmailToProcess {

  /** The email message-ID. */
  @PrimaryKey
  @Persistent
  private String messageId;

  /** The unprocessed email bytes stream. */
  @Persistent
  private Blob blob;

  /**
   * The date (in milliseconds since epoch) when this email has been processed unsuccessfully. Null
   * if the email has never been processed yet.
   */
  @Persistent
  private Long processingDate;

  /**
   * Initializes an email to process.
   * 
   * @param id The email message ID.
   * @param data The raw unprocessed email bytes.
   */
  public EmailToProcess(String id, byte[] data) {
    this.messageId = id;
    this.blob = new Blob(data);
  }

  /** @returns The email message ID. */
  public String getMessageId() {
    return messageId;
  }

  /** @returns The first date when this email has been processed unsuccessfully. */
  public Long getProcessingDate() {
    return this.processingDate;
  }

  /** Sets the date this email has been processed. */
  public void setProcessingDate(long date) {
    this.processingDate = date;
  }

  /** @return An InputStream for the email content. */
  public InputStream getInputStream() {
    return new ByteArrayInputStream(blob.getBytes());
  }
}
