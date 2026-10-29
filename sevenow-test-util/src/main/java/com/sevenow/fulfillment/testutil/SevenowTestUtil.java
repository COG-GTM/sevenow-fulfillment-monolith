package com.sevenow.fulfillment.testutil;

import java.util.Optional;

import static org.junit.Assert.assertTrue;

public class SevenowTestUtil {

  public static <T> void assertPresent(Optional<T> value) {
    assertTrue(value.isPresent());
  }
}
