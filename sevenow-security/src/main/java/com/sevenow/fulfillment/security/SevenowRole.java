package com.sevenow.fulfillment.security;

public enum SevenowRole {
  CUSTOMER("customerId"),
  STORE_ASSOCIATE("storeId"),
  DRIVER("driverId"),
  STORE_ADMIN(null),
  DISPATCHER(null);

  private final String idClaim;

  SevenowRole(String idClaim) {
    this.idClaim = idClaim;
  }

  /**
   * Name of the JWT claim that holds the customer, store or driver id this role acts for, or null for
   * {@link #STORE_ADMIN} and {@link #DISPATCHER}, operator roles that do not act for a single entity.
   */
  public String getIdClaim() {
    return idClaim;
  }

  public String authority() {
    return "ROLE_" + name();
  }
}
