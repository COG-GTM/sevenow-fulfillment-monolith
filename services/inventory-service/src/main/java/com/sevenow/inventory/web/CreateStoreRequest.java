package com.sevenow.inventory.web;

import com.sevenow.inventory.common.Address;
import com.sevenow.inventory.domain.Product;

import java.util.List;

public class CreateStoreRequest {

  private String storeNumber;
  private String name;
  private Address address;
  private Catalog catalog;

  public static class Catalog {
    private List<Product> products;

    public List<Product> getProducts() {
      return products;
    }

    public void setProducts(List<Product> products) {
      this.products = products;
    }
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

  public Catalog getCatalog() {
    return catalog;
  }

  public void setCatalog(Catalog catalog) {
    this.catalog = catalog;
  }
}
