package com.restaurant.ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.restaurant.ops.common.ApiException;
import com.restaurant.ops.config.AppProperties;
import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.Position;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.profile.ProfilePhotoService;
import com.restaurant.ops.storage.LocalProfilePhotoStorage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ProfilePhotoServiceTest {
  @Test
  void acceptsValidPngAndDeletesReplacement() {
    AppProperties properties = properties();
    ProfilePhotoService service = new ProfilePhotoService(new LocalProfilePhotoStorage(properties), properties);
    Employee employee = employee();
    MockMultipartFile first = new MockMultipartFile("file", "a.png", "image/png", pngBytes());
    MockMultipartFile second = new MockMultipartFile("file", "b.png", "image/png", pngBytes());

    String firstKey = service.upload(employee, first);
    String secondKey = service.upload(employee, second);

    assertThat(secondKey).isNotEqualTo(firstKey);
    assertThat(employee.getProfilePhotoKey()).isEqualTo(secondKey);
    assertThat(Files.exists(Path.of(properties.profilePhoto().storageDir(), firstKey))).isFalse();
  }

  @Test
  void rejectsDisguisedImageAndOversizedFile() {
    AppProperties properties = properties();
    ProfilePhotoService service = new ProfilePhotoService(new LocalProfilePhotoStorage(properties), properties);

    assertThatThrownBy(() -> service.upload(employee(), new MockMultipartFile("file", "fake.png", "image/png", "not-png".getBytes())))
        .isInstanceOf(ApiException.class)
        .hasMessage("PROFILE_PHOTO_INVALID_TYPE");
    assertThatThrownBy(() -> service.upload(employee(), new MockMultipartFile("file", "big.png", "image/png", new byte[2048])))
        .isInstanceOf(ApiException.class)
        .hasMessage("PROFILE_PHOTO_TOO_LARGE");
  }

  @Test
  void deleteClearsProfilePhotoKey() {
    AppProperties properties = properties();
    ProfilePhotoService service = new ProfilePhotoService(new LocalProfilePhotoStorage(properties), properties);
    Employee employee = employee();
    service.upload(employee, new MockMultipartFile("file", "a.png", "image/png", pngBytes()));

    service.delete(employee);

    assertThat(employee.getProfilePhotoKey()).isNull();
  }

  private byte[] pngBytes() {
    return new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0};
  }

  private Employee employee() {
    Employee employee = new Employee();
    employee.setEnglishName("Photo");
    employee.setPreferredName("Photo");
    employee.setNormalizedEmail("photo@example.com");
    employee.setNormalizedPhone("+12065550111");
    employee.setPasswordHash("hash");
    employee.setToastPinHash("pin");
    employee.setToastPinCiphertext("protected");
    employee.setHomeStore(StoreCode.SEATTLE);
    employee.setEligibleStores(EnumSet.of(StoreCode.SEATTLE));
    employee.setPositions(EnumSet.of(Position.HOST));
    return employee;
  }

  private AppProperties properties() {
    return new AppProperties(
        "https://app.example.com",
        new AppProperties.Cors("http://localhost:8081"),
        new AppProperties.Jwt("test", "jwt-secret", 15),
        new AppProperties.ToastPin("toast-pin-test-key"),
        new AppProperties.ProfilePhoto("target/test-profile-photos", 1024),
        new AppProperties.Security(8, 24, 30, 30, 10),
        new AppProperties.Bootstrap(false, "", "", "", "", "", ""),
        new AppProperties.DevelopmentSeed(false),
        new AppProperties.OrderingSeed(false)
    );
  }
}
