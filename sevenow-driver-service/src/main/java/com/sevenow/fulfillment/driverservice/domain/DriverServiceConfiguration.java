package com.sevenow.fulfillment.driverservice.domain;

import com.sevenow.fulfillment.domain.DriverRepository;
import com.sevenow.fulfillment.domain.DomainConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(DomainConfiguration.class)
public class DriverServiceConfiguration {

  @Bean
  public DriverService driverService(DriverRepository driverRepository) {
    return new DriverService(driverRepository);
  }

}
