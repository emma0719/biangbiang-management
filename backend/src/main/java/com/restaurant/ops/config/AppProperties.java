package com.restaurant.ops.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    String publicBaseUrl,
    Cors cors,
    Jwt jwt,
    ToastPin toastPin,
    ProfilePhoto profilePhoto,
    Security security,
    Bootstrap bootstrap,
    DevelopmentSeed developmentSeed,
    OrderingSeed orderingSeed
) {
  public record Cors(String allowedOrigins) {}
  public record Jwt(String issuer, String secret, long accessTokenMinutes) {}
  public record ToastPin(String encryptionKey) {}
  public record ProfilePhoto(String storageDir, long maxBytes) {}
  public record Security(int passwordMinLength, int invitationHours, int resetTokenMinutes, int contactChangeMinutes, int rateLimitPerMinute) {}
  public record Bootstrap(boolean enabled, String email, String phone, String password, String englishName, String preferredName, String toastPin) {}
  public record DevelopmentSeed(boolean enabled) {}
  public record OrderingSeed(boolean enabled) {}
}
