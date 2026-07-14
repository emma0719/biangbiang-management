package com.restaurant.ops.auth;

import com.restaurant.ops.common.ApiException;
import com.restaurant.ops.common.Normalizer;
import com.restaurant.ops.config.AppProperties;
import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.EmployeeRepository;
import com.restaurant.ops.employee.EmployeeStatus;
import com.restaurant.ops.invitation.Invitation;
import com.restaurant.ops.invitation.InvitationService;
import com.restaurant.ops.security.JwtService;
import com.restaurant.ops.security.SensitiveValueProtector;
import com.restaurant.ops.security.SecureTokenService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final EmployeeRepository employees;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordResetRepository resetRequests;
  private final InvitationService invitations;
  private final Normalizer normalizer;
  private final PasswordEncoder passwordEncoder;
  private final SecureTokenService tokens;
  private final JwtService jwtService;
  private final SensitiveValueProtector valueProtector;
  private final AppProperties properties;
  private final OutboxNotifier notifier;

  public AuthService(EmployeeRepository employees, RefreshTokenRepository refreshTokens, PasswordResetRepository resetRequests, InvitationService invitations, Normalizer normalizer, PasswordEncoder passwordEncoder, SecureTokenService tokens, JwtService jwtService, SensitiveValueProtector valueProtector, AppProperties properties, OutboxNotifier notifier) {
    this.employees = employees;
    this.refreshTokens = refreshTokens;
    this.resetRequests = resetRequests;
    this.invitations = invitations;
    this.normalizer = normalizer;
    this.passwordEncoder = passwordEncoder;
    this.tokens = tokens;
    this.jwtService = jwtService;
    this.valueProtector = valueProtector;
    this.properties = properties;
    this.notifier = notifier;
  }

  @Transactional
  public AuthDtos.ActivationResponse activate(AuthDtos.ActivationRequest request) {
    validatePassword(request.password());
    Invitation invitation = invitations.lockValidInvitation(request.token());
    String email = normalizer.email(request.email());
    String phone = normalizer.phone(request.phone());
    String toastPinHash = tokens.hash(request.toastPin());
    if (employees.existsByNormalizedEmail(email)) throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_IN_USE");
    if (employees.existsByNormalizedPhone(phone)) throw new ApiException(HttpStatus.CONFLICT, "PHONE_ALREADY_IN_USE");
    if (employees.existsByToastPinHash(toastPinHash)) throw new ApiException(HttpStatus.CONFLICT, "TOAST_PIN_ALREADY_IN_USE");

    Employee employee = new Employee();
    employee.setEnglishName(request.englishName().trim());
    employee.setPreferredName(request.preferredName().trim());
    employee.setNormalizedEmail(email);
    employee.setNormalizedPhone(phone);
    employee.setPasswordHash(passwordEncoder.encode(request.password()));
    employee.setToastPinHash(toastPinHash);
    employee.setToastPinCiphertext(valueProtector.protect(request.toastPin()));
    employee.setHomeStore(request.homeStore());
    employee.setEligibleStores(request.eligibleStores());
    employee.setPositions(invitation.getPositions());
    employees.save(employee);
    invitation.markUsed(employee);
    return new AuthDtos.ActivationResponse(issueTokens(employee), employee.getPositions());
  }

  @Transactional
  public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request) {
    String identifier = request.identifier().trim();
    if (properties.developmentSeed().enabled() && identifier.equals("test_admin")) {
      identifier = "test_admin@dev.example.com";
    }
    Optional<Employee> found = identifier.contains("@")
        ? employees.findByNormalizedEmail(normalizer.email(identifier))
        : employees.findByNormalizedPhone(normalizer.phone(identifier));
    Employee employee = found.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS"));
    if (employee.getStatus() != EmployeeStatus.ACTIVE || !passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS");
    }
    employee.setLastLoginAt(Instant.now());
    return issueTokens(employee);
  }

  @Transactional
  public AuthDtos.TokenResponse refresh(String rawRefreshToken) {
    String hash = tokens.hash(rawRefreshToken);
    RefreshToken current = refreshTokens.findByTokenHash(hash)
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_REFRESH_TOKEN"));
    if (current.getRevokedAt() != null || current.getExpiresAt().isBefore(Instant.now()) || current.getEmployee().getStatus() != EmployeeStatus.ACTIVE) {
      current.markReused();
      revokeFamily(current.getFamilyId());
      throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_REFRESH_TOKEN");
    }
    current.revoke();
    AuthDtos.TokenResponse next = issueTokens(current.getEmployee(), current.getFamilyId());
    current.setReplacedByTokenHash(tokens.hash(next.refreshToken()));
    return next;
  }

  @Transactional
  public void logoutCurrent(String rawRefreshToken) {
    refreshTokens.findByTokenHash(tokens.hash(rawRefreshToken)).ifPresent(RefreshToken::revoke);
  }

  @Transactional
  public void logoutAll(Employee employee) {
    refreshTokens.findByEmployeeAndRevokedAtIsNull(employee).forEach(RefreshToken::revoke);
  }

  @Transactional
  public void forgotPassword(AuthDtos.ForgotPasswordRequest request) {
    Optional<Employee> found = request.identifier().contains("@")
        ? employees.findByNormalizedEmail(normalizer.email(request.identifier()))
        : employees.findByNormalizedPhone(normalizer.phone(request.identifier()));
    found.ifPresent(employee -> {
      resetRequests.findByEmployeeAndUsedAtIsNullAndRevokedAtIsNull(employee).forEach(PasswordResetRequest::revoke);
      String rawToken = tokens.newToken();
      PasswordResetRequest reset = new PasswordResetRequest();
      reset.setEmployee(employee);
      reset.setDeliveryMethod(request.channel());
      reset.setTokenHash(tokens.hash(rawToken));
      reset.setExpiresAt(Instant.now().plus(properties.security().resetTokenMinutes(), ChronoUnit.MINUTES));
      resetRequests.save(reset);
      notifier.sendPasswordReset(request.channel(), request.identifier(), rawToken);
    });
  }

  @Transactional
  public void resetPassword(AuthDtos.ResetPasswordRequest request) {
    validatePassword(request.password());
    PasswordResetRequest reset = resetRequests.findByTokenHash(tokens.hash(request.token()))
        .filter(item -> item.getUsedAt() == null && item.getRevokedAt() == null && item.getExpiresAt().isAfter(Instant.now()))
        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "RESET_TOKEN_INVALID_OR_EXPIRED"));
    Employee employee = reset.getEmployee();
    employee.setPasswordHash(passwordEncoder.encode(request.password()));
    reset.use();
    logoutAll(employee);
  }

  private AuthDtos.TokenResponse issueTokens(Employee employee) {
    return issueTokens(employee, UUID.randomUUID().toString());
  }

  private AuthDtos.TokenResponse issueTokens(Employee employee, String familyId) {
    String rawRefresh = tokens.newToken();
    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setEmployee(employee);
    refreshToken.setFamilyId(familyId);
    refreshToken.setTokenHash(tokens.hash(rawRefresh));
    refreshToken.setExpiresAt(Instant.now().plus(90, ChronoUnit.DAYS));
    refreshTokens.save(refreshToken);
    return new AuthDtos.TokenResponse(jwtService.createAccessToken(employee), rawRefresh);
  }

  private void revokeFamily(String familyId) {
    refreshTokens.findByFamilyIdAndRevokedAtIsNull(familyId).forEach(RefreshToken::revoke);
  }

  private void validatePassword(String password) {
    if (password.length() < properties.security().passwordMinLength()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_SHORT");
    }
  }
}
