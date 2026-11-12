package com.sevenow.fulfillment.domain;

import com.sevenow.fulfillment.common.Address;
import com.sevenow.fulfillment.common.PersonName;
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
  private Plan plan;

  private Boolean available = false;

  private Boolean approved = false;

  public Driver() {
  }

  public Driver(PersonName name, Address address) {
    this.name = name;
    this.address = address;
  }

  public void noteAvailable() {
    this.available = true;

  }

  public void addAction(Action action) {
    plan.add(action);
  }

  public void cancelDelivery(Order order) {
    plan.removeDelivery(order);
  }

  public boolean isAvailable() {
    return Boolean.TRUE.equals(available);
  }

  public boolean isApproved() {
    return Boolean.TRUE.equals(approved);
  }

  public void noteApproved() {
    this.approved = true;
  }

  public Plan getPlan() {
    return plan;
  }

  public Long getId() {
    return id;
  }

  public void noteUnavailable() {
    this.available = false;
  }

  public List<Action> actionsForDelivery(Order order) {
    return plan.actionsForDelivery(order);
  }
}
