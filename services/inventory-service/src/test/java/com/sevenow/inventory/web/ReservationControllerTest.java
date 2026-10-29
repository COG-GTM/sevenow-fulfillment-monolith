package com.sevenow.inventory.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sevenow.inventory.common.Money;
import com.sevenow.inventory.common.MoneyModule;
import com.sevenow.inventory.domain.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReservationControllerTest {

  private ReservationService reservationService;

  private static final String BODY = "{\"storeId\": 1, \"items\": [" +
          "{\"productId\": \"RED-BULL-12OZ\", \"quantity\": 2}, {\"productId\": \"WATER-7SELECT-1L\", \"quantity\": 1}]}";

  @Before
  public void setUp() {
    reservationService = mock(ReservationService.class);
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new MoneyModule()).registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    io.restassured.module.mockmvc.RestAssuredMockMvc.standaloneSetup(MockMvcBuilders.standaloneSetup(new ReservationController(reservationService))
            .setControllerAdvice(new InventoryExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper)));
  }

  private static Reservation reservation(long id) {
    Reservation r = new Reservation(1L, Arrays.asList(
            new ReservationItem("MONSTER-16OZ", "Monster Energy (16 oz)", new Money("2.99"), 2, "RED-BULL-12OZ"),
            new ReservationItem("WATER-7SELECT-1L", "7-Select Purified Water (1 L)", new Money("1.49"), 1, null)));
    ReflectionTestUtils.setField(r, "id", id);
    return r;
  }

  @Test
  public void shouldReserveAndReturnResolvedLineItems() {
    when(reservationService.reserve(eq(1L), anyList())).thenReturn(reservation(7L));

    given().contentType("application/json").body(BODY)
            .when().post("/reservations")
            .then().statusCode(201)
            .body("id", equalTo(7))
            .body("storeId", equalTo(1))
            .body("status", equalTo("RESERVED"))
            .body("items[0].productId", equalTo("MONSTER-16OZ"))
            .body("items[0].name", equalTo("Monster Energy (16 oz)"))
            .body("items[0].price", equalTo("2.99"))
            .body("items[0].quantity", equalTo(2))
            .body("items[0].substitutedFromProductId", equalTo("RED-BULL-12OZ"))
            .body("items[1].substitutedFromProductId", nullValue());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<RequestedItem>> captor = ArgumentCaptor.forClass(List.class);
    verify(reservationService).reserve(eq(1L), captor.capture());
    assertEquals("RED-BULL-12OZ", captor.getValue().get(0).getProductId());
    assertEquals(2, captor.getValue().get(0).getQuantity());
  }

  @Test
  public void shouldReturn409WithRejectedProductIdsWhenStockInsufficient() {
    when(reservationService.reserve(eq(1L), anyList())).thenThrow(new ProductOutOfStockException(Collections.singletonList("WINGS-8PC")));

    given().contentType("application/json").body(BODY)
            .when().post("/reservations")
            .then().statusCode(409)
            .body("rejectedProductIds", equalTo(Collections.singletonList("WINGS-8PC")));
  }

  @Test
  public void shouldReturn400ForUnknownProduct() {
    when(reservationService.reserve(eq(1L), anyList())).thenThrow(new InvalidProductIdException("NOPE"));

    given().contentType("application/json").body(BODY)
            .when().post("/reservations")
            .then().statusCode(400)
            .body("invalidProductId", equalTo("NOPE"));
  }

  @Test
  public void shouldResolveSubstitutionsWithoutReserving() {
    when(reservationService.resolve(eq(1L), anyList())).thenReturn(reservation(7L).getItems());

    given().contentType("application/json").body(BODY)
            .when().post("/reservations/resolve")
            .then().statusCode(200)
            .body("items[0].productId", equalTo("MONSTER-16OZ"))
            .body("items[0].substitutedFromProductId", equalTo("RED-BULL-12OZ"));
  }

  @Test
  public void shouldReleaseReservation() {
    Reservation released = reservation(7L);
    released.release();
    when(reservationService.release(7L)).thenReturn(released);

    given().when().post("/reservations/7/release")
            .then().statusCode(200)
            .body("id", equalTo(7))
            .body("status", equalTo("RELEASED"));
  }

  @Test
  public void shouldReturn404WhenReleasingUnknownReservation() {
    when(reservationService.release(anyLong())).thenThrow(new ReservationNotFoundException(99L));

    given().when().post("/reservations/99/release").then().statusCode(404);
  }

  @Test
  public void shouldGetReservation() {
    when(reservationService.findById(7L)).thenReturn(Optional.of(reservation(7L)));
    when(reservationService.findById(99L)).thenReturn(Optional.empty());

    given().when().get("/reservations/7").then().statusCode(200).body("items.size()", equalTo(2));
    given().when().get("/reservations/99").then().statusCode(404);
  }
}
