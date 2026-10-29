package com.sevenow.fulfillment.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;

/**
 * Authenticates HS256 bearer tokens issued by the 7NOW identity provider / API gateway.
 * Required claims: {@code sub}, {@code exp}, {@code role} (a {@link SevenowRole}) and the role's id claim
 * ({@code customerId}, {@code storeId} or {@code driverId}).
 */
public class JwtAuthenticationProvider implements AuthenticationProvider {

  public static final int MIN_SECRET_BYTES = 32;

  private final MACVerifier verifier;

  public JwtAuthenticationProvider(String secret) {
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
      throw new IllegalArgumentException("sevenow.auth.jwt.secret must be set to at least " + MIN_SECRET_BYTES + " bytes");
    }
    try {
      this.verifier = new MACVerifier(secret.getBytes(StandardCharsets.UTF_8));
    } catch (JOSEException e) {
      throw new IllegalArgumentException("Invalid sevenow.auth.jwt.secret", e);
    }
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    SevenowPrincipal principal = decode(((BearerTokenAuthenticationToken) authentication).getToken());
    return new UsernamePasswordAuthenticationToken(principal, null,
            Collections.singletonList(new SimpleGrantedAuthority(principal.getRole().authority())));
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
  }

  SevenowPrincipal decode(String token) {
    try {
      SignedJWT jwt = SignedJWT.parse(token);
      if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(verifier)) {
        throw new BadCredentialsException("Invalid token signature");
      }
      JWTClaimsSet claims = jwt.getJWTClaimsSet();
      Date expiration = claims.getExpirationTime();
      if (expiration == null || !expiration.after(new Date())) {
        throw new BadCredentialsException("Token expired");
      }
      String subject = claims.getSubject();
      String roleName = claims.getStringClaim("role");
      if (subject == null || roleName == null) {
        throw new BadCredentialsException("Token missing sub or role");
      }
      SevenowRole role = SevenowRole.valueOf(roleName);
      Long id = claims.getLongClaim(role.getIdClaim());
      if (id == null) {
        throw new BadCredentialsException("Token missing " + role.getIdClaim());
      }
      return new SevenowPrincipal(subject, role, id);
    } catch (ParseException | JOSEException | IllegalArgumentException e) {
      throw new BadCredentialsException("Invalid token", e);
    }
  }
}
