package com.sevenow.fulfillment.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.PlainHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.junit.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtAuthenticationProviderTest {

  private static final String SECRET = "unit-test-signing-secret-0123456789abcdef";

  private final JwtAuthenticationProvider provider = new JwtAuthenticationProvider(SECRET);

  @Test
  public void shouldAuthenticateValidToken() throws Exception {
    Authentication authentication = provider.authenticate(new BearerTokenAuthenticationToken(
            sign(SECRET, claims("DRIVER", "driverId", 42L, 60_000))));

    SevenowPrincipal principal = (SevenowPrincipal) authentication.getPrincipal();
    assertThat(authentication.isAuthenticated()).isTrue();
    assertThat(principal.getRole()).isEqualTo(SevenowRole.DRIVER);
    assertThat(principal.getId()).isEqualTo(42L);
    assertThat(principal.isDriver(42L)).isTrue();
    assertThat(principal.isDriver(43L)).isFalse();
    assertThat(principal.isCustomer(42L)).isFalse();
    assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_DRIVER");
  }

  @Test(expected = BadCredentialsException.class)
  public void shouldRejectTokenSignedWithOtherSecret() throws Exception {
    provider.authenticate(new BearerTokenAuthenticationToken(
            sign("some-other-signing-secret-0123456789abcdef", claims("CUSTOMER", "customerId", 1L, 60_000))));
  }

  @Test(expected = BadCredentialsException.class)
  public void shouldRejectExpiredToken() throws Exception {
    provider.authenticate(new BearerTokenAuthenticationToken(sign(SECRET, claims("CUSTOMER", "customerId", 1L, -1_000))));
  }

  @Test(expected = BadCredentialsException.class)
  public void shouldRejectUnsignedToken() {
    provider.authenticate(new BearerTokenAuthenticationToken(
            new PlainJWT(new PlainHeader(), claims("CUSTOMER", "customerId", 1L, 60_000)).serialize()));
  }

  @Test(expected = BadCredentialsException.class)
  public void shouldRejectTokenWithoutRoleIdClaim() throws Exception {
    provider.authenticate(new BearerTokenAuthenticationToken(sign(SECRET, claims("STORE_ASSOCIATE", "customerId", 1L, 60_000))));
  }

  @Test(expected = BadCredentialsException.class)
  public void shouldRejectUnknownRole() throws Exception {
    provider.authenticate(new BearerTokenAuthenticationToken(sign(SECRET, claims("ADMIN", "customerId", 1L, 60_000))));
  }

  @Test(expected = BadCredentialsException.class)
  public void shouldRejectGarbage() {
    provider.authenticate(new BearerTokenAuthenticationToken("not-a-jwt"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRequireStrongSecret() {
    new JwtAuthenticationProvider("too-short");
  }

  private static JWTClaimsSet claims(String role, String idClaim, long id, long expiresInMillis) {
    return new JWTClaimsSet.Builder()
            .subject("user-" + id)
            .claim("role", role)
            .claim(idClaim, id)
            .expirationTime(new Date(System.currentTimeMillis() + expiresInMillis))
            .build();
  }

  private static String sign(String secret, JWTClaimsSet claims) throws Exception {
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
    return jwt.serialize();
  }
}
