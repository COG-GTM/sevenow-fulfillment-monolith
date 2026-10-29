package com.sevenow.delivery.domain;

import com.sevenow.delivery.common.Address;
import com.sevenow.delivery.common.PersonName;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DriverService {

  private final DriverRepository driverRepository;

  public DriverService(DriverRepository driverRepository) {
    this.driverRepository = driverRepository;
  }

  public Driver createDriver(PersonName name, Address address) {
    return driverRepository.save(new Driver(name, address));
  }

  public void updateAvailability(long driverId, boolean available) {
    Driver driver = findDriverById(driverId);
    if (available)
      driver.noteAvailable();
    else
      driver.noteUnavailable();
  }

  @Transactional(readOnly = true)
  public Driver findDriverById(long driverId) {
    return driverRepository.findById(driverId).orElseThrow(() -> new DriverNotFoundException(driverId));
  }
}
