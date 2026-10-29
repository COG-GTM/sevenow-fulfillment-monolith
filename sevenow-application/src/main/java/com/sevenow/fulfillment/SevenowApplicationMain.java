package com.sevenow.fulfillment;

import com.sevenow.fulfillment.customerservice.main.CustomerServiceConfiguration;
import com.sevenow.fulfillment.orderservice.main.OrderServiceConfiguration;
import com.sevenow.fulfillment.storeservice.StoreServiceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@ComponentScan
@Import({CustomerServiceConfiguration.class,
        OrderServiceConfiguration.class,
        StoreServiceConfiguration.class})
public class SevenowApplicationMain {

  public static void main(String[] args) {
    SpringApplication.run(SevenowApplicationMain.class, args);
  }
}
