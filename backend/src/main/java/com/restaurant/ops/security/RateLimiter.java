package com.restaurant.ops.security;

public interface RateLimiter {
  boolean allow(String key, int maxPerMinute);
}
