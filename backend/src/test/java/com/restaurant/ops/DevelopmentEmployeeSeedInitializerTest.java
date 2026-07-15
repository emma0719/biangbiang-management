package com.restaurant.ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.restaurant.ops.common.Normalizer;
import com.restaurant.ops.config.AppProperties;
import com.restaurant.ops.config.DevelopmentEmployeeSeedInitializer;
import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.EmployeeRepository;
import com.restaurant.ops.employee.EmployeeStatus;
import com.restaurant.ops.employee.Position;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.security.SecureTokenService;
import com.restaurant.ops.security.SensitiveValueProtector;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.env.MockEnvironment;

class DevelopmentEmployeeSeedInitializerTest {
  @Test
  void developmentSeedContainsRevisedNineteenEmployeesPlusLocalhostAdmin() {
    EmployeeRepository employees = org.mockito.Mockito.mock(EmployeeRepository.class);
    when(employees.findAll()).thenReturn(List.of());
    when(employees.findByNormalizedEmail(anyString())).thenReturn(Optional.empty());
    when(employees.save(org.mockito.Mockito.any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
    DevelopmentEmployeeSeedInitializer initializer = new DevelopmentEmployeeSeedInitializer(
        properties(true),
        employees,
        new Normalizer(),
        new BCryptPasswordEncoder(),
        new SecureTokenService(),
        new SensitiveValueProtector(properties(true)),
        environment("dev")
    );

    initializer.run(null);

    ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);
    org.mockito.Mockito.verify(employees, org.mockito.Mockito.times(20)).save(saved.capture());
    List<Employee> seeded = saved.getAllValues();

    assertThat(seeded).hasSize(20);
    assertThat(seeded).allSatisfy(employee -> assertThat(employee.getStatus().name()).isEqualTo("ACTIVE"));
    assertThat(seeded).extracting(Employee::getNormalizedEmail).doesNotHaveDuplicates();
    assertThat(seeded).extracting(Employee::getNormalizedPhone).doesNotHaveDuplicates();
    assertThat(seeded).extracting(Employee::getToastPinHash).doesNotHaveDuplicates();
    assertThat(seeded).filteredOn(employee -> employee.getEnglishName().equals("Tommy")).hasSize(1);
    assertThat(seeded).filteredOn(employee -> employee.getEnglishName().equals("Cystal")).hasSize(1);
    assertThat(find(seeded, "Test Admin").getPositions()).containsExactly(Position.OWNER);
    assertThat(find(seeded, "Emma").getPositions()).containsExactlyInAnyOrder(Position.SERVER_TWO_STAR, Position.SHIFT_LEADER, Position.HOST, Position.BARTENDER, Position.FOOD_RUNNER);
    assertThat(find(seeded, "Yumi").getPositions()).containsExactly(Position.FINANCIAL_MANAGER);
    assertThat(find(seeded, "Alison").getPositions()).containsExactly(Position.OWNER);
    assertThat(find(seeded, "Sia").getPositions()).containsExactly(Position.OWNER);
  }

  @Test
  void developmentSeedUpsertsByEmailSoRestartsDoNotCreateDuplicateRecords() {
    EmployeeRepository employees = org.mockito.Mockito.mock(EmployeeRepository.class);
    List<Employee> existing = new ArrayList<>();
    when(employees.findAll()).thenReturn(existing);
    when(employees.findByNormalizedEmail(anyString())).thenAnswer(invocation -> {
      String email = invocation.getArgument(0);
      return existing.stream().filter(employee -> employee.getNormalizedEmail().equals(email)).findFirst();
    });
    when(employees.save(org.mockito.Mockito.any(Employee.class))).thenAnswer(invocation -> {
      Employee employee = invocation.getArgument(0);
      if (existing.stream().noneMatch(saved -> saved.getNormalizedEmail().equals(employee.getNormalizedEmail()))) {
        existing.add(employee);
      }
      return employee;
    });
    DevelopmentEmployeeSeedInitializer initializer = new DevelopmentEmployeeSeedInitializer(
        properties(true),
        employees,
        new Normalizer(),
        new BCryptPasswordEncoder(),
        new SecureTokenService(),
        new SensitiveValueProtector(properties(true)),
        environment("dev")
    );

    initializer.run(null);
    initializer.run(null);

    assertThat(existing).hasSize(20);
    assertThat(existing).filteredOn(employee -> employee.getEnglishName().equals("Tommy")).hasSize(1);
  }

  @Test
  void developmentSeedRepairsExistingAlexCredentialsAndProfile() {
    EmployeeRepository employees = org.mockito.Mockito.mock(EmployeeRepository.class);
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    List<Employee> existing = new ArrayList<>();
    Employee alex = new Employee();
    alex.setEnglishName("Old Alex");
    alex.setPreferredName("Old Alex");
    alex.setNormalizedEmail("alex@dev.example.com");
    alex.setNormalizedPhone("+12065559999");
    alex.setPasswordHash(passwordEncoder.encode("wrong-password"));
    alex.setToastPinHash("old-toast-pin-hash");
    alex.setToastPinCiphertext("old-toast-pin-ciphertext");
    alex.setHomeStore(StoreCode.REDMOND);
    alex.setEligibleStores(Set.of(StoreCode.REDMOND));
    alex.setPositions(Set.of(Position.HOST));
    alex.setStatus(EmployeeStatus.DEACTIVATED);
    existing.add(alex);
    when(employees.findAll()).thenReturn(existing);
    when(employees.findByNormalizedEmail(anyString())).thenAnswer(invocation -> {
      String email = invocation.getArgument(0);
      return existing.stream().filter(employee -> employee.getNormalizedEmail().equals(email)).findFirst();
    });
    when(employees.save(org.mockito.Mockito.any(Employee.class))).thenAnswer(invocation -> {
      Employee employee = invocation.getArgument(0);
      if (existing.stream().noneMatch(saved -> saved.getNormalizedEmail().equals(employee.getNormalizedEmail()))) {
        existing.add(employee);
      }
      return employee;
    });
    DevelopmentEmployeeSeedInitializer initializer = new DevelopmentEmployeeSeedInitializer(
        properties(true),
        employees,
        new Normalizer(),
        passwordEncoder,
        new SecureTokenService(),
        new SensitiveValueProtector(properties(true)),
        environment("dev")
    );

    initializer.run(null);

    Employee repaired = existing.stream()
        .filter(employee -> employee.getNormalizedEmail().equals("alex@dev.example.com"))
        .findFirst()
        .orElseThrow();
    assertThat(passwordEncoder.matches("password123", repaired.getPasswordHash())).isTrue();
    assertThat(repaired.getEnglishName()).isEqualTo("Alex");
    assertThat(repaired.getNormalizedPhone()).isEqualTo("+12065550101");
    assertThat(repaired.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
    assertThat(repaired.getPositions()).containsExactly(Position.MANAGER);
    assertThat(repaired.getHomeStore()).isEqualTo(StoreCode.SEATTLE);
    assertThat(repaired.getEligibleStores()).containsExactlyInAnyOrder(EnumSet.allOf(StoreCode.class).toArray(StoreCode[]::new));
  }

  @Test
  void developmentSeedDoesNotRunOutsideDevProfile() {
    EmployeeRepository employees = org.mockito.Mockito.mock(EmployeeRepository.class);
    DevelopmentEmployeeSeedInitializer initializer = new DevelopmentEmployeeSeedInitializer(
        properties(true),
        employees,
        new Normalizer(),
        new BCryptPasswordEncoder(),
        new SecureTokenService(),
        new SensitiveValueProtector(properties(true)),
        environment("prod")
    );

    initializer.run(null);

    org.mockito.Mockito.verify(employees, org.mockito.Mockito.never()).save(org.mockito.Mockito.any(Employee.class));
  }

  @Test
  void developmentSeedDoesNotRunWhenDisabled() {
    EmployeeRepository employees = org.mockito.Mockito.mock(EmployeeRepository.class);
    DevelopmentEmployeeSeedInitializer initializer = new DevelopmentEmployeeSeedInitializer(
        properties(false),
        employees,
        new Normalizer(),
        new BCryptPasswordEncoder(),
        new SecureTokenService(),
        new SensitiveValueProtector(properties(false)),
        environment("dev")
    );

    initializer.run(null);

    org.mockito.Mockito.verify(employees, org.mockito.Mockito.never()).save(org.mockito.Mockito.any(Employee.class));
  }

  private Employee find(List<Employee> employees, String name) {
    return employees.stream().filter(employee -> employee.getEnglishName().equals(name)).findFirst().orElseThrow();
  }

  private AppProperties properties(boolean developmentSeedEnabled) {
    return new AppProperties(
        "https://app.example.com",
        new AppProperties.Cors("http://localhost:8081"),
        new AppProperties.Jwt("test", "01234567890123456789012345678901", 15),
        new AppProperties.ToastPin("toast-pin-test-key-32-bytes-long"),
        new AppProperties.ProfilePhoto("target/test-profile-photos", 1024),
        new AppProperties.Security(8, 24, 30, 30, 10),
        new AppProperties.Bootstrap(false, "", "", "", "", "", ""),
        new AppProperties.DevelopmentSeed(developmentSeedEnabled),
        new AppProperties.OrderingSeed(false)
    );
  }

  private MockEnvironment environment(String profile) {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles(profile);
    return environment;
  }
}
