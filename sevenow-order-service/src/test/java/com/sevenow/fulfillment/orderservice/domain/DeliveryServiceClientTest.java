package com.sevenow.fulfillment.orderservice.domain;

import com.sevenow.fulfillment.domain.Order;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class DeliveryServiceClientTest {

  private MockRestServiceServer server;
  private DeliveryServiceClient client;

  @Before
  public void setUp() {
    RestTemplate restTemplate = new RestTemplate();
    server = MockRestServiceServer.createServer(restTemplate);
    client = new DeliveryServiceClient(restTemplate, "http://delivery-service:8083/");
  }

  @Test
  public void shouldPostOrderAcceptedToDeliveriesAndReturnDriverId() {
    Order order = mock(Order.class);
    when(order.getId()).thenReturn(42L);
    server.expect(requestTo("http://delivery-service:8083/deliveries"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.eventType").value("OrderAccepted"))
            .andExpect(jsonPath("$.orderId").value(42))
            .andExpect(jsonPath("$.readyBy").value("2026-10-29T12:00"))
            .andRespond(withSuccess("{\"orderId\":42,\"driverId\":1,\"actions\":[]}", MediaType.APPLICATION_JSON));

    assertThat(client.scheduleDelivery(order, LocalDateTime.of(2026, 10, 29, 12, 0))).isEqualTo(1L);
    server.verify();
  }

  @Test
  public void shouldReadDriverPlanActionsForOrder() {
    server.expect(requestTo("http://delivery-service:8083/drivers/1"))
            .andRespond(withSuccess("{\"id\":1,\"plan\":{\"actions\":[" +
                    "{\"type\":\"PICKUP\",\"orderId\":42,\"time\":null}," +
                    "{\"type\":\"PICKUP\",\"orderId\":7,\"time\":null}," +
                    "{\"type\":\"DROPOFF\",\"orderId\":42,\"time\":\"2026-10-29T12:30:00\"}]}}", MediaType.APPLICATION_JSON));

    List<DeliveryAction> actions = client.actionsFor(1L, 42L);

    assertThat(actions).extracting(DeliveryAction::getType).containsExactly("PICKUP", "DROPOFF");
    assertThat(actions.get(1).getTime()).isEqualTo(LocalDateTime.of(2026, 10, 29, 12, 30));
  }
}
