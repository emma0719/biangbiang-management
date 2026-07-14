package com.restaurant.ops;

import static org.assertj.core.api.Assertions.assertThat;

import com.restaurant.ops.config.DevelopmentEmployeeSeedInitializer;
import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.EmployeeRepository;
import com.restaurant.ops.employee.EmployeeStatus;
import com.restaurant.ops.employee.Position;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.security.AuthorizationService;
import com.restaurant.ops.security.SecureTokenService;
import com.restaurant.ops.security.SensitiveValueProtector;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class DevelopmentEmployeeSeedMySqlIntegrationTest {
  @Container
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
      .withDatabaseName("restaurant_ops_seed_test")
      .withUsername("test")
      .withPassword("test");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("app.jwt.secret", () -> "01234567890123456789012345678901");
    registry.add("app.toast-pin.encryption-key", () -> "toast-pin-test-key-32-bytes-long");
    registry.add("app.development-seed.enabled", () -> "true");
  }

  @Autowired EmployeeRepository employees;
  @Autowired DevelopmentEmployeeSeedInitializer seedInitializer;
  @Autowired AuthorizationService authorization;
  @Autowired SecureTokenService tokens;
  @Autowired SensitiveValueProtector valueProtector;
  @Autowired PasswordEncoder passwordEncoder;

  @Test
  void developmentSeedCreatesRevisedNineteenEmployeesAndIsIdempotent() throws Exception {
    seedInitializer.run(new DefaultApplicationArguments());

    assertSeededEmployees();
    long countAfterFirstRun = employees.count();

    seedInitializer.run(new DefaultApplicationArguments());

    assertSeededEmployees();
    assertThat(employees.count()).isEqualTo(countAfterFirstRun);
  }

  @Test
  void staleSeedEmployeesAreRemovedButUnrelatedEmployeesArePreserved() throws Exception {
    Employee stale = employee("Old Seed", "old-seed@dev.example.com", "+1206550198", "9198", Position.HOST);
    Employee unrelated = employee("Outside", "outside@example.com", "+1206550199", "9199", Position.HOST);
    employees.save(stale);
    employees.save(unrelated);

    seedInitializer.run(new DefaultApplicationArguments());

    assertThat(employees.findByNormalizedEmail("old-seed@dev.example.com")).isEmpty();
    assertThat(employees.findByNormalizedEmail("outside@example.com")).isPresent();
    assertSeededEmployees();
  }

  private void assertSeededEmployees() {
    var seeded = DevelopmentEmployeeSeedInitializer.seedEmails().stream()
        .map(email -> employees.findByNormalizedEmail(email).orElseThrow())
        .toList();

    assertThat(seeded).hasSize(20);
    assertThat(seeded).extracting(Employee::getNormalizedEmail).doesNotHaveDuplicates();
    assertThat(seeded).extracting(Employee::getNormalizedPhone).doesNotHaveDuplicates();
    assertThat(seeded).extracting(Employee::getToastPinHash).doesNotHaveDuplicates();
    assertThat(seeded).allSatisfy(employee -> {
      assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
      assertThat(employee.getNormalizedEmail()).endsWith("@dev.example.com");
      assertThat(employee.getNormalizedPhone()).startsWith("+120655501");
    });
    assertThat(seeded).filteredOn(employee -> employee.getEnglishName().equals("Tommy")).hasSize(1);
    assertThat(seeded).filteredOn(employee -> employee.getEnglishName().equals("Cystal")).hasSize(1);

    assertThat(employee("Emma").getPositions()).containsExactlyInAnyOrder(Position.SERVER_TWO_STAR, Position.SHIFT_LEADER, Position.HOST, Position.BARTENDER, Position.FOOD_RUNNER);
    assertThat(employee("Annie").getPositions()).containsExactlyInAnyOrder(Position.SERVER_TWO_STAR, Position.SHIFT_LEADER);
    assertThat(employee("David").getPositions()).containsExactlyInAnyOrder(Position.SERVER_TWO_STAR, Position.SHIFT_LEADER, Position.HOST, Position.BARTENDER, Position.FOOD_RUNNER);

    Employee alex = employee("Alex");
    Employee mini = employee("Mini");
    Employee yumi = employee("Yumi");
    Employee alison = employee("Alison");
    Employee sia = employee("Sia");
    Employee testAdmin = employee("Test Admin");
    Employee shiftLeader = employee("Annie");
    Employee host = employee("Mary");

    assertThat(alex.getPositions()).containsExactly(Position.MANAGER);
    assertThat(mini.getPositions()).containsExactly(Position.MANAGER);
    assertThat(yumi.getPositions()).containsExactly(Position.FINANCIAL_MANAGER);
    assertThat(alison.getPositions()).containsExactly(Position.OWNER);
    assertThat(sia.getPositions()).containsExactly(Position.OWNER);

    assertThat(testAdmin.getPositions()).containsExactly(Position.OWNER);

    for (Employee businessPartner : java.util.List.of(alex, mini, yumi, alison, sia, testAdmin)) {
      assertThat(businessPartner.getPositions()).allSatisfy(position -> assertThat(position.permissionGroup()).isEqualTo(Position.PermissionGroup.BUSINESS_PARTNER));
      assertThat(authorization.isBusinessPartner(businessPartner)).isTrue();
      assertThat(authorization.canManageEmployee(businessPartner, host)).isTrue();
      assertThat(authorization.qualifyingWorkingPositions(businessPartner)).containsExactlyInAnyOrder(Position.restaurantOperatingPositions().toArray(Position[]::new));
    }

    assertThat(shiftLeader.getPositions()).contains(Position.SHIFT_LEADER);
    assertThat(shiftLeader.getPositions()).allSatisfy(position -> assertThat(position.permissionGroup()).isNotEqualTo(Position.PermissionGroup.BUSINESS_PARTNER));
    assertThat(authorization.isBusinessPartner(shiftLeader)).isFalse();
    assertThat(authorization.canManageEmployee(shiftLeader, host)).isFalse();
  }

  private Employee employee(String name) {
    return employees.findAll().stream()
        .filter(employee -> employee.getEnglishName().equals(name))
        .findFirst()
        .orElseThrow();
  }

  private Employee employee(String name, String email, String phone, String toastPin, Position position) {
    Employee employee = new Employee();
    employee.setEnglishName(name);
    employee.setPreferredName(name);
    employee.setNormalizedEmail(email);
    employee.setNormalizedPhone(phone);
    employee.setPasswordHash(passwordEncoder.encode("password123"));
    employee.setToastPinHash(tokens.hash(toastPin));
    employee.setToastPinCiphertext(valueProtector.protect(toastPin));
    employee.setHomeStore(StoreCode.SEATTLE);
    employee.setEligibleStores(EnumSet.allOf(StoreCode.class));
    employee.setPositions(Set.of(position));
    return employee;
  }
}
