package com.sevenow.delivery.domain;

import com.sevenow.delivery.common.Address;
import com.sevenow.delivery.common.PersonName;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "drivers")
@Access(AccessType.FIELD)
@DynamicUpdate
public class Driver {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Embedded
  private PersonName name;

  @Embedded
  private Address address;

  @Embedded
  private Plan plan = new Plan();

  private Boolean available;

  public Driver() {
  }

  public Driver(PersonName name, Address address) {
    this.name = name;
    this.address = address;
  }

  public void noteAvailable() {
    this.available = true;
  }

  public void noteUnavailable() {
    this.available = false;
  }

  public void addAction(Action action) {
    if (plan == null) {
      plan = new Plan();
    }
    plan.add(action);
  }

  public void cancelDelivery(long orderId) {
    plan.removeDelivery(orderId);
  }

  public boolean isAvailable() {
    return Boolean.TRUE.equals(available);
  }

  public Plan getPlan() {
    return plan;
  }

  public Long getId() {
    return id;
  }

  public PersonName getName() {
    return name;
  }

  public Address getAddress() {
    return address;
  }

  public List<Action> actionsForDelivery(long orderId) {
    return plan.actionsForDelivery(orderId);
  }
}
