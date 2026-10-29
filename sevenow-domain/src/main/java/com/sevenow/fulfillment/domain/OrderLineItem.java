package com.sevenow.fulfillment.domain;

import com.sevenow.fulfillment.common.Money;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.*;

@Embeddable
public class OrderLineItem {

  public OrderLineItem() {
  }

  private int quantity;
  private String productId;
  private String name;
  private String substitutedFromProductId;

  @Embedded
  @AttributeOverrides(@AttributeOverride(name="amount", column=@Column(name="price")))
  private Money price;

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  public OrderLineItem(String productId, String name, Money price, int quantity) {
    this(productId, name, price, quantity, null);
  }

  public OrderLineItem(String productId, String name, Money price, int quantity, String substitutedFromProductId) {
    this.productId = productId;
    this.name = name;
    this.price = price;
    this.quantity = quantity;
    this.substitutedFromProductId = substitutedFromProductId;
  }

  public String getSubstitutedFromProductId() {
    return substitutedFromProductId;
  }

  public boolean isSubstituted() {
    return substitutedFromProductId != null;
  }

  public Money deltaForChangedQuantity(int newQuantity) {
    return price.multiply(newQuantity - quantity);
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPrice(Money price) {
    this.price = price;
  }

  public int getQuantity() {
    return quantity;
  }

  public String getProductId() {
    return productId;
  }

  public String getName() {
    return name;
  }

  public Money getPrice() {
    return price;
  }


  public Money getTotal() {
    return price.multiply(quantity);
  }

}
