package com.sevenow.fulfillment.driverservice.web;

import com.sevenow.fulfillment.driverservice.api.DriverAvailability;
import com.sevenow.fulfillment.driverservice.api.CreateDriverRequest;
import com.sevenow.fulfillment.driverservice.api.CreateDriverResponse;
import com.sevenow.fulfillment.driverservice.domain.DriverService;
import com.sevenow.fulfillment.domain.Driver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class DriverController {

  private DriverService driverService;

  public DriverController(DriverService driverService) {
    this.driverService = driverService;
  }

  @RequestMapping(path="/drivers", method= RequestMethod.POST)
  public ResponseEntity<CreateDriverResponse> create(@RequestBody CreateDriverRequest request) {
    Driver driver = driverService.createDriver(request.getName(), request.getAddress());
    return new ResponseEntity<>(new CreateDriverResponse(driver.getId()), HttpStatus.OK);
  }

  @RequestMapping(path="/drivers/{driverId}/availability", method= RequestMethod.POST)
  public ResponseEntity<String> updateDriverLocation(@PathVariable long driverId, @RequestBody DriverAvailability availability) {
    driverService.updateAvailability(driverId, availability.isAvailable());
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @RequestMapping(path="/drivers/{driverId}", method= RequestMethod.GET)
  public ResponseEntity<Driver> get(@PathVariable long driverId) {
    Driver driver = driverService.findDriverById(driverId);
    return new ResponseEntity<>(driver, HttpStatus.OK);
  }

}
