package com.sevenow.fulfillment.domain;

import com.sevenow.fulfillment.common.Money;
import com.sevenow.fulfillment.common.PersonName;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;

@Entity
@Table(name = "customers")
@Access(AccessType.FIELD)
@DynamicUpdate
public class Customer {

  @Id
  @GeneratedValue
  private Long id;

  @Embedded
  private PersonName name;

  private Customer() {
  }

  public Customer(PersonName name) {
    this.name = name;
  }


  public void validateOrderByCustomer(Money orderTotal) {
    // implement some business logic
  }

  public Long getId() {
    return id;
  }

  public PersonName getName() {
    return name;
  }
}
