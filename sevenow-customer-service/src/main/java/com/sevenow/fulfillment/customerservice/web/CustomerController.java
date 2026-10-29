package com.sevenow.fulfillment.customerservice.web;

import com.sevenow.fulfillment.customerservice.api.web.CreateCustomerRequest;
import com.sevenow.fulfillment.customerservice.api.web.CreateCustomerResponse;
import com.sevenow.fulfillment.customerservice.domain.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path="/customers")
public class CustomerController {

  @Autowired
  private CustomerService customerService;

  @RequestMapping(method= RequestMethod.POST)
  public CreateCustomerResponse create(@RequestBody CreateCustomerRequest request) {
    return new CreateCustomerResponse(customerService.create(request.getName()).getId());
  }

  @RequestMapping(method= RequestMethod.GET,  path="/{customerId}")
  public ResponseEntity<GetCustomerResponse> get(@PathVariable long customerId) {
    return customerService.findById(customerId)
            .map(customer -> new ResponseEntity<>(new GetCustomerResponse(customer.getName()), HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }
}
