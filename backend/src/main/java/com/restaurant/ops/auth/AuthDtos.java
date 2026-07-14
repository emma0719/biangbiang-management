package com.restaurant.ops.auth;

import com.restaurant.ops.employee.Position;
import com.restaurant.ops.employee.StoreCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public class AuthDtos {
  public record ActivationRequest(
      @NotBlank String token,
      @NotBlank String englishName,
      @NotBlank String preferredName,
      @Email @NotBlank String email,
      @NotBlank String phone,
      @NotBlank String password,
      @NotBlank String toastPin,
      @NotNull StoreCode homeStore,
      @NotEmpty Set<StoreCode> eligibleStores
  ) {}

  public record LoginRequest(@NotBlank String identifier, @NotBlank String password) {}
  public record RefreshRequest(@NotBlank String refreshToken) {}
  public record TokenResponse(String accessToken, String refreshToken) {}
  public record ForgotPasswordRequest(@NotBlank String identifier, @NotBlank String channel) {}
  public record ResetPasswordRequest(@NotBlank String token, @NotBlank String password) {}
  public record ActivationResponse(TokenResponse tokens, Set<Position> positions) {}
}
