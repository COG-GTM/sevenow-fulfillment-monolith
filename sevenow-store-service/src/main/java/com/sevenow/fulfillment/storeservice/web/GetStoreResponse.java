package com.sevenow.fulfillment.storeservice.web;

import com.sevenow.fulfillment.common.Address;
import com.sevenow.fulfillment.domain.Product;

import java.util.List;

public class GetStoreResponse {
  private Long id;
  private String storeNumber;
  private String name;
  private Address address;
  private List<Product> products;

  public GetStoreResponse() {
  }

  public GetStoreResponse(Long id, String storeNumber, String name, Address address, List<Product> products) {
    this.id = id;
    this.storeNumber = storeNumber;
    this.name = name;
    this.address = address;
    this.products = products;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public List<Product> getProducts() {
    return products;
  }

  public void setProducts(List<Product> products) {
    this.products = products;
  }
}
