package com.sevenow.fulfillment.endtoendtests.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.ObjectMapperConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import io.eventuate.util.test.async.Eventually;
import com.sevenow.fulfillment.common.Address;
import com.sevenow.fulfillment.common.Money;
import com.sevenow.fulfillment.common.MoneyModule;
import com.sevenow.fulfillment.common.PersonName;
import com.sevenow.fulfillment.customerservice.api.web.CreateCustomerRequest;
import com.sevenow.fulfillment.driverservice.api.DriverAvailability;
import com.sevenow.fulfillment.driverservice.api.CreateDriverRequest;
import com.sevenow.fulfillment.orderservice.api.web.CreateOrderRequest;
import com.sevenow.fulfillment.orderservice.api.web.OrderAcceptance;
import com.sevenow.fulfillment.orderservice.api.web.ReviseOrderRequest;
import com.sevenow.fulfillment.storeservice.events.CreateStoreRequest;
import com.sevenow.fulfillment.storeservice.events.ProductDTO;
import com.sevenow.fulfillment.storeservice.events.StoreCatalogDTO;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static com.jayway.restassured.RestAssured.given;
import static com.sevenow.fulfillment.endtoendtests.common.TestTokens.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class AbstractEndToEndTests {

  public static final String CHERRY_SLURPEE_ID = "SLURPEE-CHERRY-M";
  public static final String PEPPERONI_PIZZA_ID = "PIZZA-PEPPERONI-WHOLE";
  public static final String CHEESE_PIZZA_ID = "PIZZA-CHEESE-WHOLE";
  public static final String CHICKEN_WINGS_ID = "WINGS-8PC";
  public static final String STORE_NUMBER = "38114";
  public static final String STORE_NAME = "7-Eleven #38114";
  private static final Address STORE_ADDRESS = new Address("3200 Hackberry Rd", null, "Irving", "TX", "75063");

  private final int revisedQuantityOfSlurpees = 10;
  private int customerId;
  private int storeId;
  private int orderId;
  private final Money priceOfSlurpee = new Money("2.19");
  private final Money priceOfPepperoniPizza = new Money("7.99");
  private final Money priceOfCheesePizza = new Money("6.99");
  private final Money priceOfChickenWings = new Money("6.49");
  private static ObjectMapper objectMapper = new ObjectMapper();
  private int driverId;
  private int assignedDriverId;

  private String baseUrl(int port, String path, String... pathElements) {
    assertNotNull("host", getHost());
    StringBuilder sb = new StringBuilder("http://");
    sb.append(getHost());
    sb.append(":");
    sb.append(port);
    sb.append("/");
    sb.append(path);

    for (String pe : pathElements) {
      sb.append("/");
      sb.append(pe);
    }
    String s = sb.toString();
    System.out.println("url=" + s);
    return s;
  }

  private String customerBaseUrl(String... pathElements) {
    return baseUrl(getApplicationPort(), "customers", pathElements);
  }

  private String storeBaseUrl(String... pathElements) {
    return baseUrl(getApplicationPort(), "stores", pathElements);
  }

  private String orderBaseUrl(String... pathElements) {
    return baseUrl(getApplicationPort(), "orders", pathElements);
  }

  @BeforeClass
  public static void initialize() {
    objectMapper.registerModule(new MoneyModule());
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    RestAssured.config = RestAssuredConfig.config().objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory(
            (aClass, s) -> objectMapper
    ));

  }

  @Test
  public void shouldCreateReviseAndCancelOrder() {

    createOrder();

    reviseOrder();

    cancelOrder();

  }

  @Test
  public void shouldDeliverOrder() {

    createOrder();

    createDriver();

    noteDriverAvailable();

    acceptOrder();

    assertOrderAssignedToDriver();

    startPickingOrder();

    verifyOrderState(orderId, "PICKING");

    orderPacked();

    verifyOrderState(orderId, "PACKED");

    orderReadyforPickup();

    verifyOrderState(orderId, "READY_FOR_PICKUP");

    assertLifecycleCallRejected("pickedup", TestTokens.driver(assignedDriverId + 1_000_000), 403);
    assertLifecycleCallRejected("pickedup", TestTokens.customer(customerId), 403);

    verifyOrderState(orderId, "READY_FOR_PICKUP");

    pickupOrder();

    assertLifecycleCallRejected("delivered", TestTokens.driver(assignedDriverId + 1_000_000), 403);

    verifyOrderState(orderId, "PICKED_UP");


    deliverOrder();

    verifyOrderState(orderId, "DELIVERED");

  }

  @Test
  public void shouldRejectUnauthenticatedAndUnauthorizedLifecycleCalls() {

    createOrder();

    for (String step : new String[]{"cancel", "revise", "accept", "picking", "packed", "ready", "pickedup", "delivered"}) {
      assertLifecycleCallRejected(step, null, 401);
      assertLifecycleCallRejected(step, TestTokens.sign("not-the-7now-signing-secret-0123456789", "CUSTOMER", "customerId", customerId), 401);
    }

    assertLifecycleCallRejected("cancel", TestTokens.customer(customerId + 1_000_000), 403);
    assertLifecycleCallRejected("revise", TestTokens.customer(customerId + 1_000_000), 403);
    assertLifecycleCallRejected("cancel", TestTokens.storeAssociate(storeId), 403);
    assertLifecycleCallRejected("accept", TestTokens.customer(customerId), 403);
    assertLifecycleCallRejected("accept", TestTokens.storeAssociate(storeId + 1_000_000), 403);
    assertLifecycleCallRejected("accept", TestTokens.driver(1), 403);

    verifyOrderState(orderId, "APPROVED");

    cancelOrder();
  }

  private void assertLifecycleCallRejected(String step, String token, int expectedStatus) {
    String body;
    switch (step) {
      case "accept":
        body = "{\"readyBy\":\"" + LocalDateTime.now().plusHours(9) + "\"}";
        break;
      case "revise":
        body = "{\"revisedLineItemQuantities\":{\"" + CHERRY_SLURPEE_ID + "\":1}}";
        break;
      default:
        body = "{}";
    }
    com.jayway.restassured.specification.RequestSpecification request = given().body(body).contentType("application/json");
    if (token != null) {
      request = request.header("Authorization", bearer(token));
    }
    request.
            when().
            post(orderBaseUrl(Long.toString(orderId), step)).
            then().
            statusCode(expectedStatus);
  }

  @Test
  public void shouldSubstituteOutOfStockProduct() {
    customerId = createCustomer();
    storeId = createStore();
    verifyStoreCreated(storeId);

    Integer substitutedOrderId = given().
            body(new CreateOrderRequest(customerId, storeId, Collections.singletonList(new CreateOrderRequest.LineItem(PEPPERONI_PIZZA_ID, 2)))).
            contentType("application/json").
            when().
            post(orderBaseUrl()).
            then().
            statusCode(200).
            extract().
            path("orderId");
    assertNotNull(substitutedOrderId);

    given().
            when().
            get(orderBaseUrl(Integer.toString(substitutedOrderId))).
            then().
            statusCode(200).
            body("state", equalTo("APPROVED")).
            body("lineItems[0].productId", equalTo(CHEESE_PIZZA_ID)).
            body("lineItems[0].substitutedFromProductId", equalTo(PEPPERONI_PIZZA_ID)).
            body("lineItems[0].quantity", equalTo(2)).
            body("orderTotal", equalTo(priceOfCheesePizza.multiply(2).asString()));
  }

  @Test
  public void shouldRejectOutOfStockProductWithoutSubstitute() {
    customerId = createCustomer();
    storeId = createStore();
    verifyStoreCreated(storeId);

    given().
            body(new CreateOrderRequest(customerId, storeId, Arrays.asList(
                    new CreateOrderRequest.LineItem(CHERRY_SLURPEE_ID, 1),
                    new CreateOrderRequest.LineItem(CHICKEN_WINGS_ID, 1)))).
            contentType("application/json").
            when().
            post(orderBaseUrl()).
            then().
            statusCode(400).
            body("rejectedProductIds", equalTo(Collections.singletonList(CHICKEN_WINGS_ID)));
  }

  private void verifyOrderState(int orderId, String expectedState) {
    Eventually.eventually(String.format("verifyOrderState %s %s", orderId, expectedState), () -> {
      String state = given().
              when().
              get(orderBaseUrl(Integer.toString(orderId))).
              then().
              statusCode(200)
              .extract().
                      path("state");
      assertEquals(expectedState, state);
    });
  }

  private void reviseOrder() {
    reviseOrder(orderId);
    verifyOrderRevised(orderId);
  }

  private void verifyOrderRevised(int orderId) {
    Eventually.eventually(String.format("verifyOrderRevised state %s", orderId), () -> {
      String orderTotal = given().
              when().
              get(baseUrl(getApplicationPort(), "orders", Integer.toString(orderId))).
              then().
              statusCode(200)
              .extract().
                      path("orderTotal");
      assertEquals(priceOfSlurpee.multiply(revisedQuantityOfSlurpees).asString(), orderTotal);
    });
    Eventually.eventually(String.format("verifyOrderRevised state %s", orderId), () -> {
      String state = given().
              when().
              get(orderBaseUrl(Integer.toString(orderId))).
              then().
              statusCode(200)
              .extract().
                      path("state");
      assertEquals("APPROVED", state);
    });
  }

  private void reviseOrder(int orderId) {
    given().
            header("Authorization", bearer(TestTokens.customer(customerId))).
            body(new ReviseOrderRequest(Collections.singletonMap(CHERRY_SLURPEE_ID, revisedQuantityOfSlurpees)))
            .contentType("application/json").
            when().
            post(orderBaseUrl(Integer.toString(orderId), "revise")).
            then().
            statusCode(200);
  }


  private void createOrder() {
    customerId = createCustomer();

    storeId = createStore();

    verifyStoreCreated(storeId);

    orderId = createOrder(customerId, storeId);

    verifyOrderAuthorized(orderId);

    verifyOrderHistoryUpdated(orderId, customerId);
  }

  private void cancelOrder() {
    cancelOrder(orderId);

    verifyOrderCancelled(orderId);
  }

  private void verifyOrderCancelled(int orderId) {
    Eventually.eventually(String.format("verifyOrderCancelled %s", orderId), () -> {
      String state = given().
              when().
              get(orderBaseUrl(Integer.toString(orderId))).
              then().
              statusCode(200)
              .extract().
                      path("state");
      assertEquals("CANCELLED", state);
    });

  }

  private void cancelOrder(int orderId) {
    given().
            header("Authorization", bearer(TestTokens.customer(customerId))).
            body("{}").
            contentType("application/json").
            when().
            post(orderBaseUrl(Integer.toString(orderId), "cancel")).
            then().
            statusCode(200);

  }

  private Integer createCustomer() {
    Integer customerId =
            given().
                    body(new CreateCustomerRequest(new PersonName("John", "Doe"))).
                    contentType("application/json").
                    when().
                    post(customerBaseUrl()).
                    then().
                    statusCode(200).
                    extract().
                    path("customerId");

    assertNotNull(customerId);
    return customerId;
  }

  private int createStore() {
    Integer storeId =
            given().
                    body(new CreateStoreRequest(STORE_NUMBER, STORE_NAME,
                            STORE_ADDRESS,
                            new StoreCatalogDTO(Arrays.asList(
                                    new ProductDTO(CHERRY_SLURPEE_ID, "Cherry Slurpee (Medium)", priceOfSlurpee, 40),
                                    new ProductDTO(PEPPERONI_PIZZA_ID, "Pepperoni Pizza (Whole)", priceOfPepperoniPizza, 0, CHEESE_PIZZA_ID),
                                    new ProductDTO(CHEESE_PIZZA_ID, "Cheese Pizza (Whole)", priceOfCheesePizza, 6),
                                    new ProductDTO(CHICKEN_WINGS_ID, "Chicken Wings (8 pc)", priceOfChickenWings, 0))))).

                    contentType("application/json").
                    when().
                    post(storeBaseUrl()).
                    then().
                    statusCode(200).
                    extract().
                    path("id");

    assertNotNull(storeId);
    return storeId;
  }

  private void verifyStoreCreated(int storeId) {
    Eventually.eventually(String.format("verifyStoreCreated %s", storeId), () ->
            given().
                    when().
                    get(storeBaseUrl(Integer.toString(storeId))).
                    then().
                    statusCode(200).
                    body("storeNumber", equalTo(STORE_NUMBER)).
                    body("products.size()", equalTo(4)));
  }

  private int createOrder(int customerId, int storeId) {
    Integer orderId =
            given().
                    body(new CreateOrderRequest(customerId, storeId, Collections.singletonList(new CreateOrderRequest.LineItem(CHERRY_SLURPEE_ID, 5)))).
                    contentType("application/json").
                    when().
                    post(orderBaseUrl()).
                    then().
                    statusCode(200).
                    extract().
                    path("orderId");

    assertNotNull(orderId);
    return orderId;
  }

  private void verifyOrderAuthorized(int orderId) {
    Eventually.eventually(String.format("verifyOrderApproved %s", orderId), () -> {
      String state = given().
              when().
              get(orderBaseUrl(Integer.toString(orderId))).
              then().
              statusCode(200)
              .extract().
                      path("state");
      assertEquals("APPROVED", state);
    });
  }

  private void verifyOrderHistoryUpdated(int orderId, int customerId) {
    Eventually.eventually(String.format("verifyOrderHistoryUpdated %s", orderId), () -> {
      String state = given().
              when().
              get(orderBaseUrl() + "?customerId=" + customerId).
              then().
              statusCode(200)
              .body("[0].storeName", equalTo(STORE_NAME))
              .extract().
                      path("[0].state");
      assertNotNull(state);
    });
  }

  private void createDriver() {
    driverId = given().
            body(new CreateDriverRequest(new PersonName("Dana", "Driver"), new Address("5601 N MacArthur Blvd", null, "Irving", "TX", "75038"))).
            contentType("application/json").
            when().
            post(baseUrl(getApplicationPort(), "drivers")).
            then().
            statusCode(200)
            .extract()
            .path("id");
  }

  private void noteDriverAvailable() {
    given().
            body(new DriverAvailability(true)).
            contentType("application/json").
            when().
            post(baseUrl(getApplicationPort(), "drivers", Long.toString(driverId), "availability")).
            then().
            statusCode(200);
  }

  private void acceptOrder() {
    given().
            header("Authorization", bearer(TestTokens.storeAssociate(storeId))).
            body(new OrderAcceptance(LocalDateTime.now().plusHours(9))).
            contentType("application/json").
            when().
            post(orderBaseUrl(Long.toString(orderId), "accept")).
            then().
            statusCode(200);
  }

  private void assertOrderAssignedToDriver() {
    int driverId = Eventually.eventuallyReturning(() -> {
      int assignedDriver = given().
              when().
              get(orderBaseUrl(Long.toString(orderId))).
              then().
              statusCode(200)
              .body("driverActions[0].type", equalTo("PICKUP"))
              .body("driverActions[1].type", equalTo("DROPOFF"))
              .extract()
              .path("assignedDriver");
      assertThat(assignedDriver).isGreaterThan(0);
      return assignedDriver;
    });
    assignedDriverId = driverId;

    given().
            when().
            get(baseUrl(getApplicationPort(), "drivers", Long.toString(driverId))).
            then().
            statusCode(200)
            .body("plan.actions[0].type", equalTo("PICKUP"))
            .body("plan.actions[1].type", equalTo("DROPOFF"));

  }

  private void startPickingOrder() {
    given().
            header("Authorization", bearer(TestTokens.storeAssociate(storeId))).
            when().
            post(orderBaseUrl(Long.toString(orderId), "picking")).
            then().
            statusCode(200);
  }

  private void orderPacked() {
    given().
            header("Authorization", bearer(TestTokens.storeAssociate(storeId))).
            when().
            post(orderBaseUrl(Long.toString(orderId), "packed")).
            then().
            statusCode(200);
  }

  private void orderReadyforPickup() {
    given().
            header("Authorization", bearer(TestTokens.storeAssociate(storeId))).
            when().
            post(orderBaseUrl(Long.toString(orderId), "ready")).
            then().
            statusCode(200);
  }

  private void pickupOrder() {
    given().
            header("Authorization", bearer(TestTokens.driver(assignedDriverId))).
            when().
            post(orderBaseUrl(Long.toString(orderId), "pickedup")).
            then().
            statusCode(200);
  }

  private void deliverOrder() {
    given().
            header("Authorization", bearer(TestTokens.driver(assignedDriverId))).
            when().
            post(orderBaseUrl(Long.toString(orderId), "delivered")).
            then().
            statusCode(200);
  }

  public abstract String getHost();

  public abstract int getApplicationPort();
}
