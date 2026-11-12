package com.sevenow.fulfillment.security;

/**
 * Authenticated caller: a customer, an associate of one store, or a driver, identified by that entity's id; or a
 * store admin or dispatcher (operators), which have no entity id.
 */
public final class SevenowPrincipal {

  private final String subject;
  private final SevenowRole role;
  private final long id;

  public SevenowPrincipal(String subject, SevenowRole role, long id) {
    this.subject = subject;
    this.role = role;
    this.id = id;
  }

  public String getSubject() {
    return subject;
  }

  public SevenowRole getRole() {
    return role;
  }

  public long getId() {
    return id;
  }

  public boolean isCustomer(Long customerId) {
    return role == SevenowRole.CUSTOMER && customerId != null && id == customerId;
  }

  public boolean isStoreAssociateOf(Long storeId) {
    return role == SevenowRole.STORE_ASSOCIATE && storeId != null && id == storeId;
  }

  public boolean isDriver(Long driverId) {
    return role == SevenowRole.DRIVER && driverId != null && id == driverId;
  }

  public boolean isStoreAdmin() {
    return role == SevenowRole.STORE_ADMIN;
  }

  public boolean isDispatcher() {
    return role == SevenowRole.DISPATCHER;
  }

  @Override
  public String toString() {
    return role + ":" + id;
  }
}
