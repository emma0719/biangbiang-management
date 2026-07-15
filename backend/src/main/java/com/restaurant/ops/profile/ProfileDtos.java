package com.restaurant.ops.profile;

import com.restaurant.ops.employee.EmployeeStatus;
import com.restaurant.ops.employee.Position;
import com.restaurant.ops.employee.StoreCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Set;

public class ProfileDtos {
  public record EmployeePublicResponse(Long id, String displayName, String profilePhotoKey, Set<Position> positions, StoreCode homeStore, EmployeeStatus status) {}
  public record EmployeePrivateResponse(
      Long id,
      String englishName,
      String preferredName,
      String displayName,
      String email,
      String phone,
      StoreCode homeStore,
      Set<StoreCode> eligibleStores,
      Set<Position> positions,
      EmployeeStatus status,
      String toastPin,
      Instant lastLoginAt,
      Instant createdAt
  ) {}
  public record UpdateProfileRequest(@NotBlank String englishName, @NotBlank String preferredName) {}
  public record UpdateStoresRequest(@NotEmpty Set<StoreCode> eligibleStores) {}
  public record ManagerUpdateStoresRequest(@NotNull StoreCode homeStore, @NotEmpty Set<StoreCode> eligibleStores) {}
  public record UpdateToastPinRequest(@NotBlank String toastPin) {}
  public record RequestEmailChange(@Email @NotBlank String email) {}
  public record RequestPhoneChange(@NotBlank String phone) {}
  public record VerifyContactChange(@NotBlank String token) {}
}
