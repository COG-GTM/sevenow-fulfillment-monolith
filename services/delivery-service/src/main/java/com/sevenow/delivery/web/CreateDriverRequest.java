package com.sevenow.delivery.web;

import com.sevenow.delivery.common.Address;
import com.sevenow.delivery.common.PersonName;

public class CreateDriverRequest {

  private PersonName name;
  private Address address;

  public PersonName getName() {
    return name;
  }

  public void setName(PersonName name) {
    this.name = name;
  }

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }
}
