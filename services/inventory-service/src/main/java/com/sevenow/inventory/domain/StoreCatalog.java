package com.sevenow.inventory.domain;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.List;

public class StoreCatalog {
  private List<Product> products;

  private StoreCatalog() {
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  public List<Product> getProducts() {
    return products;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public void setProducts(List<Product> products) {
    this.products = products;
  }

  public StoreCatalog(List<Product> products) {

    this.products = products;
  }

}
