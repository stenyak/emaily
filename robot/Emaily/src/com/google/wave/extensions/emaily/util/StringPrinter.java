package com.google.wave.extensions.emaily.util;

import java.util.Collection;

/**
 * Enhanced StringBuilder. StringBuilder cannot be extended, thus we wrap it.
 * 
 * @author taton
 */
public class StringPrinter {

  private StringBuilder sb = new StringBuilder();

  public StringPrinter() {
  }

  public StringPrinter print(String str) {
    sb.append(str);
    return this;
  }

  public StringPrinter println(String str) {
    sb.append(str).append('\n');
    return this;
  }

  public StringPrinter printf(String format, Object... args) {
    sb.append(String.format(format, args));
    return this;
  }

  public StringPrinter printfln(String format, Object... args) {
    printf(format, args);
    sb.append('\n');
    return this;
  }

  public <T> StringPrinter join(Collection<T> objects, String separator) {
    sb.append(StrUtil.join(objects, separator));
    return this;
  }

  public <T> StringPrinter join(T[] objects, String separator) {
    sb.append(StrUtil.join(objects, separator));
    return this;
  }

  public <T> StringPrinter joinln(Collection<T> objects, String separator) {
    join(objects, separator);
    sb.append('\n');
    return this;
  }

  public StringPrinter println(Object[] objects, String separator) {
    join(objects, separator);
    sb.append('\n');
    return this;
  }

  public StringPrinter cat(Object... objects) {
    for (Object object : objects)
      sb.append(object);
    return this;
  }

  public StringPrinter catln(Object... objects) {
    cat(objects);
    sb.append('\n');
    return this;
  }

  public int length() {
    return sb.length();
  }

  @Override
  public String toString() {
    return sb.toString();
  }
}
