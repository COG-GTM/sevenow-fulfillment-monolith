package com.sevenow.fulfillment.domain;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DriverRepository extends CrudRepository<Driver, Long> {

  @Query("SELECT c FROM Driver c WHERE c.available = true")
  List<Driver> findAllAvailable();

}
