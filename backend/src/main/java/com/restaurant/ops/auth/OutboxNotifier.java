package com.restaurant.ops.auth;

public interface OutboxNotifier {
  void sendPasswordReset(String channel, String destination, String token);
  void sendContactVerification(String channel, String destination, String token);
}
