package com.restaurant.ops.common;

import org.springframework.stereotype.Component;

@Component
public class Normalizer {
  public String email(String email) {
    return email.trim().toLowerCase();
  }

  public String phone(String phone) {
    String digits = phone.replaceAll("[^0-9]", "");
    if (digits.length() == 10) {
      return "+1" + digits;
    }
    if (digits.length() == 11 && digits.startsWith("1")) {
      return "+" + digits;
    }
    return "+" + digits;
  }
}
