package com.sevenow.inventory.web;

import com.sevenow.inventory.common.Address;
import com.sevenow.inventory.domain.Product;
import com.sevenow.inventory.domain.Store;

import java.util.List;

public class GetStoreResponse {

  private final Long id;
  private final String storeNumber;
  private final String name;
  private final Address address;
  private final List<Product> products;

  public GetStoreResponse(Store store) {
    this.id = store.getId();
    this.storeNumber = store.getStoreNumber();
    this.name = store.getName();
    this.address = store.getAddress();
    this.products = store.getProducts();
  }

  public Long getId() {
    return id;
  }

  public String getStoreNumber() {
    return storeNumber;
  }

  public String getName() {
    return name;
  }

  public Address getAddress() {
    return address;
  }

  public List<Product> getProducts() {
    return products;
  }
}
