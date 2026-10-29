package com.sevenow.fulfillment;

import com.sevenow.fulfillment.customerservice.main.CustomerServiceConfiguration;
import com.sevenow.fulfillment.endtoendtests.common.AbstractEndToEndTests;
import com.sevenow.fulfillment.orderservice.main.OrderServiceConfiguration;
import com.sevenow.fulfillment.storeservice.StoreServiceConfiguration;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes=SevenowApplicationTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SevenowApplicationTest extends AbstractEndToEndTests {

  @Configuration
  @EnableAutoConfiguration
  @ComponentScan
  @Import({CustomerServiceConfiguration.class,
          OrderServiceConfiguration.class,
          StoreServiceConfiguration.class})
  public static class Config {

  }

  @LocalServerPort
  private int port;

  @Override
  public String getHost() {
    return "localhost";
  }

  @Override
  public int getApplicationPort() {
    return port;
  }
}
