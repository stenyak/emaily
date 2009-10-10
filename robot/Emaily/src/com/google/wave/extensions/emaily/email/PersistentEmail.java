package com.google.wave.extensions.emaily.email;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.jdo.annotations.PersistenceCapable;

/**
 * Persistent class to store incoming emails until we can process them and
 * create Waves.
 * 
 * @author taton
 */
@PersistenceCapable
public class PersistentEmail implements Serializable {

  private static final long serialVersionUID = 8472461757525198617L;

  /**
   * Reads an InputStream into a byte array.
   * 
   * @param is The input stream.
   * @return The byte array.
   * @throws IOException
   */
  public static byte[] readInputStream(InputStream is) throws IOException {
    final int BUFFER_SIZE = 1024;
    List<byte[]> buffers = new ArrayList<byte[]>();
    int size = 0;
    // First read the input buffer into a list a fixed size buffers.
    while (true) {
      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead = is.read(buffer);
      if (bytesRead == -1) // End of file
        break;
      size += bytesRead;
      buffers.add(buffer);
    }
    // Then concatenate the buffers into a single complete byte array.
    byte[] data = new byte[size];
    int offset = 0;
    int remaining = size;
    for (byte[] buffer : buffers) {
      int bytesToCopy = Math.min(BUFFER_SIZE, remaining);
      System.arraycopy(buffer, 0, data, offset, bytesToCopy);
      remaining -= bytesToCopy;
      offset += bytesToCopy;
    }
    return data;
  }

  /** The incoming email unprocessed bytes stream. */
  private byte[] data;

  /**
   * Creates a new Persistent email with the given byte array.
   * 
   * @param data The email content as a byte array.
   */
  public PersistentEmail(byte[] data) {
    this.data = data;
  }

  /** @return An InputStream for the email content. */
  public InputStream getInputStream() {
    return new ByteArrayInputStream(data);
  }
}
