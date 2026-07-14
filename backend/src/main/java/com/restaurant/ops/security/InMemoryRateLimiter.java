package com.restaurant.ops.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRateLimiter implements RateLimiter {
  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  @Override
  public boolean allow(String key, int maxPerMinute) {
    long minute = Instant.now().getEpochSecond() / 60;
    Bucket bucket = buckets.compute(key, (ignored, existing) -> {
      if (existing == null || existing.minute != minute) return new Bucket(minute);
      return existing;
    });
    return bucket.count.incrementAndGet() <= maxPerMinute;
  }

  private static class Bucket {
    private final long minute;
    private final AtomicInteger count = new AtomicInteger();

    private Bucket(long minute) {
      this.minute = minute;
    }
  }
}
