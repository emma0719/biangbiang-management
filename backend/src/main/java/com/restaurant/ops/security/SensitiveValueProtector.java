package com.restaurant.ops.security;

import com.restaurant.ops.common.ApiException;
import com.restaurant.ops.config.AppProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SensitiveValueProtector {
  private static final int IV_BYTES = 12;
  private static final int GCM_TAG_BITS = 128;
  private final AppProperties properties;
  private final SecureRandom secureRandom = new SecureRandom();

  public SensitiveValueProtector(AppProperties properties) {
    this.properties = properties;
  }

  public String protect(String plaintext) {
    try {
      byte[] iv = new byte[IV_BYTES];
      secureRandom.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));
      byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
      buffer.put(iv);
      buffer.put(encrypted);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
    } catch (Exception e) {
      if (e instanceof ApiException apiException) {
        throw apiException;
      }
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SENSITIVE_VALUE_PROTECTION_FAILED");
    }
  }

  public String reveal(String protectedValue) {
    try {
      byte[] bytes = Base64.getUrlDecoder().decode(protectedValue);
      ByteBuffer buffer = ByteBuffer.wrap(bytes);
      byte[] iv = new byte[IV_BYTES];
      buffer.get(iv);
      byte[] encrypted = new byte[buffer.remaining()];
      buffer.get(encrypted);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception e) {
      if (e instanceof ApiException apiException) {
        throw apiException;
      }
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SENSITIVE_VALUE_PROTECTION_FAILED");
    }
  }

  private SecretKeySpec key() throws Exception {
    String secret = properties.toastPin().encryptionKey();
    if (secret == null || secret.isBlank()) {
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "TOAST_PIN_ENCRYPTION_KEY_NOT_CONFIGURED");
    }
    byte[] hash = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
    return new SecretKeySpec(hash, "AES");
  }
}
