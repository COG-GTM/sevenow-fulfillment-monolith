package com.sevenow.fulfillment.storeservice;

import com.sevenow.fulfillment.commonswagger.CommonSwaggerConfiguration;
import com.sevenow.fulfillment.security.SevenowSecurityConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@ComponentScan
@Import({CommonSwaggerConfiguration.class, SevenowSecurityConfiguration.class})
public class StoreServiceConfiguration {
}
