package com.sevenow.fulfillment.orderservice.web;

import com.sevenow.fulfillment.orderservice.domain.OrderServiceWithRepositoriesConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan
@Import(OrderServiceWithRepositoriesConfiguration.class)
public class OrderWebConfiguration {
}
