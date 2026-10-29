package com.sevenow.fulfillment.orderservice.web;

import com.sevenow.fulfillment.orderservice.domain.OrderServiceWithRepositoriesConfiguration;
import com.sevenow.fulfillment.security.SevenowSecurityConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan
@Import({OrderServiceWithRepositoriesConfiguration.class, SevenowSecurityConfiguration.class})
public class OrderWebConfiguration {
}
