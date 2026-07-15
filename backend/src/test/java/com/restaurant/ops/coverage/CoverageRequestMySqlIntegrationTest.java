package com.restaurant.ops.coverage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.restaurant.ops.common.ApiException;
import com.restaurant.ops.coverage.CoverageDtos.CreateCoverageRequest;
import com.restaurant.ops.coverage.CoverageDtos.RejectCoverageRequest;
import com.restaurant.ops.coverage.CoverageEnums.CoverageStatus;
import com.restaurant.ops.coverage.CoverageEnums.CoverageType;
import com.restaurant.ops.coverage.CoverageEnums.ShiftType;
import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.EmployeeRepository;
import com.restaurant.ops.employee.EmployeeStatus;
import com.restaurant.ops.employee.Position;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.security.SecureTokenService;
import com.restaurant.ops.security.SensitiveValueProtector;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class CoverageRequestMySqlIntegrationTest {
  private static final AtomicInteger employeeSequence = new AtomicInteger(1000);

  @Container
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
      .withDatabaseName("restaurant_ops_coverage")
      .withUsername("test")
      .withPassword("test");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("app.jwt.secret", () -> "01234567890123456789012345678901");
    registry.add("app.toast-pin.encryption-key", () -> "toast-pin-test-key-32-bytes-long");
    registry.add("app.development-seed.enabled", () -> "false");
    registry.add("app.ordering-seed.enabled", () -> "false");
  }

  @Autowired CoverageService coverage;
  @Autowired CoverageRequestRepository requests;
  @Autowired CoverageAuditEventRepository auditEvents;
  @Autowired EmployeeRepository employees;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired SecureTokenService tokens;
  @Autowired SensitiveValueProtector valueProtector;

  @Test
  void activeEmployeeCreatesPublicRequestAndItAppearsInUnfilteredShiftPool() {
    Employee requester = employee("public-requester", Position.HOST, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    Employee otherPosition = employee("public-bartender", Position.BARTENDER, StoreCode.REDMOND, EmployeeStatus.ACTIVE);

    var created = coverage.create(requester, publicRequest(StoreCode.SEATTLE, Position.HOST, LocalDate.now().plusDays(2)));

    assertThat(created.status()).isEqualTo(CoverageStatus.OPEN);
    assertThat(created.coverageType()).isEqualTo(CoverageType.PUBLIC);
    assertThat(created.replacementAssigned()).isFalse();
    assertThat(coverage.pool(otherPosition)).extracting(response -> response.id()).contains(created.id());
  }

  @Test
  void directRequestRequiresActiveReplacementAndSkipsPool() {
    Employee requester = employee("direct-requester", Position.HOST, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    Employee replacement = employee("direct-replacement", Position.FOOD_RUNNER, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    Employee inactiveReplacement = employee("direct-inactive", Position.HOST, StoreCode.SEATTLE, EmployeeStatus.DEACTIVATED);

    assertThatThrownBy(() -> coverage.create(requester, directRequest(null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("COVERAGE_REPLACEMENT_REQUIRED");
    assertThatThrownBy(() -> coverage.create(requester, directRequest(inactiveReplacement.getId())))
        .isInstanceOf(ApiException.class)
        .hasMessage("COVERAGE_REPLACEMENT_ACTIVE_EMPLOYEE_REQUIRED");
    assertThatThrownBy(() -> coverage.create(requester, directRequest(requester.getId())))
        .isInstanceOf(ApiException.class)
        .hasMessage("COVERAGE_REPLACEMENT_CANNOT_BE_SELF");

    var created = coverage.create(requester, directRequest(replacement.getId()));

    assertThat(created.status()).isEqualTo(CoverageStatus.PENDING_APPROVAL);
    assertThat(created.replacementEmployee().id()).isEqualTo(replacement.getId());
    assertThat(coverage.pool(replacement)).extracting(response -> response.id()).doesNotContain(created.id());
  }

  @Test
  void publicClaimMovesToPendingAndPreventsSelfOrSecondClaim() {
    Employee requester = employee("claim-requester", Position.HOST, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    Employee first = employee("claim-first", Position.BARTENDER, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    Employee second = employee("claim-second", Position.FOOD_RUNNER, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    var created = coverage.create(requester, publicRequest(StoreCode.SEATTLE, Position.HOST, LocalDate.now().plusDays(3)));

    assertThatThrownBy(() -> coverage.claim(requester, created.id()))
        .isInstanceOf(ApiException.class)
        .hasMessage("COVERAGE_CANNOT_CLAIM_OWN_REQUEST");

    var claimed = coverage.claim(first, created.id());

    assertThat(claimed.status()).isEqualTo(CoverageStatus.PENDING_APPROVAL);
    assertThat(claimed.replacementEmployee().id()).isEqualTo(first.getId());
    assertThatThrownBy(() -> coverage.claim(second, created.id()))
        .isInstanceOf(ApiException.class)
        .hasMessage("COVERAGE_REQUEST_ALREADY_CLAIMED");
  }

  @Test
  void concurrentClaimAllowsOnlyOneReplacement() throws Exception {
    Employee requester = employee("concurrent-requester", Position.HOST, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    Employee first = employee("concurrent-first", Position.BARTENDER, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    Employee second = employee("concurrent-second", Position.FOOD_RUNNER, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    var created = coverage.create(requester, publicRequest(StoreCode.SEATTLE, Position.HOST, LocalDate.now().plusDays(4)));
    AtomicInteger successes = new AtomicInteger();
    CountDownLatch start = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);

    for (Employee claimant : List.of(first, second)) {
      executor.submit(() -> {
        await(start);
        try {
          coverage.claim(claimant, created.id());
          successes.incrementAndGet();
        } catch (ApiException ignored) {
        }
      });
    }

    start.countDown();
    executor.shutdown();
    assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

    CoverageRequest stored = requests.findById(created.id()).orElseThrow();
    assertThat(successes.get()).isEqualTo(1);
    assertThat(stored.getStatus()).isEqualTo(CoverageStatus.PENDING_APPROVAL);
    assertThat(stored.getReplacementEmployee().getId()).isIn(first.getId(), second.getId());
  }

  @Test
  void onlyBusinessPartnerCanApproveOrRejectPendingCoverage() {
    Employee requester = employee("approval-requester", Position.HOST, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    Employee replacement = employee("approval-replacement", Position.FOOD_RUNNER, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    Employee ordinary = employee("approval-ordinary", Position.BARTENDER, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    Employee manager = employee("approval-manager", Position.MANAGER, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    var pending = coverage.create(requester, directRequest(replacement.getId()));

    assertThatThrownBy(() -> coverage.approve(ordinary, pending.id()))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_BUSINESS_PARTNER_REQUIRED");

    var approved = coverage.approve(manager, pending.id());

    assertThat(approved.status()).isEqualTo(CoverageStatus.APPROVED);
    assertThat(approved.approvedBy().id()).isEqualTo(manager.getId());
    assertThat(approved.approvedAt()).isNotNull();

    var rejectedCandidate = coverage.create(requester, directRequest(replacement.getId()));
    assertThatThrownBy(() -> coverage.reject(ordinary, rejectedCandidate.id(), new RejectCoverageRequest("no")))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_BUSINESS_PARTNER_REQUIRED");

    var rejected = coverage.reject(manager, rejectedCandidate.id(), new RejectCoverageRequest("manager note"));

    assertThat(rejected.status()).isEqualTo(CoverageStatus.REJECTED);
    assertThat(rejected.reviewedBy().id()).isEqualTo(manager.getId());
    assertThat(rejected.managerNote()).isEqualTo("manager note");
  }

  @Test
  void requesterCanCancelOpenOrPendingButOthersAndFinalStatesCannot() {
    Employee requester = employee("cancel-requester", Position.HOST, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    Employee replacement = employee("cancel-replacement", Position.FOOD_RUNNER, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    Employee other = employee("cancel-other", Position.BARTENDER, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    Employee manager = employee("cancel-manager", Position.MANAGER, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    var open = coverage.create(requester, publicRequest(StoreCode.SEATTLE, Position.HOST, LocalDate.now().plusDays(5)));
    var pending = coverage.create(requester, directRequest(replacement.getId()));

    assertThatThrownBy(() -> coverage.cancel(other, open.id()))
        .isInstanceOf(ApiException.class)
        .hasMessage("COVERAGE_CANCEL_OWN_REQUEST_REQUIRED");
    assertThat(coverage.cancel(requester, open.id()).status()).isEqualTo(CoverageStatus.CANCELLED);
    assertThat(coverage.cancel(requester, pending.id()).status()).isEqualTo(CoverageStatus.CANCELLED);

    var approvedCandidate = coverage.create(requester, directRequest(replacement.getId()));
    coverage.approve(manager, approvedCandidate.id());

    assertThatThrownBy(() -> coverage.cancel(requester, approvedCandidate.id()))
        .isInstanceOf(ApiException.class)
        .hasMessage("COVERAGE_REQUEST_NOT_CANCELLABLE");
    assertThatThrownBy(() -> coverage.approve(manager, approvedCandidate.id()))
        .isInstanceOf(ApiException.class)
        .hasMessage("COVERAGE_REQUEST_NOT_PENDING_APPROVAL");
    assertThatThrownBy(() -> coverage.reject(manager, approvedCandidate.id(), new RejectCoverageRequest("too late")))
        .isInstanceOf(ApiException.class)
        .hasMessage("COVERAGE_REQUEST_NOT_PENDING_APPROVAL");
  }

  @Test
  void upcomingReturnsOnlyApprovedRequestsWithinRequestedFutureWindow() {
    Employee manager = employee("upcoming-manager", Position.MANAGER, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    Employee requester = employee("upcoming-requester", Position.HOST, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    Employee replacement = employee("upcoming-replacement", Position.FOOD_RUNNER, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    var inWindow = coverage.create(requester, directRequest(replacement.getId(), LocalDate.now().plusDays(3)));
    var outsideWindow = coverage.create(requester, directRequest(replacement.getId(), LocalDate.now().plusDays(20)));
    var pending = coverage.create(requester, directRequest(replacement.getId(), LocalDate.now().plusDays(4)));
    coverage.approve(manager, inWindow.id());
    coverage.approve(manager, outsideWindow.id());

    assertThat(coverage.upcoming(manager, 14))
        .extracting(response -> response.id())
        .contains(inWindow.id())
        .doesNotContain(outsideWindow.id(), pending.id());
  }

  @Test
  void inactiveEmployeesCannotCreateClaimOrQueryProtectedCoverage() {
    Employee inactive = employee("inactive-actor", Position.HOST, StoreCode.SEATTLE, EmployeeStatus.DEACTIVATED);
    Employee requester = employee("inactive-requester", Position.HOST, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    var open = coverage.create(requester, publicRequest(StoreCode.SEATTLE, Position.HOST, LocalDate.now().plusDays(6)));

    assertThatThrownBy(() -> coverage.create(inactive, publicRequest(StoreCode.SEATTLE, Position.HOST, LocalDate.now().plusDays(6))))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_ACTIVE_EMPLOYEE_REQUIRED");
    assertThatThrownBy(() -> coverage.claim(inactive, open.id()))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_ACTIVE_EMPLOYEE_REQUIRED");
    assertThatThrownBy(() -> coverage.pool(inactive))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_ACTIVE_EMPLOYEE_REQUIRED");
  }

  @Test
  void auditRecordsLifecycleEventsAndTimeValidationRejectsCrossMidnightByDefault() {
    Employee requester = employee("audit-requester", Position.HOST, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    Employee replacement = employee("audit-replacement", Position.FOOD_RUNNER, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);
    Employee manager = employee("audit-manager", Position.MANAGER, StoreCode.SEATTLE, EmployeeStatus.ACTIVE);

    assertThatThrownBy(() -> coverage.create(requester, new CreateCoverageRequest(
        StoreCode.SEATTLE,
        CoverageType.PUBLIC,
        LocalDate.now().plusDays(7),
        LocalTime.of(22, 0),
        LocalTime.of(2, 0),
        ShiftType.DINNER,
        Position.HOST,
        null,
        null
    ))).isInstanceOf(ApiException.class).hasMessage("COVERAGE_END_TIME_MUST_BE_AFTER_START_TIME");

    var created = coverage.create(requester, directRequest(replacement.getId()));
    coverage.reject(manager, created.id(), new RejectCoverageRequest("not enough info"));

    assertThat(auditEvents.findByRequestIdOrderByCreatedAtAsc(created.id()))
        .extracting(CoverageAuditEvent::getAction)
        .containsExactly("COVERAGE_REQUEST_CREATED", "COVERAGE_REQUEST_REJECTED");
    assertThat(auditEvents.findByRequestIdOrderByCreatedAtAsc(created.id()).get(1).getOldStatus()).isEqualTo(CoverageStatus.PENDING_APPROVAL);
    assertThat(auditEvents.findByRequestIdOrderByCreatedAtAsc(created.id()).get(1).getNewStatus()).isEqualTo(CoverageStatus.REJECTED);
  }

  private CreateCoverageRequest publicRequest(StoreCode store, Position position, LocalDate shiftDate) {
    return new CreateCoverageRequest(
        store,
        CoverageType.PUBLIC,
        shiftDate,
        LocalTime.of(17, 0),
        LocalTime.of(22, 0),
        ShiftType.DINNER,
        position,
        "need coverage",
        null
    );
  }

  private CreateCoverageRequest directRequest(Long replacementId) {
    return directRequest(replacementId, LocalDate.now().plusDays(2));
  }

  private CreateCoverageRequest directRequest(Long replacementId, LocalDate shiftDate) {
    return new CreateCoverageRequest(
        StoreCode.SEATTLE,
        CoverageType.DIRECT,
        shiftDate,
        LocalTime.of(10, 0),
        LocalTime.of(14, 0),
        ShiftType.LUNCH,
        Position.HOST,
        "covered offline",
        replacementId
    );
  }

  private Employee employee(String prefix, Position position, StoreCode homeStore, EmployeeStatus status) {
    int sequence = employeeSequence.incrementAndGet();
    String unique = prefix + "-" + UUID.randomUUID();
    String toastPin = String.format("%08d", sequence);
    Employee employee = new Employee();
    employee.setEnglishName(prefix);
    employee.setPreferredName(prefix);
    employee.setNormalizedEmail(unique + "@coverage.example.com");
    employee.setNormalizedPhone("+1206556" + String.format("%04d", sequence));
    employee.setPasswordHash(passwordEncoder.encode("password123"));
    employee.setToastPinHash(tokens.hash(toastPin));
    employee.setToastPinCiphertext(valueProtector.protect(toastPin));
    employee.setHomeStore(homeStore);
    employee.setEligibleStores(EnumSet.allOf(StoreCode.class));
    employee.setPositions(Set.of(position));
    employee.setStatus(status);
    return employees.saveAndFlush(employee);
  }

  private void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
