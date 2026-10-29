package com.sevenow.delivery.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sevenow.delivery.common.Address;
import com.sevenow.delivery.common.PersonName;
import com.sevenow.delivery.domain.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.standaloneSetup;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeliveryControllerTest {

  private static final LocalDateTime READY_BY = LocalDateTime.of(2026, 10, 29, 12, 0);

  private DeliveryService deliveryService;
  private DriverService driverService;
  private Driver dana;

  @Before
  public void setUp() {
    deliveryService = mock(DeliveryService.class);
    driverService = mock(DriverService.class);
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    standaloneSetup(MockMvcBuilders.standaloneSetup(new DeliveryController(deliveryService), new DriverController(driverService))
            .setControllerAdvice(new DeliveryExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper)));
    dana = new Driver(new PersonName("Dana", "Driver"), new Address("5601 N MacArthur Blvd", null, "Irving", "TX", "75038"));
    ReflectionTestUtils.setField(dana, "id", 1L);
    dana.noteAvailable();
  }

  @Test
  public void shouldScheduleDeliveryAndReturnAssignedDriver() {
    dana.addAction(Action.makePickup(42L));
    dana.addAction(Action.makeDropoff(42L, READY_BY.plusMinutes(30)));
    when(deliveryService.schedule(42L, READY_BY)).thenReturn(dana);

    given().contentType("application/json")
            .body("{\"orderId\": 42, \"readyBy\": \"2026-10-29T12:00:00\"," +
                    " \"deliveryAddress\": {\"street1\": \"1 Main St\", \"city\": \"Irving\", \"state\": \"TX\", \"zip\": \"75063\"}}")
            .when().post("/deliveries")
            .then().statusCode(200)
            .body("orderId", equalTo(42))
            .body("driverId", equalTo(1))
            .body("deliveryAddress.street1", equalTo("1 Main St"))
            .body("actions[0].type", equalTo("PICKUP"))
            .body("actions[0].orderId", equalTo(42))
            .body("actions[0].time", nullValue())
            .body("actions[1].type", equalTo("DROPOFF"))
            .body("actions[1].time", equalTo("2026-10-29T12:30:00"));
  }

  @Test
  public void shouldAcceptOrderAcceptedEventPayload() {
    when(deliveryService.schedule(eq(7L), eq(READY_BY))).thenReturn(dana);

    given().contentType("application/json")
            .body("{\"eventType\": \"OrderAccepted\", \"orderId\": 7, \"readyBy\": \"2026-10-29T12:00:00\"}")
            .when().post("/deliveries")
            .then().statusCode(200).body("driverId", equalTo(1));
  }

  @Test
  public void shouldReturn409WhenNoDriverAvailable() {
    when(deliveryService.schedule(eq(42L), eq(READY_BY))).thenThrow(new NoDriverAvailableException(42L));

    given().contentType("application/json")
            .body("{\"orderId\": 42, \"readyBy\": \"2026-10-29T12:00:00\"}")
            .when().post("/deliveries")
            .then().statusCode(409);
  }

  @Test
  public void shouldReturn400WhenOrderIdMissing() {
    given().contentType("application/json")
            .body("{\"readyBy\": \"2026-10-29T12:00:00\"}")
            .when().post("/deliveries")
            .then().statusCode(400);
  }

  @Test
  public void shouldCreateDriverAndUpdateAvailability() {
    when(driverService.createDriver(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(dana);

    given().contentType("application/json")
            .body("{\"name\": {\"firstName\": \"Dana\", \"lastName\": \"Driver\"}, \"address\": {\"city\": \"Irving\"}}")
            .when().post("/drivers")
            .then().statusCode(200).body("id", equalTo(1));

    given().contentType("application/json").body("{\"available\": false}")
            .when().post("/drivers/1/availability")
            .then().statusCode(200);
    verify(driverService).updateAvailability(1L, false);
  }

  @Test
  public void shouldGetDriverWithPlanActions() {
    dana.addAction(Action.makePickup(42L));
    when(driverService.findDriverById(1L)).thenReturn(dana);
    when(driverService.findDriverById(99L)).thenThrow(new DriverNotFoundException(99L));

    given().when().get("/drivers/1")
            .then().statusCode(200)
            .body("name.firstName", equalTo("Dana"))
            .body("available", equalTo(true))
            .body("plan.actions[0].type", equalTo("PICKUP"))
            .body("plan.actions[0].orderId", equalTo(42));
    given().when().get("/drivers/99").then().statusCode(404);
  }
}
