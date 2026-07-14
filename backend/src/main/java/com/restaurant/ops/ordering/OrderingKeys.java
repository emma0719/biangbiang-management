package com.restaurant.ops.ordering;

import java.util.Locale;

final class OrderingKeys {
  private OrderingKeys() {}

  static String key(String value) {
    return value == null ? "" : value.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
  }

  static String productKey(String code, String name) {
    return key((code == null || code.isBlank() ? "" : code + " ") + name);
  }
}
