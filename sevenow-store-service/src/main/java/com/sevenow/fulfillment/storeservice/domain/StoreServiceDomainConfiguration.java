package com.sevenow.fulfillment.storeservice.domain;

import com.sevenow.fulfillment.domain.DomainConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(DomainConfiguration.class)
public class StoreServiceDomainConfiguration {

  @Bean
  public StoreService storeService() {
    return new StoreService();
  }
}
