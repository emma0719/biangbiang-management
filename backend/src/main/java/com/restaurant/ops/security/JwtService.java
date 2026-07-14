package com.restaurant.ops.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.restaurant.ops.config.AppProperties;
import com.restaurant.ops.employee.Employee;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.restaurant.ops.common.ApiException;

@Service
public class JwtService {
  private final AppProperties properties;

  public JwtService(AppProperties properties) {
    this.properties = properties;
  }

  public String createAccessToken(Employee employee) {
    if (properties.jwt().secret() == null || properties.jwt().secret().isBlank()) {
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "JWT_SECRET_NOT_CONFIGURED");
    }
    Instant now = Instant.now();
    return JWT.create()
        .withIssuer(properties.jwt().issuer())
        .withSubject(String.valueOf(employee.getId()))
        .withIssuedAt(now)
        .withExpiresAt(now.plus(properties.jwt().accessTokenMinutes(), ChronoUnit.MINUTES))
        .sign(Algorithm.HMAC256(properties.jwt().secret()));
  }

  public Long verifySubject(String token) {
    try {
      String subject = JWT.require(Algorithm.HMAC256(properties.jwt().secret()))
          .withIssuer(properties.jwt().issuer())
          .build()
          .verify(token)
          .getSubject();
      return Long.valueOf(subject);
    } catch (JWTVerificationException | NumberFormatException e) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_TOKEN");
    }
  }
}
