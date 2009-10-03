package com.google.wave.extensions.emaily.email;

import static org.junit.Assert.*;

import org.junit.Test;

public class EmailAddressUtilTest {
  private EmailAddressUtil emailAddressUtil = new EmailAddressUtil();

  @Test
  public void testWaveIdToEmailAddress() {
    assertEquals("test1+test@example.com", emailAddressUtil.waveIdToEmailAddress("emaily-wave+test1+test+example.com@appspot.com"));
    assertEquals(null, emailAddressUtil.waveIdToEmailAddress("hello@world.com"));
    assertEquals(null, emailAddressUtil.waveIdToEmailAddress("emaily-wave@appspot.com"));
  }

  @Test
  public void testEmailAddressToWaveId() {
    assertEquals("emaily-wave+test1+test+example.com@appspot.com", emailAddressUtil.emailAddressToWaveId("test1+test@example.com"));
  }
  
  @Test(expected= RuntimeException.class)
  public void testEmailAddressToWaveIdInvalidEmail() {
    emailAddressUtil.emailAddressToWaveId("1234");
  }

}
