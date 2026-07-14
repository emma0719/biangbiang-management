package com.restaurant.ops.profile;

import com.restaurant.ops.common.ApiException;
import com.restaurant.ops.config.AppProperties;
import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.storage.ProfilePhotoStorage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfilePhotoService {
  private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
  private final ProfilePhotoStorage storage;
  private final AppProperties properties;

  public ProfilePhotoService(ProfilePhotoStorage storage, AppProperties properties) {
    this.storage = storage;
    this.properties = properties;
  }

  @Transactional
  public String upload(Employee employee, MultipartFile file) {
    validate(file);
    try {
      byte[] bytes = file.getBytes();
      String oldKey = employee.getProfilePhotoKey();
      String key = storage.save(employee.getId(), file.getContentType(), bytes.length, new ByteArrayInputStream(bytes));
      employee.setProfilePhotoKey(key);
      storage.delete(oldKey);
      return key;
    } catch (IOException e) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "PROFILE_PHOTO_INVALID");
    }
  }

  @Transactional
  public void delete(Employee employee) {
    String oldKey = employee.getProfilePhotoKey();
    employee.setProfilePhotoKey(null);
    storage.delete(oldKey);
  }

  private void validate(MultipartFile file) {
    if (file == null || file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "PROFILE_PHOTO_REQUIRED");
    if (file.getSize() > properties.profilePhoto().maxBytes()) throw new ApiException(HttpStatus.BAD_REQUEST, "PROFILE_PHOTO_TOO_LARGE");
    String contentType = file.getContentType();
    if (!ALLOWED_TYPES.contains(contentType)) throw new ApiException(HttpStatus.BAD_REQUEST, "PROFILE_PHOTO_UNSUPPORTED_TYPE");
    try {
      byte[] bytes = file.getBytes();
      boolean valid = switch (contentType) {
        case "image/jpeg" -> bytes.length > 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
        case "image/png" -> bytes.length > 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47;
        case "image/webp" -> bytes.length > 12 && bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46 && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50;
        default -> false;
      };
      if (!valid) throw new ApiException(HttpStatus.BAD_REQUEST, "PROFILE_PHOTO_INVALID_TYPE");
    } catch (IOException e) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "PROFILE_PHOTO_INVALID");
    }
  }
}
