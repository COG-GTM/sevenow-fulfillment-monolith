package com.sevenow.inventory.domain;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface StoreRepository extends CrudRepository<Store, Long> {

  /**
   * Atomically takes {@code quantity} units of a product. Returns 0 (and changes nothing) when the
   * store does not hold that many, so concurrent reservations can never drive stock negative.
   */
  @Modifying
  @Query(value = "update store_products set on_hand_quantity = on_hand_quantity - :quantity " +
          "where store_id = :storeId and id = :productId and on_hand_quantity >= :quantity", nativeQuery = true)
  int decrementOnHand(@Param("storeId") long storeId, @Param("productId") String productId, @Param("quantity") int quantity);

  @Modifying
  @Query(value = "update store_products set on_hand_quantity = on_hand_quantity + :quantity " +
          "where store_id = :storeId and id = :productId", nativeQuery = true)
  int incrementOnHand(@Param("storeId") long storeId, @Param("productId") String productId, @Param("quantity") int quantity);
}
