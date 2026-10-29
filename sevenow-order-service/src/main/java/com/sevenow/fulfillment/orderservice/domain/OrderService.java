package com.sevenow.fulfillment.orderservice.domain;

import io.micrometer.core.instrument.MeterRegistry;
import com.sevenow.fulfillment.customerservice.domain.CustomerService;
import com.sevenow.fulfillment.domain.*;
import com.sevenow.fulfillment.orderservice.web.ProductIdAndQuantity;
import com.sevenow.fulfillment.security.SevenowCaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static java.util.stream.Collectors.toList;

@Transactional
public class OrderService {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private OrderRepository orderRepository;

  private StoreRepository storeRepository;

  private Optional<MeterRegistry> meterRegistry;

  private CustomerService customerService;
  private DriverRepository driverRepository;
  private Random random = new Random();

  public OrderService(OrderRepository orderRepository,
                      StoreRepository storeRepository,
                      Optional<MeterRegistry> meterRegistry,
                      CustomerService customerService, DriverRepository driverRepository) {

    this.orderRepository = orderRepository;
    this.storeRepository = storeRepository;
    this.meterRegistry = meterRegistry;
    this.customerService = customerService;
    this.driverRepository = driverRepository;
  }

  @Transactional
  public Order createOrder(long customerId, long storeId,
                           List<ProductIdAndQuantity> lineItems) {
    Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new StoreNotFoundException(storeId));


    List<OrderLineItem> orderLineItems = makeOrderLineItems(lineItems, store);

    Order order = new Order(customerId, store, orderLineItems);

    customerService.validateOrderForCustomer(customerId, order.getOrderTotal());

    // TODO - charge a credit card too

    orderRepository.save(order);

    meterRegistry.ifPresent(mr1 -> mr1.counter("approved_orders").increment());

    meterRegistry.ifPresent(mr -> mr.counter("placed_orders").increment());

    return order;
  }

  /**
   * Substitution rule: a product with zero onHandQuantity is replaced by its configured
   * substituteProductId (priced as the substitute); if no substitute is configured, or the
   * substitute is itself out of stock, the line item is rejected and the whole order fails.
   */
  private List<OrderLineItem> makeOrderLineItems(List<ProductIdAndQuantity> lineItems, Store store) {
    List<String> rejected = new ArrayList<>();
    List<OrderLineItem> result = lineItems.stream().map(li -> {
      Product requested = store.findProduct(li.getProductId()).orElseThrow(() -> new InvalidProductIdException(li.getProductId()));
      if (!requested.isOutOfStock()) {
        return new OrderLineItem(requested.getId(), requested.getName(), requested.getPrice(), li.getQuantity());
      }
      Optional<Product> substitute = Optional.ofNullable(requested.getSubstituteProductId())
              .flatMap(store::findProduct)
              .filter(p -> !p.isOutOfStock());
      if (!substitute.isPresent()) {
        rejected.add(requested.getId());
        return null;
      }
      Product s = substitute.get();
      logger.info("Substituting out-of-stock product {} with {} at store {}", requested.getId(), s.getId(), store.getStoreNumber());
      return new OrderLineItem(s.getId(), s.getName(), s.getPrice(), li.getQuantity(), requested.getId());
    }).collect(toList());
    if (!rejected.isEmpty()) {
      throw new ProductOutOfStockException(rejected);
    }
    return result;
  }

  @Transactional
  public Order cancel(Long orderId) {
    Order order = tryToFindOrder(orderId);
    SevenowCaller.requireCustomer(order.getCustomerId());

    order.cancel();

    return order;
  }

  @Transactional
  public Order reviseOrder(long orderId, OrderRevision orderRevision) {
    Order order = tryToFindOrder(orderId);
    SevenowCaller.requireCustomer(order.getCustomerId());
    order.revise(orderRevision);
    return order;
  }

  public void accept(long orderId, LocalDateTime readyBy) {
    Order order = tryToFindOrder(orderId);
    requireStoreAssociate(order);
    order.acceptTicket(readyBy);
    scheduleDelivery(order, readyBy);
  }

  public void scheduleDelivery(Order order, LocalDateTime readyBy) {

    // Stupid implementation

    List<Driver> drivers = driverRepository.findAllAvailable();
    Driver driver = drivers.get(random.nextInt(drivers.size()));
    driver.addAction(Action.makePickup(order));
    driver.addAction(Action.makeDropoff(order, readyBy.plusMinutes(30)));

    order.schedule(driver);

  }


  private void requireStoreAssociate(Order order) {
    SevenowCaller.requireStoreAssociate(order.getStore().getId());
  }

  private void requireAssignedDriver(Order order) {
    SevenowCaller.requireDriver(order.getAssignedDriver() == null ? null : order.getAssignedDriver().getId());
  }

  private Order tryToFindOrder(Long orderId) {
    return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
  }

  @Transactional
  public void notePicking(long orderId) {
    Order order = tryToFindOrder(orderId);
    requireStoreAssociate(order);
    order.notePicking();
  }

  @Transactional
  public void notePacked(long orderId) {
    Order order = tryToFindOrder(orderId);
    requireStoreAssociate(order);
    order.notePacked();
  }

  @Transactional
  public void noteReadyForPickup(long orderId) {
    Order order = tryToFindOrder(orderId);
    requireStoreAssociate(order);
    order.noteReadyForPickup();
  }

  @Transactional
  public void notePickedUp(long orderId) {
    Order order = tryToFindOrder(orderId);
    requireAssignedDriver(order);
    order.notePickedUp();
  }

  @Transactional
  public void noteDelivered(long orderId) {
    Order order = tryToFindOrder(orderId);
    requireAssignedDriver(order);
    order.noteDelivered();
  }
}
