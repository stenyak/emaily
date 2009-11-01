package com.google.wave.extensions.emaily.data;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Container to store the ID of an incoming message that has not been processed.
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class EmailToProcess {

  /** The email message-ID. */
  @PrimaryKey
  @Persistent
  private String messageId;

  public EmailToProcess(String id) {
    this.messageId = id;
  }

  public String getMessageId() {
    return messageId;
  }
}
