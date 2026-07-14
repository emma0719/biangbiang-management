package com.restaurant.ops;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.restaurant.ops.auth.AuthDtos;
import com.restaurant.ops.auth.AuthService;
import com.restaurant.ops.auth.OutboxNotifier;
import com.restaurant.ops.auth.PasswordResetRepository;
import com.restaurant.ops.auth.RefreshToken;
import com.restaurant.ops.auth.RefreshTokenRepository;
import com.restaurant.ops.common.ApiException;
import com.restaurant.ops.common.Normalizer;
import com.restaurant.ops.config.AppProperties;
import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.EmployeeRepository;
import com.restaurant.ops.employee.EmployeeStatus;
import com.restaurant.ops.employee.Position;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.invitation.InvitationService;
import com.restaurant.ops.security.JwtService;
import com.restaurant.ops.security.SensitiveValueProtector;
import com.restaurant.ops.security.SecureTokenService;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  @Mock EmployeeRepository employees;
  @Mock RefreshTokenRepository refreshTokens;
  @Mock PasswordResetRepository resetRequests;
  @Mock InvitationService invitations;
  @Mock OutboxNotifier notifier;

  @Test
  void refreshRejectsDeactivatedAccountAndRevokesTokenFamily() {
    SecureTokenService secureTokens = new SecureTokenService();
    Employee employee = employee();
    employee.setStatus(EmployeeStatus.DEACTIVATED);
    RefreshToken refresh = new RefreshToken();
    refresh.setEmployee(employee);
    refresh.setFamilyId("family");
    refresh.setTokenHash(secureTokens.hash("refresh"));
    refresh.setExpiresAt(Instant.now().plusSeconds(3600));
    when(refreshTokens.findByTokenHash(secureTokens.hash("refresh"))).thenReturn(Optional.of(refresh));
    when(refreshTokens.findByFamilyIdAndRevokedAtIsNull("family")).thenReturn(java.util.List.of(refresh));

    AuthService service = new AuthService(employees, refreshTokens, resetRequests, invitations, new Normalizer(), new BCryptPasswordEncoder(), secureTokens, new JwtService(properties()), new SensitiveValueProtector(properties()), properties(), notifier);

    assertThatThrownBy(() -> service.refresh("refresh"))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_INVALID_REFRESH_TOKEN");
  }

  private AppProperties properties() {
    return new AppProperties(
        "https://app.example.com",
        new AppProperties.Cors("http://localhost:8081"),
        new AppProperties.Jwt("test", "01234567890123456789012345678901", 15),
        new AppProperties.ToastPin("toast-pin-test-key-32-bytes-long"),
        new AppProperties.ProfilePhoto("target/test-profile-photos", 1024),
        new AppProperties.Security(8, 24, 30, 30, 10),
        new AppProperties.Bootstrap(false, "", "", "", "", "", ""),
        new AppProperties.DevelopmentSeed(false),
        new AppProperties.OrderingSeed(false)
    );
  }

  private Employee employee() {
    Employee employee = new Employee();
    employee.setEnglishName("Test");
    employee.setPreferredName("Test");
    employee.setNormalizedEmail("test@example.com");
    employee.setNormalizedPhone("+12065550100");
    employee.setPasswordHash("hash");
    employee.setToastPinHash("pin-hash");
    employee.setToastPinCiphertext("1234");
    employee.setHomeStore(StoreCode.SEATTLE);
    employee.setEligibleStores(EnumSet.of(StoreCode.SEATTLE));
    employee.setPositions(EnumSet.of(Position.HOST));
    return employee;
  }
}
