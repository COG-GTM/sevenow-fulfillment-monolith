package com.sevenow.fulfillment.storeservice.events;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

public class StoreCatalogDTO {

  public static final int MAX_PRODUCTS = 500;

  @NotNull
  @Size(max = MAX_PRODUCTS)
  @Valid
  private List<@NotNull ProductDTO> products;

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

  public List<ProductDTO> getProducts() {
    return products;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public void setProducts(List<ProductDTO> products) {
    this.products = products;
  }

  public StoreCatalogDTO(List<ProductDTO> products) {

    this.products = products;
  }

}