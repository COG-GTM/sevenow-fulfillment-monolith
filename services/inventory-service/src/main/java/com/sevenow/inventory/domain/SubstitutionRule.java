package com.sevenow.inventory.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The 7NOW substitution rule, moved here from the monolith's {@code OrderService.makeOrderLineItems}.
 * For each requested product: unknown id -> {@link InvalidProductIdException}; enough on hand -> as is;
 * otherwise its {@code substituteProductId}, if that has enough on hand; otherwise the product is rejected
 * and the whole request fails with {@link ProductOutOfStockException} listing every rejected id.
 * "Enough on hand" means {@code onHandQuantity >= quantity} (the monolith only checks {@code > 0}).
 */
public class SubstitutionRule {

  public List<ReservationItem> resolve(Store store, List<RequestedItem> requestedItems) {
    List<String> rejected = new ArrayList<>();
    List<ReservationItem> result = new ArrayList<>();
    for (RequestedItem item : requestedItems) {
      if (item.getQuantity() <= 0) {
        throw new IllegalArgumentException("quantity must be positive for " + item.getProductId());
      }
      Product requested = store.findProduct(item.getProductId())
              .orElseThrow(() -> new InvalidProductIdException(item.getProductId()));
      if (requested.hasOnHand(item.getQuantity())) {
        result.add(new ReservationItem(requested.getId(), requested.getName(), requested.getPrice(), item.getQuantity(), null));
        continue;
      }
      Optional<Product> substitute = Optional.ofNullable(requested.getSubstituteProductId())
              .flatMap(store::findProduct)
              .filter(p -> p.hasOnHand(item.getQuantity()));
      if (substitute.isPresent()) {
        Product s = substitute.get();
        result.add(new ReservationItem(s.getId(), s.getName(), s.getPrice(), item.getQuantity(), requested.getId()));
      } else {
        rejected.add(requested.getId());
      }
    }
    if (!rejected.isEmpty()) {
      throw new ProductOutOfStockException(rejected);
    }
    return result;
  }
}
