package com.sevenow.fulfillment.storeservice.events;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.List;

public class StoreCatalogDTO {
  private List<ProductDTO> products;

  private StoreCatalogDTO() {
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  public List<ProductDTO> getProductDTOs() {
    return products;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public void setProductDTOs(List<ProductDTO> products) {
    this.products = products;
  }

  public StoreCatalogDTO(List<ProductDTO> products) {

    this.products = products;
  }

}