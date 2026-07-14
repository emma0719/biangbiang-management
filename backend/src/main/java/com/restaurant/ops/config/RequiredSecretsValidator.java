package com.restaurant.ops.config;

import java.util.Arrays;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class RequiredSecretsValidator implements ApplicationRunner {
  private final AppProperties properties;
  private final Environment environment;

  public RequiredSecretsValidator(AppProperties properties, Environment environment) {
    this.properties = properties;
    this.environment = environment;
  }

  @Override
  public void run(ApplicationArguments args) {
    boolean production = Arrays.asList(environment.getActiveProfiles()).contains("prod");
    if (production && (properties.jwt().secret() == null || properties.jwt().secret().isBlank())) {
      throw new IllegalStateException("JWT_SECRET is required in production");
    }
    if (production && (properties.toastPin().encryptionKey() == null || properties.toastPin().encryptionKey().isBlank())) {
      throw new IllegalStateException("TOAST_PIN_ENCRYPTION_KEY is required in production");
    }
  }
}
