package com.sevenow.delivery.web;

import com.sevenow.delivery.domain.Driver;
import com.sevenow.delivery.domain.DriverService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
public class DriverController {

  private final DriverService driverService;

  public DriverController(DriverService driverService) {
    this.driverService = driverService;
  }

  @RequestMapping(path = "/drivers", method = RequestMethod.POST)
  public Map<String, Long> create(@RequestBody CreateDriverRequest request) {
    Driver driver = driverService.createDriver(request.getName(), request.getAddress());
    return Collections.singletonMap("id", driver.getId());
  }

  @RequestMapping(path = "/drivers/{driverId}/availability", method = RequestMethod.POST)
  public ResponseEntity<Void> updateAvailability(@PathVariable long driverId, @RequestBody DriverAvailability availability) {
    driverService.updateAvailability(driverId, availability.isAvailable());
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @RequestMapping(path = "/drivers/{driverId}", method = RequestMethod.GET)
  public Driver get(@PathVariable long driverId) {
    return driverService.findDriverById(driverId);
  }
}
