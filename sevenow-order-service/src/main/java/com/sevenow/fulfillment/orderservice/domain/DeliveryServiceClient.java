package com.sevenow.fulfillment.orderservice.domain;

import com.sevenow.fulfillment.domain.Order;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

/**
 * Thin RestTemplate client for delivery-service. {@code POST /deliveries} is idempotent per orderId,
 * so if the monolith's accept transaction rolls back after this call, retrying the accept returns
 * the same driver instead of double-booking.
 */
public class DeliveryServiceClient implements DeliveryScheduler {

  private final RestTemplate restTemplate;
  private final String baseUrl;

  public DeliveryServiceClient(RestTemplate restTemplate, String baseUrl) {
    this.restTemplate = restTemplate;
    this.baseUrl = baseUrl.replaceAll("/+$", "");
  }

  @Override
  public long scheduleDelivery(Order order, LocalDateTime readyBy) {
    Map<String, Object> body = new HashMap<>();
    body.put("eventType", "OrderAccepted");
    body.put("orderId", order.getId());
    body.put("readyBy", readyBy.toString());
    if (order.getDeliveryInformation() != null) {
      body.put("deliveryAddress", order.getDeliveryInformation().getDeliveryAddress());
    }
    Map<?, ?> response = restTemplate.postForObject(baseUrl + "/deliveries", body, Map.class);
    return ((Number) Objects.requireNonNull(response).get("driverId")).longValue();
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<DeliveryAction> actionsFor(long driverId, long orderId) {
    Map<String, Object> driver = restTemplate.getForObject(baseUrl + "/drivers/{driverId}", Map.class, driverId);
    Map<String, Object> plan = driver == null ? null : (Map<String, Object>) driver.get("plan");
    if (plan == null || plan.get("actions") == null) {
      return Collections.emptyList();
    }
    return ((List<Map<String, Object>>) plan.get("actions")).stream()
            .filter(a -> a.get("orderId") != null && ((Number) a.get("orderId")).longValue() == orderId)
            .map(a -> new DeliveryAction((String) a.get("type"), a.get("time") == null ? null : LocalDateTime.parse((String) a.get("time"))))
            .collect(toList());
  }
}
