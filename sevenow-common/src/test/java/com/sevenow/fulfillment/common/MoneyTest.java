package com.sevenow.fulfillment.common;


import org.junit.Test;

import static org.junit.Assert.*;

public class MoneyTest {

  private final int M1_AMOUNT = 10;
  private final int M2_AMOUNT = 15;

  private Money m1 = new Money(M1_AMOUNT);
  private Money m2 = new Money(M2_AMOUNT);

  @Test
  public void shouldReturnAsString() {
    assertEquals(Integer.toString(M1_AMOUNT), new Money(M1_AMOUNT).asString());
  }

  @Test
  public void shouldCompare() {
    assertTrue(m2.isGreaterThanOrEqual(m2));
    assertTrue(m2.isGreaterThanOrEqual(m1));
    assertFalse(m1.isGreaterThanOrEqual(m2));
  }

  @Test
  public void shouldAdd() {
    assertEquals(new Money(M1_AMOUNT + M2_AMOUNT), m1.add(m2));
  }

  @Test
  public void shouldMultiply() {
    int multiplier = 12;
    assertEquals(new Money(M2_AMOUNT * multiplier), m2.multiply(multiplier));
  }

  @Test
  public void shouldAcceptBoundedAmounts() {
    assertEquals("12.34", new Money("12.34").asString());
    assertEquals("100", new Money("1E+2").asString());
    assertEquals("99999999999999999.9999", new Money("99999999999999999.9999").asString());
  }

  @Test
  public void shouldRejectUnboundedAmounts() {
    for (String amount : new String[]{"1e-200000000", "1e200000000", "1e2147483647", "1e-2147483648",
            "1.23456", "123456789012345678", "0000000000000000000000000000000001"}) {
      try {
        new Money(amount);
        fail("expected IllegalArgumentException for " + amount);
      } catch (IllegalArgumentException e) {
        // expected
      }
    }
  }



}