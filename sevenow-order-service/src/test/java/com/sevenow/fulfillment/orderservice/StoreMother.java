package com.sevenow.fulfillment.orderservice;

import com.sevenow.fulfillment.common.Address;
import com.sevenow.fulfillment.common.Money;
import com.sevenow.fulfillment.domain.Product;
import com.sevenow.fulfillment.domain.Store;
import com.sevenow.fulfillment.domain.StoreCatalog;

import java.util.Arrays;
import java.util.List;

public class StoreMother {
  public static final long STORE_ID = 1L;
  public static final String STORE_NUMBER = "38114";
  public static final String STORE_NAME = "7-Eleven #38114";
  public static final Address STORE_ADDRESS = new Address("3200 Hackberry Rd", null, "Irving", "TX", "75063");

  public static final String CHERRY_SLURPEE_ID = "SLURPEE-CHERRY-M";
  public static final String CHERRY_SLURPEE = "Cherry Slurpee (Medium)";
  public static final Money CHERRY_SLURPEE_PRICE = new Money("2.19");

  public static final String PEPPERONI_PIZZA_ID = "PIZZA-PEPPERONI-WHOLE";
  public static final String PEPPERONI_PIZZA = "Pepperoni Pizza (Whole)";
  public static final Money PEPPERONI_PIZZA_PRICE = new Money("7.99");

  public static final String CHEESE_PIZZA_ID = "PIZZA-CHEESE-WHOLE";
  public static final String CHEESE_PIZZA = "Cheese Pizza (Whole)";
  public static final Money CHEESE_PIZZA_PRICE = new Money("6.99");

  public static final String CHICKEN_WINGS_ID = "WINGS-8PC";
  public static final String CHICKEN_WINGS = "Chicken Wings (8 pc)";
  public static final Money CHICKEN_WINGS_PRICE = new Money("6.49");

  public static final Product CHERRY_SLURPEE_PRODUCT = new Product(CHERRY_SLURPEE_ID, CHERRY_SLURPEE, CHERRY_SLURPEE_PRICE, 40);
  public static final Product PEPPERONI_PIZZA_OUT_OF_STOCK = new Product(PEPPERONI_PIZZA_ID, PEPPERONI_PIZZA, PEPPERONI_PIZZA_PRICE, 0, CHEESE_PIZZA_ID);
  public static final Product CHEESE_PIZZA_PRODUCT = new Product(CHEESE_PIZZA_ID, CHEESE_PIZZA, CHEESE_PIZZA_PRICE, 6);
  public static final Product CHICKEN_WINGS_OUT_OF_STOCK = new Product(CHICKEN_WINGS_ID, CHICKEN_WINGS, CHICKEN_WINGS_PRICE, 0);

  public static final List<Product> STORE_PRODUCTS = Arrays.asList(
          CHERRY_SLURPEE_PRODUCT, PEPPERONI_PIZZA_OUT_OF_STOCK, CHEESE_PIZZA_PRODUCT, CHICKEN_WINGS_OUT_OF_STOCK);

  public static final Store SEVEN_ELEVEN_STORE = new Store(STORE_ID, STORE_NUMBER, STORE_NAME, new StoreCatalog(STORE_PRODUCTS));
}
