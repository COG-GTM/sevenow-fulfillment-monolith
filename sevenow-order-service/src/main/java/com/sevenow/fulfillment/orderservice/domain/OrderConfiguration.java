package com.sevenow.fulfillment.orderservice.domain;

import io.micrometer.core.instrument.MeterRegistry;
import com.sevenow.fulfillment.customerservice.domain.CustomerService;
import com.sevenow.fulfillment.domain.DriverRepository;
import com.sevenow.fulfillment.domain.DomainConfiguration;
import com.sevenow.fulfillment.domain.OrderRepository;
import com.sevenow.fulfillment.domain.StoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Configuration
@Import(DomainConfiguration.class)
public class OrderConfiguration {
  // TODO move to framework
  @Bean
  public OrderService orderService(StoreRepository storeRepository,
                                   OrderRepository orderRepository,
                                   Optional<MeterRegistry> meterRegistry,
                                   CustomerService customerService, DeliveryScheduler deliveryScheduler) {
    return new OrderService(orderRepository,
            storeRepository,
            meterRegistry,
            customerService, deliveryScheduler);
  }

  /**
   * With {@code sevenow.delivery-service.url} set, deliveries are scheduled by the extracted
   * delivery-service over HTTP; otherwise the monolith keeps scheduling against its own drivers tables.
   */
  @Bean
  public DeliveryScheduler deliveryScheduler(@Value("${sevenow.delivery-service.url:}") String deliveryServiceUrl,
                                             @Value("${sevenow.delivery-service.connect-timeout-ms:2000}") int connectTimeoutMs,
                                             @Value("${sevenow.delivery-service.read-timeout-ms:5000}") int readTimeoutMs,
                                             DriverRepository driverRepository) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeoutMs);
    requestFactory.setReadTimeout(readTimeoutMs);
    return deliveryServiceUrl.isEmpty()
            ? new LocalDeliveryScheduler(driverRepository)
            : new DeliveryServiceClient(new RestTemplate(requestFactory), deliveryServiceUrl);
  }

  @Bean
  public MeterRegistryCustomizer meterRegistryCustomizer(@Value("${spring.application.name}") String serviceName) {
    return registry -> registry.config().commonTags("service", serviceName);
  }
}
