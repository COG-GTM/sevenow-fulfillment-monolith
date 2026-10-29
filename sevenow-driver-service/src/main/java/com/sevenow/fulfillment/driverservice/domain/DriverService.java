package com.sevenow.fulfillment.driverservice.domain;


import com.sevenow.fulfillment.common.Address;
import com.sevenow.fulfillment.common.PersonName;
import com.sevenow.fulfillment.domain.Driver;
import com.sevenow.fulfillment.domain.DriverRepository;
import org.springframework.transaction.annotation.Transactional;

public class DriverService {

  private DriverRepository driverRepository;

  public DriverService(DriverRepository driverRepository) {
    this.driverRepository = driverRepository;
  }

  @Transactional
  public void updateAvailability(long driverId, boolean available) {
    if (available)
      noteAvailable(driverId);
    else
      noteUnavailable(driverId);
  }

  @Transactional
  public Driver createDriver(PersonName name, Address address) {
    Driver driver = new Driver(name, address);
    driverRepository.save(driver);
    return driver;
  }

  void noteAvailable(long driverId) {
    driverRepository.findById(driverId).get().noteAvailable();
  }

  void noteUnavailable(long driverId) {
    driverRepository.findById(driverId).get().noteUnavailable();
  }

  public Driver findDriverById(long driverId) {
    return driverRepository.findById(driverId).get();
  }

}
