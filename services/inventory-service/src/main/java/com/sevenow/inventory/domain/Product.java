package com.sevenow.inventory.domain;

import com.sevenow.inventory.common.Money;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.*;

@Embeddable
@Access(AccessType.FIELD)
public class Product {

  private String id;
  private String name;


  @Embedded
  @AttributeOverride(name="amount", column = @Column(name="price"))
  private Money price;

  private int onHandQuantity;

  private String substituteProductId;

  private Product() {
  }

  public Product(String id, String name, Money price, int onHandQuantity) {
    this(id, name, price, onHandQuantity, null);
  }

  public Product(String id, String name, Money price, int onHandQuantity, String substituteProductId) {
    this.id = id;
    this.name = name;
    this.price = price;
    this.onHandQuantity = onHandQuantity;
    this.substituteProductId = substituteProductId;
  }

  public boolean isOutOfStock() {
    return onHandQuantity <= 0;
  }

  public boolean hasOnHand(int quantity) {
    return onHandQuantity >= quantity;
  }

  public String getSubstituteProductId() {
    return substituteProductId;
  }

  public void setSubstituteProductId(String substituteProductId) {
    this.substituteProductId = substituteProductId;
  }

  public int getOnHandQuantity() {
    return onHandQuantity;
  }

  public void setOnHandQuantity(int onHandQuantity) {
    this.onHandQuantity = onHandQuantity;
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Money getPrice() {
    return price;
  }

  public void setPrice(Money price) {
    this.price = price;
  }
}
