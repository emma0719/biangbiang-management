package com.restaurant.ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restaurant.ops.audit.ToastPinAuditLog;
import com.restaurant.ops.audit.ToastPinAuditLogRepository;
import com.restaurant.ops.auth.DevelopmentOutboxNotifier;
import com.restaurant.ops.auth.OutboxNotifier;
import com.restaurant.ops.common.Normalizer;
import com.restaurant.ops.config.AppProperties;
import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.EmployeeRepository;
import com.restaurant.ops.employee.Position;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.profile.PendingContactChangeRepository;
import com.restaurant.ops.profile.PendingContactChange;
import com.restaurant.ops.profile.ProfileDtos;
import com.restaurant.ops.profile.ProfileService;
import com.restaurant.ops.security.AuthorizationService;
import com.restaurant.ops.security.SensitiveValueProtector;
import com.restaurant.ops.security.SecureTokenService;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {
  @Mock EmployeeRepository employees;
  @Mock PendingContactChangeRepository contactChanges;
  @Mock ToastPinAuditLogRepository toastAudits;
  @Mock OutboxNotifier notifier;

  @Test
  void employeeStoreUpdateChangesOnlyEligibleStores() {
    ProfileService service = service();
    Employee employee = employee(Position.HOST, StoreCode.SEATTLE, "1111");

    service.updateStores(employee, new ProfileDtos.UpdateStoresRequest(EnumSet.of(StoreCode.REDMOND)));

    assertThat(employee.getHomeStore()).isEqualTo(StoreCode.SEATTLE);
    assertThat(employee.getEligibleStores()).containsExactly(StoreCode.REDMOND);
  }

  @Test
  void managerCanChangeHomeStoreAndEligibleStores() {
    ProfileService service = service();
    Employee manager = employee(Position.MANAGER, StoreCode.SEATTLE, "1111");
    Employee employee = employee(Position.HOST, StoreCode.SEATTLE, "2222");

    service.managerUpdateStores(manager, employee, new ProfileDtos.ManagerUpdateStoresRequest(StoreCode.REDMOND, EnumSet.of(StoreCode.SEATTLE, StoreCode.REDMOND)));

    assertThat(employee.getHomeStore()).isEqualTo(StoreCode.REDMOND);
    assertThat(employee.getEligibleStores()).containsExactlyInAnyOrder(StoreCode.SEATTLE, StoreCode.REDMOND);
  }

  @Test
  void publicEmployeeResponseNeverContainsToastPinButPrivateResponseDoes() {
    ProfileService service = service();
    Employee employee = employee(Position.HOST, StoreCode.SEATTLE, "1234");

    assertThat(service.publicResponse(employee).toString()).doesNotContain("1234");
    assertThat(service.privateResponse(employee).toastPin()).isEqualTo("1234");
  }

  @Test
  void toastPinUpdateWritesAuditRecord() {
    ProfileService service = service();
    Employee employee = employee(Position.HOST, StoreCode.SEATTLE, "1234");
    when(employees.existsByToastPinHash(any())).thenReturn(false);

    service.updateToastPin(employee, employee, "9876");

    ArgumentCaptor<ToastPinAuditLog> audit = ArgumentCaptor.forClass(ToastPinAuditLog.class);
    verify(toastAudits).save(audit.capture());
    assertThat(valueProtector().reveal(employee.getToastPinCiphertext())).isEqualTo("9876");
    verify(contactChanges, never()).save(any());
  }

  @Test
  void emailChangeRemainsPendingUntilVerification() {
    DevelopmentOutboxNotifier outbox = new DevelopmentOutboxNotifier();
    ProfileService service = service(outbox);
    Employee employee = employee(Position.HOST, StoreCode.SEATTLE, "1234");
    when(employees.existsByNormalizedEmail("new@example.com")).thenReturn(false);
    when(contactChanges.findByEmployeeAndTypeAndVerifiedAtIsNullAndRevokedAtIsNull(employee, PendingContactChange.Type.EMAIL)).thenReturn(List.of());

    service.requestEmailChange(employee, "New@Example.com");

    assertThat(employee.getNormalizedEmail()).isEqualTo("1234@example.com");
    ArgumentCaptor<PendingContactChange> change = ArgumentCaptor.forClass(PendingContactChange.class);
    verify(contactChanges).save(change.capture());
    String token = outbox.latest("contact", "email", "New@Example.com").orElseThrow().token();
    when(contactChanges.findByTokenHash(new SecureTokenService().hash(token))).thenReturn(Optional.of(change.getValue()));

    service.verifyContactChange(employee, token, PendingContactChange.Type.EMAIL);

    assertThat(employee.getNormalizedEmail()).isEqualTo("new@example.com");
  }

  @Test
  void phoneChangeRemainsPendingUntilVerificationAndExpiredTokenFails() {
    DevelopmentOutboxNotifier outbox = new DevelopmentOutboxNotifier();
    ProfileService service = service(outbox);
    Employee employee = employee(Position.HOST, StoreCode.SEATTLE, "1234");
    when(employees.existsByNormalizedPhone("+12065550199")).thenReturn(false);
    when(contactChanges.findByEmployeeAndTypeAndVerifiedAtIsNullAndRevokedAtIsNull(employee, PendingContactChange.Type.PHONE)).thenReturn(List.of());

    service.requestPhoneChange(employee, "206-555-0199");

    assertThat(employee.getNormalizedPhone()).isEqualTo("+12065551234");
    ArgumentCaptor<PendingContactChange> change = ArgumentCaptor.forClass(PendingContactChange.class);
    verify(contactChanges).save(change.capture());
    change.getValue().setExpiresAt(Instant.now().minusSeconds(1));
    String token = outbox.latest("contact", "phone", "206-555-0199").orElseThrow().token();
    when(contactChanges.findByTokenHash(new SecureTokenService().hash(token))).thenReturn(Optional.of(change.getValue()));

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.verifyContactChange(employee, token, PendingContactChange.Type.PHONE))
        .isInstanceOf(com.restaurant.ops.common.ApiException.class)
        .hasMessage("CONTACT_CHANGE_INVALID_OR_EXPIRED");
    assertThat(employee.getNormalizedPhone()).isEqualTo("+12065551234");
  }

  @Test
  void newerVerificationRevokesOlderPendingVerification() {
    ProfileService service = service(new DevelopmentOutboxNotifier());
    Employee employee = employee(Position.HOST, StoreCode.SEATTLE, "1234");
    PendingContactChange old = new PendingContactChange();
    old.setEmployee(employee);
    old.setType(PendingContactChange.Type.EMAIL);
    old.setNormalizedValue("old@example.com");
    old.setTokenHash("old");
    old.setExpiresAt(Instant.now().plusSeconds(60));
    when(employees.existsByNormalizedEmail("new@example.com")).thenReturn(false);
    when(contactChanges.findByEmployeeAndTypeAndVerifiedAtIsNullAndRevokedAtIsNull(employee, PendingContactChange.Type.EMAIL)).thenReturn(List.of(old));

    service.requestEmailChange(employee, "new@example.com");

    assertThat(old.getRevokedAt()).isNotNull();
  }

  private ProfileService service() {
    AppProperties properties = properties();
    return new ProfileService(employees, contactChanges, toastAudits, new Normalizer(), new SecureTokenService(), new SensitiveValueProtector(properties), new AuthorizationService(), properties, notifier);
  }

  private ProfileService service(OutboxNotifier outbox) {
    AppProperties properties = properties();
    return new ProfileService(employees, contactChanges, toastAudits, new Normalizer(), new SecureTokenService(), new SensitiveValueProtector(properties), new AuthorizationService(), properties, outbox);
  }

  private AppProperties properties() {
    AppProperties properties = new AppProperties(
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
    return properties;
  }

  private SensitiveValueProtector valueProtector() {
    return new SensitiveValueProtector(properties());
  }

  private Employee employee(Position position, StoreCode homeStore, String toastPin) {
    SecureTokenService tokens = new SecureTokenService();
    Employee employee = new Employee();
    employee.setEnglishName("Test");
    employee.setPreferredName("Test");
    employee.setNormalizedEmail(toastPin + "@example.com");
    employee.setNormalizedPhone("+1206555" + toastPin);
    employee.setPasswordHash("hash");
    employee.setToastPinHash(tokens.hash(toastPin));
    employee.setToastPinCiphertext(valueProtector().protect(toastPin));
    employee.setHomeStore(homeStore);
    employee.setEligibleStores(EnumSet.of(homeStore));
    employee.setPositions(EnumSet.of(position));
    return employee;
  }
}
