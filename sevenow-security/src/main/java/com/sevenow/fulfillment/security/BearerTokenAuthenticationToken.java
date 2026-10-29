package com.sevenow.fulfillment.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;

public class BearerTokenAuthenticationToken extends AbstractAuthenticationToken {

  private final String token;

  public BearerTokenAuthenticationToken(String token) {
    super(Collections.emptyList());
    this.token = token;
    setAuthenticated(false);
  }

  public String getToken() {
    return token;
  }

  @Override
  public Object getCredentials() {
    return token;
  }

  @Override
  public Object getPrincipal() {
    return token;
  }
}
