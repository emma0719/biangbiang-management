package com.restaurant.ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.restaurant.ops.common.ApiException;
import com.restaurant.ops.config.AppProperties;
import com.restaurant.ops.security.SensitiveValueProtector;
import org.junit.jupiter.api.Test;

class SensitiveValueProtectorTest {
  @Test
  void encryptsAndDecryptsWithToastPinKey() {
    SensitiveValueProtector protector = new SensitiveValueProtector(properties("toast-pin-key"));

    String encrypted = protector.protect("0123");

    assertThat(encrypted).isNotEqualTo("0123");
    assertThat(protector.reveal(encrypted)).isEqualTo("0123");
  }

  @Test
  void samePinProducesDifferentCiphertextBecauseNonceIsFresh() {
    SensitiveValueProtector protector = new SensitiveValueProtector(properties("toast-pin-key"));

    assertThat(protector.protect("0123")).isNotEqualTo(protector.protect("0123"));
  }

  @Test
  void missingToastPinKeyFailsAndDoesNotFallbackToJwtSecret() {
    SensitiveValueProtector protector = new SensitiveValueProtector(properties(""));

    assertThatThrownBy(() -> protector.protect("0123"))
        .isInstanceOf(ApiException.class)
        .hasMessage("TOAST_PIN_ENCRYPTION_KEY_NOT_CONFIGURED");
  }

  private AppProperties properties(String toastKey) {
    return new AppProperties(
        "https://app.example.com",
        new AppProperties.Cors("http://localhost:8081"),
        new AppProperties.Jwt("test", "jwt-secret-that-must-not-be-used", 15),
        new AppProperties.ToastPin(toastKey),
        new AppProperties.ProfilePhoto("target/test-profile-photos", 1024),
        new AppProperties.Security(8, 24, 30, 30, 10),
        new AppProperties.Bootstrap(false, "", "", "", "", "", ""),
        new AppProperties.DevelopmentSeed(false),
        new AppProperties.OrderingSeed(false)
    );
  }
}
