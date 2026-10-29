package com.sevenow.inventory;

import com.sevenow.inventory.common.Address;
import com.sevenow.inventory.common.Money;
import com.sevenow.inventory.domain.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

/** Runs against the real sevenow_inventory schema (Flyway-migrated on startup). */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ReservationIntegrationTest {

  @Autowired
  private StoreService storeService;

  @Autowired
  private ReservationService reservationService;

  @Autowired
  private TransactionTemplate transactionTemplate;

  private Store newStore(int onHand) {
    return storeService.create(new Store("IT-" + System.nanoTime(), "Integration store", new Address("1 Main St", null, "Irving", "TX", "75063"),
            new StoreCatalog(Arrays.asList(
                    new Product("COKE", "Coke", new Money("1.99"), 0, "PEPSI"),
                    new Product("PEPSI", "Pepsi", new Money("1.89"), onHand)))));
  }

  private int onHand(long storeId, String productId) {
    return storeService.findProduct(storeId, productId).get().getOnHandQuantity();
  }

  @Test
  public void seededStoreHas18Products() {
    assertEquals(18, (int) transactionTemplate.execute(s -> storeService.findById(1L).get().getProducts().size()));
  }

  @Test
  public void shouldDecrementOnReserveAndRestoreOnRelease() {
    long storeId = newStore(5).getId();

    Reservation r = reservationService.reserve(storeId, Collections.singletonList(new RequestedItem("COKE", 3)));

    assertEquals("PEPSI", r.getItems().get(0).getProductId());
    assertEquals("COKE", r.getItems().get(0).getSubstitutedFromProductId());
    assertEquals(2, onHand(storeId, "PEPSI"));

    reservationService.release(r.getId());
    reservationService.release(r.getId());
    assertEquals(5, onHand(storeId, "PEPSI"));
    assertEquals(ReservationStatus.RELEASED, reservationService.findById(r.getId()).get().getStatus());
  }

  @Test
  public void concurrentReservationsNeverOversell() throws Exception {
    long storeId = newStore(5).getId();
    ExecutorService executor = Executors.newFixedThreadPool(10);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Boolean>> results = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      results.add(executor.submit(() -> {
        start.await();
        try {
          reservationService.reserve(storeId, Collections.singletonList(new RequestedItem("PEPSI", 1)));
          return true;
        } catch (ProductOutOfStockException e) {
          return false;
        }
      }));
    }
    start.countDown();
    int succeeded = 0;
    for (Future<Boolean> f : results) {
      if (f.get(30, TimeUnit.SECONDS)) succeeded++;
    }
    executor.shutdown();

    assertEquals(5, succeeded);
    assertEquals(0, onHand(storeId, "PEPSI"));
  }
}
