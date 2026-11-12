package com.sevenow.fulfillment.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Authorization checks against the authenticated caller of the current request.
 */
public final class SevenowCaller {

  private SevenowCaller() {
  }

  public static SevenowPrincipal current() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof SevenowPrincipal)) {
      throw new AuthenticationCredentialsNotFoundException("Authentication required");
    }
    return (SevenowPrincipal) authentication.getPrincipal();
  }

  public static void requireCustomer(Long customerId) {
    if (!current().isCustomer(customerId)) {
      throw new AccessDeniedException("Caller is not the customer who placed this order");
    }
  }

  public static void requireStoreAssociate(Long storeId) {
    if (!current().isStoreAssociateOf(storeId)) {
      throw new AccessDeniedException("Caller is not an associate of the store fulfilling this order");
    }
  }

  public static SevenowPrincipal requireRole(SevenowRole role) {
    SevenowPrincipal caller = current();
    if (caller.getRole() != role) {
      throw new AccessDeniedException("Caller does not have role " + role);
    }
    return caller;
  }

  public static void requireStoreAdmin() {
    requireRole(SevenowRole.STORE_ADMIN);
  }

  public static void requireDispatcher() {
    requireRole(SevenowRole.DISPATCHER);
  }

  public static void requireDriverOrDispatcher(Long driverId) {
    SevenowPrincipal caller = current();
    if (!caller.isDispatcher() && !caller.isDriver(driverId)) {
      throw new AccessDeniedException("Caller is neither this driver nor a dispatcher");
    }
  }

  public static void requireDriver(Long driverId) {
    if (!current().isDriver(driverId)) {
      throw new AccessDeniedException("Caller is not the driver assigned to this order");
    }
  }
}
