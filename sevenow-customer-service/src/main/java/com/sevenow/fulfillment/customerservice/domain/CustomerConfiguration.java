package com.sevenow.fulfillment.customerservice.domain;

import com.sevenow.fulfillment.domain.DomainConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(DomainConfiguration.class)
public class CustomerConfiguration {

  @Bean
  public CustomerService customerService() {
    return new CustomerService();
  }
}
