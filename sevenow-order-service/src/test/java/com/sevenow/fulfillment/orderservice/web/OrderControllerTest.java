package com.sevenow.fulfillment.orderservice.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sevenow.fulfillment.common.MoneyModule;
import com.sevenow.fulfillment.domain.InvalidOrderLineItemsException;
import com.sevenow.fulfillment.domain.OrderRepository;
import com.sevenow.fulfillment.domain.OrderRevision;
import com.sevenow.fulfillment.orderservice.api.web.CreateOrderRequest;
import com.sevenow.fulfillment.orderservice.api.web.ReviseOrderRequest;
import com.sevenow.fulfillment.orderservice.OrderDetailsMother;
import com.sevenow.fulfillment.orderservice.domain.OrderService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

import java.util.Collections;
import java.util.Optional;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static com.sevenow.fulfillment.orderservice.OrderDetailsMother.SLURPEE_ORDER;
import static com.sevenow.fulfillment.orderservice.OrderDetailsMother.SLURPEE_ORDER_TOTAL;
import static com.sevenow.fulfillment.orderservice.StoreMother.CHERRY_SLURPEE_ID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class OrderControllerTest {

  private OrderService orderService;
  private OrderRepository orderRepository;
  private OrderController orderController;

  @Before
  public void setUp() throws Exception {
    orderService = mock(OrderService.class);
    orderRepository = mock(OrderRepository.class);
    orderController = new OrderController(orderService, orderRepository);
  }


  @Test
  public void shouldFindOrder() {

    when(orderRepository.findById(1L)).thenReturn(Optional.of(SLURPEE_ORDER));

    given().
            standaloneSetup(configureControllers(orderController)).
    when().
            get("/orders/1").
    then().
            statusCode(200).
            body("orderId", equalTo(new Long(OrderDetailsMother.ORDER_ID).intValue())).
            body("state", equalTo(OrderDetailsMother.SLURPEE_ORDER_STATE.name())).
            body("orderTotal", equalTo(SLURPEE_ORDER_TOTAL.asString()))
    ;
  }

  @Test
  public void shouldFindNotOrder() {
    when(orderRepository.findById(1L)).thenReturn(Optional.empty());

    given().
            standaloneSetup(configureControllers(new OrderController(orderService, orderRepository))).
    when().
            get("/orders/1").
    then().
            statusCode(404)
    ;
  }

  private void postCreateOrder(int quantity) {
    given().
            standaloneSetup(configureControllers(orderController)).
            contentType("application/json").
            body(toJson(new CreateOrderRequest(1L, 1L, Collections.singletonList(new CreateOrderRequest.LineItem(CHERRY_SLURPEE_ID, quantity))))).
    when().
            post("/orders").
    then().
            statusCode(400);
  }

  @Test
  public void shouldRejectCreateWithZeroQuantity() {
    postCreateOrder(0);
    verifyZeroInteractions(orderService);
  }

  @Test
  public void shouldRejectCreateWithNegativeQuantity() {
    postCreateOrder(-1);
    verifyZeroInteractions(orderService);
  }

  @Test
  public void shouldRejectReviseWithNegativeQuantity() {
    given().
            standaloneSetup(configureControllers(orderController)).
            contentType("application/json").
            body(toJson(new ReviseOrderRequest(Collections.singletonMap(CHERRY_SLURPEE_ID, -1)))).
    when().
            post("/orders/1/revise").
    then().
            statusCode(400);
    verify(orderService, never()).reviseOrder(anyLong(), any(OrderRevision.class));
  }

  @Test
  public void shouldReturn400WhenRevisingUnknownProduct() {
    when(orderService.reviseOrder(anyLong(), any(OrderRevision.class)))
            .thenThrow(new InvalidOrderLineItemsException("Order does not contain product NOT-A-PRODUCT"));
    given().
            standaloneSetup(configureControllers(orderController)).
            contentType("application/json").
            body(toJson(new ReviseOrderRequest(Collections.singletonMap("NOT-A-PRODUCT", 1)))).
    when().
            post("/orders/1/revise").
    then().
            statusCode(400).
            body("message", equalTo("Order does not contain product NOT-A-PRODUCT"));
  }

  private String toJson(Object o) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new MoneyModule());
      return objectMapper.writeValueAsString(o);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private StandaloneMockMvcBuilder configureControllers(Object... controllers) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new MoneyModule());
    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
    return MockMvcBuilders.standaloneSetup(controllers).setMessageConverters(converter);
  }

}