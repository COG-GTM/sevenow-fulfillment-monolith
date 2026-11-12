package com.sevenow.fulfillment.storeservice.events;


import com.sevenow.fulfillment.common.Address;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CreateStoreRequest {

  @Size(max = 32)
  private String storeNumber;
  @Size(max = 255)
  private String name;
  @NotNull
  @Valid
  private StoreCatalogDTO catalog;
  private Address address;

  public CreateStoreRequest(String storeNumber, String name, Address address, StoreCatalogDTO catalog) {
    this.storeNumber = storeNumber;
    this.name = name;
    this.address = address;
    this.catalog = catalog;
  }

  public String getStoreNumber() {
    return storeNumber;
  }

  public void setStoreNumber(String storeNumber) {
    this.storeNumber = storeNumber;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public StoreCatalogDTO getCatalog() {
    return catalog;
  }

  public void setCatalog(StoreCatalogDTO catalog) {
    this.catalog = catalog;
  }

  private CreateStoreRequest() {

  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public Address getAddress() {
    return address;
  }
}
