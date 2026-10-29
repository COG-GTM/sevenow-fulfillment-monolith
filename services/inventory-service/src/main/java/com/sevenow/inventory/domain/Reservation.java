package com.sevenow.inventory.domain;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservations")
@Access(AccessType.FIELD)
public class Reservation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version
  private Long version;

  private long storeId;

  @Enumerated(EnumType.STRING)
  private ReservationStatus status;

  private LocalDateTime createdTime;
  private LocalDateTime releasedTime;

  @ElementCollection
  @CollectionTable(name = "reservation_items", joinColumns = @JoinColumn(name = "reservation_id"))
  private List<ReservationItem> items = new ArrayList<>();

  private Reservation() {
  }

  public Reservation(long storeId, List<ReservationItem> items) {
    this.storeId = storeId;
    this.items = new ArrayList<>(items);
    this.status = ReservationStatus.RESERVED;
    this.createdTime = LocalDateTime.now();
  }

  /** @return false if the reservation had already been released (release is idempotent). */
  public boolean release() {
    if (status == ReservationStatus.RELEASED) {
      return false;
    }
    status = ReservationStatus.RELEASED;
    releasedTime = LocalDateTime.now();
    return true;
  }

  public Long getId() {
    return id;
  }

  public long getStoreId() {
    return storeId;
  }

  public ReservationStatus getStatus() {
    return status;
  }

  public LocalDateTime getCreatedTime() {
    return createdTime;
  }

  public LocalDateTime getReleasedTime() {
    return releasedTime;
  }

  public List<ReservationItem> getItems() {
    return items;
  }
}
