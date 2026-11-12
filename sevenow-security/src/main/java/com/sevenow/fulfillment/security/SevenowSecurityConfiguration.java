package com.sevenow.fulfillment.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Stateless bearer-token security. All order and driver endpoints and customer reads require an authenticated caller,
 * creating a store requires STORE_ADMIN, and registering or approving a driver requires DISPATCHER. Which caller may
 * act on which order, customer or driver is enforced in OrderService, CustomerService and DriverService.
 */
@Configuration
@EnableWebSecurity
public class SevenowSecurityConfiguration extends WebSecurityConfigurerAdapter {

  static final String[] ORDER_LIFECYCLE_PATHS = {
          "/orders/*/cancel", "/orders/*/revise",
          "/orders/*/accept", "/orders/*/picking", "/orders/*/packed", "/orders/*/ready",
          "/orders/*/pickedup", "/orders/*/delivered"
  };

  private final String jwtSecret;

  public SevenowSecurityConfiguration(@Value("${sevenow.auth.jwt.secret:}") String jwtSecret) {
    this.jwtSecret = jwtSecret;
  }

  @Bean
  public JwtAuthenticationProvider jwtAuthenticationProvider() {
    return new JwtAuthenticationProvider(jwtSecret);
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http
            .csrf().disable()
            .logout().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            .exceptionHandling().authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)).and()
            .addFilterBefore(new JwtAuthenticationFilter(jwtAuthenticationProvider()), AnonymousAuthenticationFilter.class)
            .authorizeRequests()
            .antMatchers(HttpMethod.POST, ORDER_LIFECYCLE_PATHS).authenticated()
            .mvcMatchers("/orders", "/orders/**").authenticated()
            .mvcMatchers(HttpMethod.GET, "/customers/**").authenticated()
            .mvcMatchers(HttpMethod.POST, "/stores").hasRole(SevenowRole.STORE_ADMIN.name())
            .mvcMatchers(HttpMethod.POST, "/drivers", "/drivers/{driverId}/approve").hasRole(SevenowRole.DISPATCHER.name())
            .mvcMatchers("/drivers", "/drivers/**").authenticated()
            .anyRequest().permitAll();
  }
}
