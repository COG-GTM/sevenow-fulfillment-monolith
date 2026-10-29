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

import java.util.Optional;

@Configuration
@Import(DomainConfiguration.class)
public class OrderConfiguration {
  // TODO move to framework
  @Bean
  public OrderService orderService(StoreRepository storeRepository,
                                   OrderRepository orderRepository,
                                   Optional<MeterRegistry> meterRegistry,
                                   CustomerService customerService, DriverRepository driverRepository) {
    return new OrderService(orderRepository,
            storeRepository,
            meterRegistry,
            customerService, driverRepository);
  }

  @Bean
  public MeterRegistryCustomizer meterRegistryCustomizer(@Value("${spring.application.name}") String serviceName) {
    return registry -> registry.config().commonTags("service", serviceName);
  }
}
