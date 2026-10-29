package com.sevenow.inventory.domain;

import com.sevenow.inventory.common.Address;
import com.sevenow.inventory.common.Money;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class SubstitutionRuleTest {

  private final Store store = new Store(1L, "38114", "7-Eleven #38114", new Address("3200 Hackberry Rd", null, "Irving", "TX", "75063"),
          new StoreCatalog(Arrays.asList(
                  new Product("MONSTER-16OZ", "Monster Energy (16 oz)", new Money("2.99"), 2),
                  new Product("RED-BULL-12OZ", "Red Bull Energy Drink (12 oz)", new Money("3.49"), 0, "MONSTER-16OZ"),
                  new Product("PIZZA-CHEESE-WHOLE", "Cheese Pizza (Whole)", new Money("6.99"), 0),
                  new Product("PIZZA-PEPPERONI-WHOLE", "Pepperoni Pizza (Whole)", new Money("7.99"), 0, "PIZZA-CHEESE-WHOLE"),
                  new Product("WINGS-8PC", "Chicken Wings (8 pc)", new Money("6.49"), 0))));

  private final SubstitutionRule rule = new SubstitutionRule();

  @Test
  public void inStockProductResolvesAsIs() {
    List<ReservationItem> items = rule.resolve(store, Collections.singletonList(new RequestedItem("MONSTER-16OZ", 2)));
    assertEquals("MONSTER-16OZ", items.get(0).getProductId());
    assertNull(items.get(0).getSubstitutedFromProductId());
  }

  @Test
  public void outOfStockProductResolvesToInStockSubstitute() {
    ReservationItem item = rule.resolve(store, Collections.singletonList(new RequestedItem("RED-BULL-12OZ", 1))).get(0);
    assertEquals("MONSTER-16OZ", item.getProductId());
    assertEquals("RED-BULL-12OZ", item.getSubstitutedFromProductId());
    assertEquals(new Money("2.99"), item.getPrice());
  }

  @Test
  public void rejectsEveryProductWithoutAnInStockSubstitute() {
    try {
      rule.resolve(store, Arrays.asList(new RequestedItem("WINGS-8PC", 1), new RequestedItem("MONSTER-16OZ", 1),
              new RequestedItem("PIZZA-PEPPERONI-WHOLE", 1)));
      fail();
    } catch (ProductOutOfStockException e) {
      assertEquals(Arrays.asList("WINGS-8PC", "PIZZA-PEPPERONI-WHOLE"), e.getRejectedProductIds());
    }
  }

  @Test
  public void rejectsWhenQuantityExceedsOnHandOfProductAndSubstitute() {
    try {
      rule.resolve(store, Collections.singletonList(new RequestedItem("RED-BULL-12OZ", 3)));
      fail();
    } catch (ProductOutOfStockException e) {
      assertEquals(Collections.singletonList("RED-BULL-12OZ"), e.getRejectedProductIds());
    }
  }

  @Test(expected = InvalidProductIdException.class)
  public void unknownProductIsInvalid() {
    rule.resolve(store, Collections.singletonList(new RequestedItem("NOPE", 1)));
  }

  @Test
  public void duplicateLinesAreCheckedAgainstCombinedQuantity() {
    try {
      rule.resolve(store, Arrays.asList(new RequestedItem("MONSTER-16OZ", 2), new RequestedItem("MONSTER-16OZ", 1)));
      fail();
    } catch (ProductOutOfStockException e) {
      assertEquals(Collections.singletonList("MONSTER-16OZ"), e.getRejectedProductIds());
    }
  }

  @Test
  public void substituteStockIsSharedWithDirectRequests() {
    try {
      rule.resolve(store, Arrays.asList(new RequestedItem("MONSTER-16OZ", 1), new RequestedItem("RED-BULL-12OZ", 2)));
      fail();
    } catch (ProductOutOfStockException e) {
      assertEquals(Collections.singletonList("RED-BULL-12OZ"), e.getRejectedProductIds());
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void missingItemsAreABadRequest() {
    rule.resolve(store, null);
  }
}
