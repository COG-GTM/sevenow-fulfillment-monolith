package com.sevenow.fulfillment.customerservice.web;

import com.sevenow.fulfillment.common.PersonName;
import com.sevenow.fulfillment.customerservice.api.web.CreateCustomerResponse;

public class GetCustomerResponse extends CreateCustomerResponse {
  private PersonName name;

  public PersonName getName() {
    return name;
  }

  public GetCustomerResponse(PersonName name) {

    this.name = name;
  }
}
