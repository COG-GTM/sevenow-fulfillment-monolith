package com.sevenow.fulfillment.orderservice;

import com.sevenow.fulfillment.common.Money;
import com.sevenow.fulfillment.domain.*;
import com.sevenow.fulfillment.orderservice.web.ProductIdAndQuantity;

import java.util.Collections;
import java.util.List;

import static com.sevenow.fulfillment.orderservice.StoreMother.*;

public class OrderDetailsMother {

  public static long CUSTOMER_ID = 1511300065921L;

  public static final int SLURPEE_QUANTITY = 5;
  public static final ProductIdAndQuantity SLURPEE_PRODUCT_AND_QUANTITY = new ProductIdAndQuantity(CHERRY_SLURPEE_ID, SLURPEE_QUANTITY);
  public static final List<ProductIdAndQuantity> SLURPEE_PRODUCTS_AND_QUANTITIES = Collections.singletonList(SLURPEE_PRODUCT_AND_QUANTITY);

  public static List<OrderLineItem> slurpeeLineItems() {
    return Collections.singletonList(new OrderLineItem(SLURPEE_PRODUCT_AND_QUANTITY.getProductId(),
            CHERRY_SLURPEE,
            CHERRY_SLURPEE_PRICE,
            SLURPEE_PRODUCT_AND_QUANTITY.getQuantity()));
  }

  public static final Money SLURPEE_ORDER_TOTAL = CHERRY_SLURPEE_PRICE.multiply(SLURPEE_QUANTITY);

  public static long ORDER_ID = 99L;

  public static Order SLURPEE_ORDER = makeSlurpeeOrder();

  public static final OrderState SLURPEE_ORDER_STATE = OrderState.APPROVED;

  private static Order makeSlurpeeOrder() {
    Order order = new Order(CUSTOMER_ID, new Store(STORE_ID, STORE_NUMBER, STORE_NAME, new StoreCatalog(Collections.emptyList())), slurpeeLineItems());
    order.setId(ORDER_ID);
    return order;
  }
}
