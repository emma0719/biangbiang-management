package com.restaurant.ops.storage;

import com.restaurant.ops.common.ApiException;
import com.restaurant.ops.config.AppProperties;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class LocalProfilePhotoStorage implements ProfilePhotoStorage {
  private final AppProperties properties;

  public LocalProfilePhotoStorage(AppProperties properties) {
    this.properties = properties;
  }

  @Override
  public String save(Long employeeId, String contentType, long sizeBytes, InputStream body) {
    try {
      String extension = switch (contentType) {
        case "image/jpeg" -> ".jpg";
        case "image/png" -> ".png";
        case "image/webp" -> ".webp";
        default -> throw new ApiException(HttpStatus.BAD_REQUEST, "PROFILE_PHOTO_UNSUPPORTED_TYPE");
      };
      String key = employeeId + "/" + UUID.randomUUID() + extension;
      Path root = Path.of(properties.profilePhoto().storageDir()).toAbsolutePath().normalize();
      Path destination = root.resolve(key).normalize();
      if (!destination.startsWith(root)) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "PROFILE_PHOTO_INVALID_KEY");
      }
      Files.createDirectories(destination.getParent());
      Files.copy(body, destination);
      return key;
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PROFILE_PHOTO_STORAGE_FAILED");
    }
  }

  @Override
  public void delete(String key) {
    if (key == null || key.isBlank()) return;
    try {
      Path root = Path.of(properties.profilePhoto().storageDir()).toAbsolutePath().normalize();
      Path target = root.resolve(key).normalize();
      if (target.startsWith(root)) {
        Files.deleteIfExists(target);
      }
    } catch (Exception e) {
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PROFILE_PHOTO_DELETE_FAILED");
    }
  }
}
