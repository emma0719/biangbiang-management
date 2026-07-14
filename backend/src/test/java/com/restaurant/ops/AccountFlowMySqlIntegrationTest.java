package com.restaurant.ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.restaurant.ops.auth.AuthDtos;
import com.restaurant.ops.auth.AuthService;
import com.restaurant.ops.auth.RefreshTokenRepository;
import com.restaurant.ops.common.ApiException;
import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.EmployeeRepository;
import com.restaurant.ops.employee.EmployeeStatus;
import com.restaurant.ops.employee.Position;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.invitation.Invitation;
import com.restaurant.ops.invitation.InvitationRepository;
import com.restaurant.ops.invitation.InvitationService;
import com.restaurant.ops.invitation.InvitationStatus;
import com.restaurant.ops.security.SecureTokenService;
import com.restaurant.ops.security.SensitiveValueProtector;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class AccountFlowMySqlIntegrationTest {
  @Container
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
      .withDatabaseName("restaurant_ops_test")
      .withUsername("test")
      .withPassword("test");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("app.jwt.secret", () -> "01234567890123456789012345678901");
    registry.add("app.toast-pin.encryption-key", () -> "toast-pin-test-key-32-bytes-long");
    registry.add("app.public-base-url", () -> "https://app.example.com");
  }

  @Autowired InvitationService invitationService;
  @Autowired InvitationRepository invitations;
  @Autowired AuthService authService;
  @Autowired EmployeeRepository employees;
  @Autowired RefreshTokenRepository refreshTokens;
  @Autowired SecureTokenService secureTokens;
  @Autowired SensitiveValueProtector valueProtector;

  @Test
  void invitationTokenIsHashedExpiresInTwentyFourHoursAndCanBeUsedOnce() {
    Employee manager = manager();
    InvitationService.CreatedInvitation created = invitationService.create(manager, Set.of(Position.HOST));
    String rawToken = tokenFrom(created.activationLink());

    assertThat(created.invitation().getTokenHash()).isEqualTo(secureTokens.hash(rawToken));
    assertThat(created.invitation().getTokenHash()).doesNotContain(rawToken);
    assertThat(Duration.between(Instant.now(), created.invitation().getExpiresAt()).toHours()).isEqualTo(23);

    authService.activate(activation(rawToken, "once"));

    Invitation used = invitations.findById(created.invitation().getId()).orElseThrow();
    assertThat(used.getStatus()).isEqualTo(InvitationStatus.USED);
    assertThat(used.getUsedAt()).isNotNull();
    assertThatThrownBy(() -> authService.activate(activation(rawToken, "twice")))
        .isInstanceOf(ApiException.class)
        .hasMessage("INVITATION_INVALID_OR_EXPIRED");
  }

  @Test
  void regeneratedInvitationInvalidatesPriorTokenAndRevokedInvitationCannotActivate() {
    Employee manager = manager();
    InvitationService.CreatedInvitation created = invitationService.create(manager, Set.of(Position.HOST));
    String oldToken = tokenFrom(created.activationLink());
    InvitationService.CreatedInvitation regenerated = invitationService.regenerate(manager, created.invitation().getId());
    String newToken = tokenFrom(regenerated.activationLink());

    assertThat(invitationService.validate(oldToken).valid()).isFalse();
    assertThat(invitationService.validate(newToken).valid()).isTrue();

    invitationService.revoke(manager, regenerated.invitation().getId());
    assertThatThrownBy(() -> authService.activate(activation(newToken, "revoked")))
        .isInstanceOf(ApiException.class)
        .hasMessage("INVITATION_INVALID_OR_EXPIRED");
  }

  @Test
  void concurrentActivationAttemptsCreateOnlyOneAccount() throws Exception {
    Employee manager = manager();
    String rawToken = tokenFrom(invitationService.create(manager, Set.of(Position.HOST)).activationLink());
    AtomicInteger successes = new AtomicInteger();
    CountDownLatch start = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);

    for (String suffix : java.util.List.of("a", "b")) {
      executor.submit(() -> {
        await(start);
        try {
          authService.activate(activation(rawToken, "concurrent-" + suffix));
          successes.incrementAndGet();
        } catch (ApiException ignored) {
        }
      });
    }

    start.countDown();
    executor.shutdown();
    assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    assertThat(successes.get()).isEqualTo(1);
  }

  @Test
  void databaseEnforcesUniqueNormalizedEmailPhoneAndToastPin() {
    Employee first = employee("unique-a", Position.HOST, StoreCode.SEATTLE, "9001");
    employees.saveAndFlush(first);

    assertThatThrownBy(() -> employees.saveAndFlush(employeeWith("unique-a@example.com", "+12065559002", "9002")))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> employees.saveAndFlush(employeeWith("unique-b@example.com", "+12065559001", "9003")))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> employees.saveAndFlush(employeeWith("unique-c@example.com", "+12065559004", "9001")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void logoutCurrentLogoutAllAndDeactivationRevokeExpectedRefreshTokens() {
    Employee employee = employees.save(employee("session", Position.HOST, StoreCode.SEATTLE, "9101"));
    AuthDtos.TokenResponse first = authService.login(new AuthDtos.LoginRequest("session@example.com", "password123"));
    AuthDtos.TokenResponse second = authService.login(new AuthDtos.LoginRequest("+12065559101", "password123"));

    authService.logoutCurrent(first.refreshToken());
    assertThat(refreshTokens.findByTokenHash(secureTokens.hash(first.refreshToken())).orElseThrow().getRevokedAt()).isNotNull();
    assertThat(refreshTokens.findByTokenHash(secureTokens.hash(second.refreshToken())).orElseThrow().getRevokedAt()).isNull();

    authService.logoutAll(employee);
    assertThat(refreshTokens.findByTokenHash(secureTokens.hash(second.refreshToken())).orElseThrow().getRevokedAt()).isNotNull();

    AuthDtos.TokenResponse third = authService.login(new AuthDtos.LoginRequest("session@example.com", "password123"));
    employee.setStatus(EmployeeStatus.DEACTIVATED);
    employees.saveAndFlush(employee);
    authService.logoutAll(employee);
    assertThat(refreshTokens.findByTokenHash(secureTokens.hash(third.refreshToken())).orElseThrow().getRevokedAt()).isNotNull();
    assertThatThrownBy(() -> authService.login(new AuthDtos.LoginRequest("session@example.com", "password123")))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_INVALID_CREDENTIALS");
  }

  private Employee manager() {
    return employees.save(employee(UUID.randomUUID().toString(), Position.MANAGER, StoreCode.SEATTLE, UUID.randomUUID().toString().substring(0, 8)));
  }

  private Employee employee(String suffix, Position position, StoreCode homeStore, String toastPin) {
    return employeeWith(suffix + "@example.com", "+1206555" + toastPin.substring(0, Math.min(4, toastPin.length())), toastPin, position, homeStore);
  }

  private Employee employeeWith(String email, String phone, String toastPin) {
    return employeeWith(email, phone, toastPin, Position.HOST, StoreCode.SEATTLE);
  }

  private Employee employeeWith(String email, String phone, String toastPin, Position position, StoreCode homeStore) {
    Employee employee = new Employee();
    employee.setEnglishName("Test");
    employee.setPreferredName("Test");
    employee.setNormalizedEmail(email);
    employee.setNormalizedPhone(phone);
    employee.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("password123"));
    employee.setToastPinHash(secureTokens.hash(toastPin));
    employee.setToastPinCiphertext(valueProtector.protect(toastPin));
    employee.setHomeStore(homeStore);
    employee.setEligibleStores(EnumSet.of(homeStore));
    employee.setPositions(EnumSet.of(position));
    return employee;
  }

  private AuthDtos.ActivationRequest activation(String rawToken, String suffix) {
    return new AuthDtos.ActivationRequest(
        rawToken,
        "English " + suffix,
        "Preferred " + suffix,
        suffix + "@activation.example.com",
        "206555" + Math.abs(suffix.hashCode() % 10000),
        "password123",
        "7" + Math.abs(suffix.hashCode() % 100000),
        StoreCode.SEATTLE,
        Set.of(StoreCode.SEATTLE)
    );
  }

  private String tokenFrom(String activationLink) {
    return activationLink.substring(activationLink.indexOf("token=") + "token=".length());
  }

  private void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
