package com.sevenow.fulfillment.driverservice.domain;


import com.sevenow.fulfillment.common.Address;
import com.sevenow.fulfillment.common.PersonName;
import com.sevenow.fulfillment.domain.Driver;
import com.sevenow.fulfillment.domain.DriverRepository;
import com.sevenow.fulfillment.security.SevenowCaller;
import org.springframework.transaction.annotation.Transactional;

/**
 * Only a dispatcher may register or approve a driver; a driver's availability and record are readable and writable by
 * that driver or a dispatcher. New drivers are unapproved and are never scheduled until a dispatcher approves them.
 */
public class DriverService {

  private DriverRepository driverRepository;

  public DriverService(DriverRepository driverRepository) {
    this.driverRepository = driverRepository;
  }

  @Transactional
  public void updateAvailability(long driverId, boolean available) {
    SevenowCaller.requireDriverOrDispatcher(driverId);
    Driver driver = tryToFindDriver(driverId);
    if (available)
      driver.noteAvailable();
    else
      driver.noteUnavailable();
  }

  @Transactional
  public Driver createDriver(PersonName name, Address address) {
    SevenowCaller.requireDispatcher();
    Driver driver = new Driver(name, address);
    driverRepository.save(driver);
    return driver;
  }

  @Transactional
  public void approve(long driverId) {
    SevenowCaller.requireDispatcher();
    tryToFindDriver(driverId).noteApproved();
  }

  @Transactional(readOnly = true)
  public Driver findDriverForCaller(long driverId) {
    SevenowCaller.requireDriverOrDispatcher(driverId);
    return tryToFindDriver(driverId);
  }

  private Driver tryToFindDriver(long driverId) {
    return driverRepository.findById(driverId).orElseThrow(() -> new DriverNotFoundException(driverId));
  }

}
