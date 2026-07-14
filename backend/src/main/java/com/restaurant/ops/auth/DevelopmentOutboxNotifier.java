package com.restaurant.ops.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
public class DevelopmentOutboxNotifier implements OutboxNotifier {
  private static final Logger log = LoggerFactory.getLogger(DevelopmentOutboxNotifier.class);
  private final Map<String, VerificationMessage> latestMessages = new ConcurrentHashMap<>();

  @Override
  public void sendPasswordReset(String channel, String destination, String token) {
    latestMessages.put(key("password-reset", channel, destination), new VerificationMessage("password-reset", channel, destination, token, Instant.now()));
    log.info("Development password reset issued through {} for {}", channel, destination);
  }

  @Override
  public void sendContactVerification(String channel, String destination, String token) {
    latestMessages.put(key("contact", channel, destination), new VerificationMessage("contact", channel, destination, token, Instant.now()));
    log.info("Development contact verification issued through {} for {}", channel, destination);
  }

  public Optional<VerificationMessage> latest(String purpose, String channel, String destination) {
    return Optional.ofNullable(latestMessages.get(key(purpose, channel, destination)));
  }

  private String key(String purpose, String channel, String destination) {
    return purpose + ":" + channel + ":" + destination;
  }

  public record VerificationMessage(String purpose, String channel, String destination, String token, Instant issuedAt) {}
}
