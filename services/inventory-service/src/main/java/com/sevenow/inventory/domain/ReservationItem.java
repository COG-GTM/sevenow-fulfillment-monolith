package com.sevenow.inventory.domain;

import com.sevenow.inventory.common.Money;

import javax.persistence.*;

/** A resolved line item: the product actually handed out, which may be a substitute. */
@Embeddable
@Access(AccessType.FIELD)
public class ReservationItem {

  private String productId;
  private String name;

  @Embedded
  @AttributeOverride(name = "amount", column = @Column(name = "price"))
  private Money price;

  private int quantity;

  private String substitutedFromProductId;

  private ReservationItem() {
  }

  public ReservationItem(String productId, String name, Money price, int quantity, String substitutedFromProductId) {
    this.productId = productId;
    this.name = name;
    this.price = price;
    this.quantity = quantity;
    this.substitutedFromProductId = substitutedFromProductId;
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

  public int getQuantity() {
    return quantity;
  }

  public String getSubstitutedFromProductId() {
    return substitutedFromProductId;
  }

  public boolean isSubstituted() {
    return substitutedFromProductId != null;
  }
}
