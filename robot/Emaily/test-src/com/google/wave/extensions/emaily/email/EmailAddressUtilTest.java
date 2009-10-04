package com.google.wave.extensions.emaily.email;

import static org.junit.Assert.*;

import org.junit.Test;

public class EmailAddressUtilTest {
  private EmailAddressUtil emailAddressUtil = new EmailAddressUtil();

  @Test
  public void testdecodeFromEmailyDomain() {
    assertEquals("test1+test@example.com",
        emailAddressUtil.decodeFromEmailyDomain(
            "emaily-wave+test1+test+example.com@appspot.com"));
    assertEquals("test1+test@example.com",
        emailAddressUtil.decodeFromEmailyDomain(
	    "2.latest.emaily-wave+test1+test+example.com@appspot.com"));
    assertEquals(null,
        emailAddressUtil.decodeFromEmailyDomain("hello@world.com"));
    assertEquals(null,
        emailAddressUtil.decodeFromEmailyDomain(
            "emaily-wave@appspot.com"));
  }

  @Test
  public void testEncodeToEmailyDomain() {
    assertEquals("emaily-wave+test1+test+example.com@appspot.com",
        emailAddressUtil.encodeToEmailyDomain(
            "test1+test@example.com"));
  }
  
  @Test(expected = RuntimeException.class)
  public void testEncodeToEmailDomainEmpty() {
    emailAddressUtil.encodeToEmailyDomain("1234");
  }

}
