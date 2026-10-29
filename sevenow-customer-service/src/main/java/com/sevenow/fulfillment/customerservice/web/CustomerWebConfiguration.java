package com.sevenow.fulfillment.customerservice.web;

import com.sevenow.fulfillment.customerservice.domain.CustomerConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan
@Import(CustomerConfiguration.class)
public class CustomerWebConfiguration {
}
