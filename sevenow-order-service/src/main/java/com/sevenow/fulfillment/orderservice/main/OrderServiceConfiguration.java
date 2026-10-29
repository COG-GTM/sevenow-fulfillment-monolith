package com.sevenow.fulfillment.orderservice.main;

import com.sevenow.fulfillment.commonswagger.CommonSwaggerConfiguration;
import com.sevenow.fulfillment.orderservice.domain.OrderConfiguration;
import com.sevenow.fulfillment.orderservice.web.OrderWebConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@ComponentScan
@EntityScan
@Import({OrderConfiguration.class, OrderWebConfiguration.class, CommonSwaggerConfiguration.class})
public class OrderServiceConfiguration {
}
