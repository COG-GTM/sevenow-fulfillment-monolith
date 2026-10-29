package com.sevenow.fulfillment.customerservice.main;

import com.sevenow.fulfillment.commonswagger.CommonSwaggerConfiguration;
import com.sevenow.fulfillment.customerservice.web.CustomerWebConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@ComponentScan
@EntityScan
@Import({CustomerWebConfiguration.class, CommonSwaggerConfiguration.class})
public class CustomerServiceConfiguration {
}
