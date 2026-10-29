package com.sevenow.inventory.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The 7NOW substitution rule, moved here from the monolith's {@code OrderService.makeOrderLineItems}.
 * For each requested product: unknown id -> {@link InvalidProductIdException}; enough on hand -> as is;
 * otherwise its {@code substituteProductId}, if that has enough on hand; otherwise the product is rejected
 * and the whole request fails with {@link ProductOutOfStockException} listing every rejected id.
 * "Enough on hand" means {@code onHandQuantity - already allocated to earlier lines >= quantity}, so duplicate
 * lines and shared substitutes are checked against their combined quantity (the monolith only checks {@code > 0}).
 */
public class SubstitutionRule {

  public List<ReservationItem> resolve(Store store, List<RequestedItem> requestedItems) {
    List<String> rejected = new ArrayList<>();
    List<ReservationItem> result = new ArrayList<>();
    Map<String, Integer> allocated = new HashMap<>();
    if (requestedItems == null || requestedItems.isEmpty()) {
      throw new IllegalArgumentException("items must not be empty");
    }
    for (RequestedItem item : requestedItems) {
      if (item == null || item.getProductId() == null) {
        throw new IllegalArgumentException("productId is required");
      }
      if (item.getQuantity() <= 0) {
        throw new IllegalArgumentException("quantity must be positive for " + item.getProductId());
      }
      Product requested = store.findProduct(item.getProductId())
              .orElseThrow(() -> new InvalidProductIdException(item.getProductId()));
      if (available(requested, allocated) >= item.getQuantity()) {
        allocated.merge(requested.getId(), item.getQuantity(), Integer::sum);
        result.add(new ReservationItem(requested.getId(), requested.getName(), requested.getPrice(), item.getQuantity(), null));
        continue;
      }
      Optional<Product> substitute = Optional.ofNullable(requested.getSubstituteProductId())
              .flatMap(store::findProduct)
              .filter(p -> available(p, allocated) >= item.getQuantity());
      if (substitute.isPresent()) {
        Product s = substitute.get();
        allocated.merge(s.getId(), item.getQuantity(), Integer::sum);
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

  private static int available(Product product, Map<String, Integer> allocated) {
    return product.getOnHandQuantity() - allocated.getOrDefault(product.getId(), 0);
  }
}
