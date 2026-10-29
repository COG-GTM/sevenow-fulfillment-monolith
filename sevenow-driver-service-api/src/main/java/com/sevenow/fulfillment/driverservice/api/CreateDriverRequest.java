package com.sevenow.fulfillment.driverservice.api;

import com.sevenow.fulfillment.common.Address;
import com.sevenow.fulfillment.common.PersonName;

public class CreateDriverRequest {
  private PersonName name;
  private Address address;

  public CreateDriverRequest() {
  }

  public CreateDriverRequest(PersonName name, Address address) {
    this.name = name;
    this.address = address;
  }

  public PersonName getName() {
    return name;
  }

  public Address getAddress() {
    return address;
  }

  public void setName(PersonName name) {
    this.name = name;
  }

  public void setAddress(Address address) {
    this.address = address;
  }
}
