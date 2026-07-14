package com.restaurant.ops;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@EnabledIfEnvironmentVariable(named = "LOCAL_MYSQL_TESTS", matches = "true")
class AccountFlowLocalMySqlIntegrationTest extends AccountFlowMySqlIntegrationTest {
  @DynamicPropertySource
  static void localMysqlProperties(DynamicPropertyRegistry registry) {
    String url = System.getenv("LOCAL_MYSQL_TEST_DB_URL");
    if (url == null || !url.contains("_test")) {
      throw new IllegalStateException("LOCAL_MYSQL_TEST_DB_URL must point to a dedicated test database whose name contains _test");
    }
    registry.add("spring.datasource.url", () -> url);
    registry.add("spring.datasource.username", () -> System.getenv("LOCAL_MYSQL_TEST_DB_USERNAME"));
    registry.add("spring.datasource.password", () -> System.getenv("LOCAL_MYSQL_TEST_DB_PASSWORD"));
    registry.add("app.jwt.secret", () -> "01234567890123456789012345678901");
    registry.add("app.toast-pin.encryption-key", () -> "toast-pin-test-key-32-bytes-long");
    registry.add("app.public-base-url", () -> "https://app.example.com");
  }
}
