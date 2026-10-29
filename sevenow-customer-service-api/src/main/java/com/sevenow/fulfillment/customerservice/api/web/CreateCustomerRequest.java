package com.sevenow.fulfillment.customerservice.api.web;

import com.sevenow.fulfillment.common.PersonName;

public class CreateCustomerRequest {
  private PersonName name;

  public PersonName getName() {
    return name;
  }

  public void setName(PersonName name) {
    this.name = name;
  }

  public CreateCustomerRequest(PersonName name) {

    this.name = name;
  }

  private CreateCustomerRequest() {
  }


}
