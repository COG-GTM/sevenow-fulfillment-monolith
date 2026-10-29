package com.sevenow.fulfillment.endtoendtests.common;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Mints the bearer tokens the 7NOW identity provider would issue, signed with SEVENOW_AUTH_JWT_SECRET.
 */
public class TestTokens {

  public static String customer(long customerId) {
    return sign(secret(), "CUSTOMER", "customerId", customerId);
  }

  public static String storeAssociate(long storeId) {
    return sign(secret(), "STORE_ASSOCIATE", "storeId", storeId);
  }

  public static String driver(long driverId) {
    return sign(secret(), "DRIVER", "driverId", driverId);
  }

  public static String bearer(String token) {
    return "Bearer " + token;
  }

  public static String sign(String secret, String role, String idClaim, long id) {
    JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject(role.toLowerCase() + "-" + id)
            .claim("role", role)
            .claim(idClaim, id)
            .expirationTime(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
            .build();
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    try {
      jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
    return jwt.serialize();
  }

  private static String secret() {
    String secret = System.getenv("SEVENOW_AUTH_JWT_SECRET");
    if (secret == null || secret.isEmpty()) {
      throw new IllegalStateException("SEVENOW_AUTH_JWT_SECRET must be set (see set-env.sh)");
    }
    return secret;
  }
}
