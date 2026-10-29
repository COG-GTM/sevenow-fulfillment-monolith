package com.sevenow.delivery.domain;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Embeddable
public class Plan {

  @ElementCollection
  @CollectionTable(name = "driver_actions", joinColumns = @JoinColumn(name = "driver_id"))
  private List<Action> actions = new LinkedList<>();

  public void add(Action action) {
    actions.add(action);
  }

  public void removeDelivery(long orderId) {
    actions = actions.stream().filter(action -> !action.actionFor(orderId)).collect(Collectors.toList());
  }

  public List<Action> getActions() {
    return actions;
  }

  public List<Action> actionsForDelivery(long orderId) {
    return actions.stream().filter(action -> action.actionFor(orderId)).collect(Collectors.toList());
  }
}
