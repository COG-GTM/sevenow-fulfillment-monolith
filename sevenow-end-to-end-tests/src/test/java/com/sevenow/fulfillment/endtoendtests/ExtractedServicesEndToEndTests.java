package com.sevenow.fulfillment.endtoendtests;

import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;

/** Runs against the docker-compose deployment of inventory-service (8082) and delivery-service (8083). */
public class ExtractedServicesEndToEndTests {

  private final String host = System.getenv("DOCKER_HOST_IP");

  private String inventory(String path) {
    return "http://" + host + ":8082" + path;
  }

  private String delivery(String path) {
    return "http://" + host + ":8083" + path;
  }

  private int onHand(String productId) {
    return given().when().get(inventory("/stores/1/products/" + productId)).then().statusCode(200).extract().path("onHandQuantity");
  }

  @Test
  public void shouldServeSeededStoreCatalog() {
    given().when().get(inventory("/stores/1"))
            .then().statusCode(200)
            .body("storeNumber", equalTo("38114"))
            .body("products.size()", equalTo(18))
            .body("products.id", hasItem("RED-BULL-12OZ"));
  }

  @Test
  public void shouldReserveWithSubstitutionAndRelease() {
    int monsterBefore = onHand("MONSTER-16OZ");

    int reservationId = given().contentType("application/json")
            .body("{\"storeId\": 1, \"items\": [{\"productId\": \"RED-BULL-12OZ\", \"quantity\": 2}]}")
            .when().post(inventory("/reservations"))
            .then().statusCode(201)
            .body("items[0].productId", equalTo("MONSTER-16OZ"))
            .body("items[0].substitutedFromProductId", equalTo("RED-BULL-12OZ"))
            .body("items[0].price", equalTo("2.99"))
            .extract().path("id");

    org.junit.Assert.assertEquals(monsterBefore - 2, onHand("MONSTER-16OZ"));

    given().when().post(inventory("/reservations/" + reservationId + "/release"))
            .then().statusCode(200).body("status", equalTo("RELEASED"));
    org.junit.Assert.assertEquals(monsterBefore, onHand("MONSTER-16OZ"));

    given().when().get(inventory("/reservations/" + reservationId)).then().statusCode(200).body("status", equalTo("RELEASED"));
  }

  @Test
  public void shouldReject409WhenNoInStockSubstitute() {
    given().contentType("application/json")
            .body("{\"storeId\": 1, \"items\": [{\"productId\": \"WINGS-8PC\", \"quantity\": 1}]}")
            .when().post(inventory("/reservations"))
            .then().statusCode(409)
            .body("rejectedProductIds", equalTo(java.util.Collections.singletonList("WINGS-8PC")));
  }

  @Test
  public void shouldScheduleDeliveryOnSeededDriver() {
    long orderId = 900000L + System.currentTimeMillis() % 100000;
    int driverId = given().contentType("application/json")
            .body("{\"orderId\": " + orderId + ", \"readyBy\": \"2026-10-29T12:00:00\"}")
            .when().post(delivery("/deliveries"))
            .then().statusCode(200)
            .body("actions[0].type", equalTo("PICKUP"))
            .body("actions[1].type", equalTo("DROPOFF"))
            .body("actions[1].time", equalTo("2026-10-29T12:30:00"))
            .extract().path("driverId");

    given().when().get(delivery("/drivers/" + driverId))
            .then().statusCode(200)
            .body("available", equalTo(true))
            .body("plan.actions.orderId", hasItem((int) orderId));
  }
}
