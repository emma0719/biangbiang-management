package com.restaurant.ops.storage;

import java.io.InputStream;

public interface ProfilePhotoStorage {
  String save(Long employeeId, String contentType, long sizeBytes, InputStream body);
  void delete(String key);
}
