package com.sevenow.delivery.domain;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DriverRepository extends CrudRepository<Driver, Long> {

  @Query("SELECT c FROM Driver c WHERE c.available = true")
  List<Driver> findAllAvailable();

  @Query("SELECT DISTINCT d FROM Driver d JOIN d.plan.actions a WHERE a.orderId = :orderId")
  List<Driver> findByOrderId(@Param("orderId") long orderId);

  /** Claims the order for scheduling; 1 if this transaction claimed it, 0 if it was already claimed (and committed). */
  @Modifying
  @Query(value = "insert ignore into delivery_assignments (order_id) values (:orderId)", nativeQuery = true)
  int claimOrder(@Param("orderId") long orderId);

  @Modifying
  @Query(value = "update delivery_assignments set driver_id = :driverId where order_id = :orderId", nativeQuery = true)
  int assignOrder(@Param("orderId") long orderId, @Param("driverId") Long driverId);
}
