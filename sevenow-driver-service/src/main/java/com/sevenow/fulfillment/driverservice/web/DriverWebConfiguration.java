package com.sevenow.fulfillment.driverservice.web;

import com.sevenow.fulfillment.driverservice.domain.DriverServiceConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(DriverServiceConfiguration.class)
@ComponentScan
public class DriverWebConfiguration {
}
