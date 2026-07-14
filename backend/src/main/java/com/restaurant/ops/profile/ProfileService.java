package com.restaurant.ops.profile;

import com.restaurant.ops.audit.ToastPinAuditLog;
import com.restaurant.ops.audit.ToastPinAuditLogRepository;
import com.restaurant.ops.auth.OutboxNotifier;
import com.restaurant.ops.common.ApiException;
import com.restaurant.ops.common.Normalizer;
import com.restaurant.ops.config.AppProperties;
import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.EmployeeRepository;
import com.restaurant.ops.security.AuthorizationService;
import com.restaurant.ops.security.SensitiveValueProtector;
import com.restaurant.ops.security.SecureTokenService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
  private final EmployeeRepository employees;
  private final PendingContactChangeRepository contactChanges;
  private final ToastPinAuditLogRepository toastAudits;
  private final Normalizer normalizer;
  private final SecureTokenService tokens;
  private final SensitiveValueProtector valueProtector;
  private final AuthorizationService authorization;
  private final AppProperties properties;
  private final OutboxNotifier notifier;

  public ProfileService(EmployeeRepository employees, PendingContactChangeRepository contactChanges, ToastPinAuditLogRepository toastAudits, Normalizer normalizer, SecureTokenService tokens, SensitiveValueProtector valueProtector, AuthorizationService authorization, AppProperties properties, OutboxNotifier notifier) {
    this.employees = employees;
    this.contactChanges = contactChanges;
    this.toastAudits = toastAudits;
    this.normalizer = normalizer;
    this.tokens = tokens;
    this.valueProtector = valueProtector;
    this.authorization = authorization;
    this.properties = properties;
    this.notifier = notifier;
  }

  public ProfileDtos.EmployeePrivateResponse privateResponse(Employee employee) {
    return new ProfileDtos.EmployeePrivateResponse(employee.getId(), employee.getEnglishName(), employee.getPreferredName(), employee.getDisplayName(), employee.getNormalizedEmail(), employee.getNormalizedPhone(), employee.getHomeStore(), employee.getEligibleStores(), employee.getPositions(), employee.getStatus(), valueProtector.reveal(employee.getToastPinCiphertext()), employee.getLastLoginAt(), employee.getCreatedAt());
  }

  public ProfileDtos.EmployeePublicResponse publicResponse(Employee employee) {
    return new ProfileDtos.EmployeePublicResponse(employee.getId(), employee.getDisplayName(), employee.getProfilePhotoKey(), employee.getPositions(), employee.getHomeStore());
  }

  @Transactional
  public Employee updateProfile(Employee actor, ProfileDtos.UpdateProfileRequest request) {
    Employee managedActor = managed(actor);
    managedActor.setEnglishName(request.englishName().trim());
    managedActor.setPreferredName(request.preferredName().trim());
    return managedActor;
  }

  @Transactional
  public Employee updateStores(Employee actor, ProfileDtos.UpdateStoresRequest request) {
    Employee managedActor = managed(actor);
    managedActor.setEligibleStores(request.eligibleStores());
    return managedActor;
  }

  @Transactional
  public Employee managerUpdateStores(Employee actor, Employee target, ProfileDtos.ManagerUpdateStoresRequest request) {
    if (!authorization.isManager(actor)) {
      throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_MANAGER_REQUIRED");
    }
    Employee managedTarget = managed(target);
    managedTarget.setHomeStore(request.homeStore());
    managedTarget.setEligibleStores(request.eligibleStores());
    return managedTarget;
  }

  @Transactional
  public void updateToastPin(Employee actor, Employee target, String toastPin) {
    Employee managedActor = managed(actor);
    Employee managedTarget = managed(target);
    if (!Objects.equals(managedActor.getId(), managedTarget.getId()) && !authorization.isManager(managedActor)) {
      throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_MANAGER_REQUIRED");
    }
    String hash = tokens.hash(toastPin);
    if (!hash.equals(managedTarget.getToastPinHash()) && employees.existsByToastPinHash(hash)) {
      throw new ApiException(HttpStatus.CONFLICT, "TOAST_PIN_ALREADY_IN_USE");
    }
    ToastPinAuditLog audit = new ToastPinAuditLog();
    audit.setActor(managedActor);
    audit.setEmployee(managedTarget);
    audit.setOldMaskedValue(tokens.mask(valueProtector.reveal(managedTarget.getToastPinCiphertext())));
    audit.setNewMaskedValue(tokens.mask(toastPin));
    toastAudits.save(audit);
    managedTarget.setToastPinHash(hash);
    managedTarget.setToastPinCiphertext(valueProtector.protect(toastPin));
  }

  @Transactional
  public void requestEmailChange(Employee employee, String email) {
    Employee managedEmployee = managed(employee);
    String normalized = normalizer.email(email);
    if (employees.existsByNormalizedEmail(normalized)) throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_IN_USE");
    requestChange(managedEmployee, PendingContactChange.Type.EMAIL, normalized, email);
  }

  @Transactional
  public void requestPhoneChange(Employee employee, String phone) {
    Employee managedEmployee = managed(employee);
    String normalized = normalizer.phone(phone);
    if (employees.existsByNormalizedPhone(normalized)) throw new ApiException(HttpStatus.CONFLICT, "PHONE_ALREADY_IN_USE");
    requestChange(managedEmployee, PendingContactChange.Type.PHONE, normalized, phone);
  }

  @Transactional
  public void verifyContactChange(Employee employee, String rawToken, PendingContactChange.Type type) {
    Employee managedEmployee = managed(employee);
    PendingContactChange change = contactChanges.findByTokenHash(tokens.hash(rawToken))
        .filter(item -> item.getEmployee() == managedEmployee || Objects.equals(item.getEmployee().getId(), managedEmployee.getId()))
        .filter(item -> item.getType() == type)
        .filter(item -> item.getVerifiedAt() == null && item.getRevokedAt() == null && item.getExpiresAt().isAfter(Instant.now()))
        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "CONTACT_CHANGE_INVALID_OR_EXPIRED"));
    if (type == PendingContactChange.Type.EMAIL) managedEmployee.setNormalizedEmail(change.getNormalizedValue());
    if (type == PendingContactChange.Type.PHONE) managedEmployee.setNormalizedPhone(change.getNormalizedValue());
    change.verify();
  }

  private void requestChange(Employee employee, PendingContactChange.Type type, String normalizedValue, String destination) {
    contactChanges.findByEmployeeAndTypeAndVerifiedAtIsNullAndRevokedAtIsNull(employee, type).forEach(PendingContactChange::revoke);
    String rawToken = tokens.newToken();
    PendingContactChange change = new PendingContactChange();
    change.setEmployee(employee);
    change.setType(type);
    change.setNormalizedValue(normalizedValue);
    change.setTokenHash(tokens.hash(rawToken));
    change.setExpiresAt(Instant.now().plus(properties.security().contactChangeMinutes(), ChronoUnit.MINUTES));
    contactChanges.save(change);
    notifier.sendContactVerification(type.name().toLowerCase(), destination, rawToken);
  }

  private Employee managed(Employee employee) {
    if (employee.getId() == null) {
      return employee;
    }
    return employees.findById(employee.getId())
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND"));
  }
}
