package com.sevenow.fulfillment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

public class RequiredDatasourceCredentials implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    requireProperty(environment, "spring.datasource.username", "SPRING_DATASOURCE_USERNAME");
    requireProperty(environment, "spring.datasource.password", "SPRING_DATASOURCE_PASSWORD");
  }

  private void requireProperty(ConfigurableEnvironment environment, String property, String envVar) {
    if (!StringUtils.hasText(environment.getProperty(property))) {
      throw new IllegalStateException(property + " is not configured; set " + envVar);
    }
  }
}
