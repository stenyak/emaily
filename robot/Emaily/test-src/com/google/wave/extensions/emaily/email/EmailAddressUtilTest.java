package com.google.wave.extensions.emaily.email;

import static org.junit.Assert.*;

import org.junit.Test;

public class EmailAddressUtilTest {
  private EmailAddressUtil emailAddressUtil = new EmailAddressUtil();

  @Test
  public void testWaveParticipantIdToEmailAddress() {
    assertEquals("test1+test@example.com",
        emailAddressUtil.waveParticipantIdToEmailAddress(
            "emaily-wave+test1+test+example.com@appspot.com"));
    assertEquals("test1+test@example.com",
        emailAddressUtil.waveParticipantIdToEmailAddress(
	    "2.latest.emaily-wave+test1+test+example.com@appspot.com"));
    assertEquals(null,
        emailAddressUtil.waveParticipantIdToEmailAddress("hello@world.com"));
    assertEquals(null,
        emailAddressUtil.waveParticipantIdToEmailAddress(
            "emaily-wave@appspot.com"));
  }

  @Test
  public void testEmailAddressToWaveId() {
    assertEquals("emaily-wave+test1+test+example.com@appspot.com",
        emailAddressUtil.emailAddressToWaveParticipantId(
            "test1+test@example.com"));
  }
  
  @Test(expected = RuntimeException.class)
  public void testEmailAddressToWaveIdInvalidEmail() {
    emailAddressUtil.emailAddressToWaveParticipantId("1234");
  }

}
