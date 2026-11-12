package com.sevenow.fulfillment.orderservice.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sevenow.fulfillment.common.MoneyModule;
import com.sevenow.fulfillment.domain.OrderRepository;
import com.sevenow.fulfillment.orderservice.OrderDetailsMother;
import com.sevenow.fulfillment.orderservice.api.web.CreateOrderRequest;
import com.sevenow.fulfillment.orderservice.domain.OrderService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static com.sevenow.fulfillment.orderservice.OrderDetailsMother.SLURPEE_ORDER;
import static com.sevenow.fulfillment.orderservice.OrderDetailsMother.SLURPEE_ORDER_TOTAL;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

  @Test
  public void shouldCreateOrder() {
    when(orderService.createOrder(anyLong(), anyLong(), anyList())).thenReturn(SLURPEE_ORDER);

    given().
            standaloneSetup(configureControllers(orderController)).
            contentType("application/json").
            body(orderBody(1)).
    when().
            post("/orders").
    then().
            statusCode(200);
  }

  @Test
  public void shouldRejectOrderWithTooManyLineItems() {
    assertCreateRejected(orderBody(CreateOrderRequest.MAX_LINE_ITEMS + 1));
  }

  @Test
  public void shouldRejectOrderWithNoLineItems() {
    assertCreateRejected(orderBody(0));
  }

  @Test
  public void shouldRejectOrderWithMissingLineItems() {
    assertCreateRejected("{\"customerId\": 1, \"storeId\": 1}");
  }

  private String orderBody(int lineItemCount) {
    List<String> lineItems = new ArrayList<>();
    for (int i = 0; i < lineItemCount; i++)
      lineItems.add("{\"productId\": \"SLURPEE-CHERRY-M\", \"quantity\": 1}");
    return "{\"customerId\": 1, \"storeId\": 1, \"lineItems\": [" + String.join(",", lineItems) + "]}";
  }

  private void assertCreateRejected(String body) {
    given().
            standaloneSetup(configureControllers(orderController)).
            contentType("application/json").
            body(body).
    when().
            post("/orders").
    then().
            statusCode(400);

    verify(orderService, never()).createOrder(anyLong(), anyLong(), anyList());
  }

  private StandaloneMockMvcBuilder configureControllers(Object... controllers) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new MoneyModule());
    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
    return MockMvcBuilders.standaloneSetup(controllers).setMessageConverters(converter);
  }

}