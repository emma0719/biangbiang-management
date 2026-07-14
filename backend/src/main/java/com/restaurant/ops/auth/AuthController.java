package com.restaurant.ops.auth;

import com.restaurant.ops.profile.PendingContactChange;
import com.restaurant.ops.profile.ProfileService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  AuthDtos.TokenResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
    return authService.login(request);
  }

  @PostMapping("/refresh")
  AuthDtos.TokenResponse refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
    return authService.refresh(request.refreshToken());
  }

  @PostMapping("/logout")
  Map<String, String> logout(@Valid @RequestBody AuthDtos.RefreshRequest request) {
    authService.logoutCurrent(request.refreshToken());
    return Map.of("status", "ok");
  }

  @PostMapping("/logout-all")
  Map<String, String> logoutAll(@AuthenticationPrincipal AppPrincipal principal) {
    authService.logoutAll(principal.employee());
    return Map.of("status", "ok");
  }

  @PostMapping("/forgot-password")
  Map<String, String> forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
    authService.forgotPassword(request);
    return Map.of("status", "ok");
  }

  @PostMapping("/reset-password")
  Map<String, String> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
    authService.resetPassword(request);
    return Map.of("status", "ok");
  }
}
