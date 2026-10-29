package com.sevenow.inventory.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReservationService {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final StoreRepository storeRepository;
  private final ReservationRepository reservationRepository;
  private final SubstitutionRule substitutionRule = new SubstitutionRule();

  public ReservationService(StoreRepository storeRepository, ReservationRepository reservationRepository) {
    this.storeRepository = storeRepository;
    this.reservationRepository = reservationRepository;
  }

  /** Applies the substitution rule without touching stock. */
  @Transactional(readOnly = true)
  public List<ReservationItem> resolve(long storeId, List<RequestedItem> items) {
    return substitutionRule.resolve(findStore(storeId), items);
  }

  /**
   * Resolves the items and takes them out of stock in one transaction. Each decrement is a
   * conditional UPDATE, so if a concurrent reservation took the stock first this one fails with
   * {@link ProductOutOfStockException} and every decrement already made here is rolled back.
   */
  public Reservation reserve(long storeId, List<RequestedItem> items) {
    Store store = findStore(storeId);
    List<ReservationItem> resolved = substitutionRule.resolve(store, items);
    for (ReservationItem item : resolved) {
      if (storeRepository.decrementOnHand(storeId, item.getProductId(), item.getQuantity()) == 0) {
        String requested = item.isSubstituted() ? item.getSubstitutedFromProductId() : item.getProductId();
        throw new ProductOutOfStockException(Collections.singletonList(requested));
      }
      if (item.isSubstituted()) {
        logger.info("Substituting out-of-stock product {} with {} at store {}",
                item.getSubstitutedFromProductId(), item.getProductId(), store.getStoreNumber());
      }
    }
    return reservationRepository.save(new Reservation(storeId, resolved));
  }

  public Reservation release(long reservationId) {
    Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    if (reservation.release()) {
      for (ReservationItem item : reservation.getItems()) {
        storeRepository.incrementOnHand(reservation.getStoreId(), item.getProductId(), item.getQuantity());
      }
    }
    return reservation;
  }

  @Transactional(readOnly = true)
  public Optional<Reservation> findById(long reservationId) {
    return reservationRepository.findById(reservationId);
  }

  private Store findStore(long storeId) {
    return storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));
  }
}
