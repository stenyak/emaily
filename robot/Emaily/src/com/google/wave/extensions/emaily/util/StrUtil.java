package com.google.wave.extensions.emaily.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;

/**
 * String utilities.
 * 
 * @author taton
 */
public class StrUtil {

  /**
   * Formats a collection of objects using the specified separator.
   * 
   * @param objects The collection to format.
   * @param separator The separator to insert between the element in the
   *          collection.
   * @return The string representing the collection of objects.
   */
  public static String join(Collection<?> objects, String separator) {
    if (objects == null)
      return "";
    StringBuilder sb = new StringBuilder();
    for (Object o : objects) {
      if (sb.length() > 0)
        sb.append(separator);
      sb.append(o.toString());
    }
    return sb.toString();
  }

  /**
   * Formats an array of objects using the specified separator.
   * 
   * @param objects The array to format.
   * @param separator The separator to insert between the element in the
   *          collection.
   * @return The string representing the array of objects.
   */
  public static <T> String join(T[] objects, String separator) {
    if (objects == null)
      return "";
    StringBuilder sb = new StringBuilder();
    for (Object o : objects) {
      if (sb.length() > 0)
        sb.append(separator);
      sb.append(o.toString());
    }
    return sb.toString();
  }

  /**
   * Reads the content of a Reader into a string.
   * 
   * @param reader The reader to read from.
   * @return The content of the reader.
   * @throws IOException
   */
  public static String readContent(Reader reader) throws IOException {
    BufferedReader bufreader = new BufferedReader(reader);
    StringBuilder sb = new StringBuilder();
    while (true) {
      String line = bufreader.readLine();
      if (line == null) {
        return sb.toString();
      }
      sb.append(line).append('\n');
    }
  }

  /**
   * Reads the content of an InputStream into a string.
   * 
   * @param is The input stream to read from.
   * @return The input stream content.
   * @throws IOException
   */
  public static String readContent(InputStream is) throws IOException {
    return readContent(new BufferedReader(new InputStreamReader(is)));
  }

  // There are no instance of this class.
  private StrUtil() {
  }
}
