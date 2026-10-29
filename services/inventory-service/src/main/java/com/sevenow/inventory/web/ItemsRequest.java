package com.sevenow.inventory.web;

import com.sevenow.inventory.domain.RequestedItem;

import java.util.List;

/** Body of {@code POST /reservations} and {@code POST /reservations/resolve}. */
public class ItemsRequest {

  private long storeId;
  private List<RequestedItem> items;

  public ItemsRequest() {
  }

  public ItemsRequest(long storeId, List<RequestedItem> items) {
    this.storeId = storeId;
    this.items = items;
  }

  public long getStoreId() {
    return storeId;
  }

  public void setStoreId(long storeId) {
    this.storeId = storeId;
  }

  public List<RequestedItem> getItems() {
    return items;
  }

  public void setItems(List<RequestedItem> items) {
    this.items = items;
  }
}
