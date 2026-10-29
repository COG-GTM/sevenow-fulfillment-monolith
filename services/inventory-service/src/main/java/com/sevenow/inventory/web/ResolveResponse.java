package com.sevenow.inventory.web;

import com.sevenow.inventory.domain.ReservationItem;

import java.util.List;

public class ResolveResponse {

  private final long storeId;
  private final List<ReservationItem> items;

  public ResolveResponse(long storeId, List<ReservationItem> items) {
    this.storeId = storeId;
    this.items = items;
  }

  public long getStoreId() {
    return storeId;
  }

  public List<ReservationItem> getItems() {
    return items;
  }
}
