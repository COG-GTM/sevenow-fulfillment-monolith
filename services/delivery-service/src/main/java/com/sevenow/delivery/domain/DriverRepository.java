package com.sevenow.delivery.domain;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DriverRepository extends CrudRepository<Driver, Long> {

  @Query("SELECT c FROM Driver c WHERE c.available = true")
  List<Driver> findAllAvailable();

  @Query("SELECT DISTINCT d FROM Driver d JOIN d.plan.actions a WHERE a.orderId = :orderId")
  List<Driver> findByOrderId(@Param("orderId") long orderId);
}
