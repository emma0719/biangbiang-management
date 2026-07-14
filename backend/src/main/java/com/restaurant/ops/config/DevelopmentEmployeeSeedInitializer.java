package com.restaurant.ops.config;

import com.restaurant.ops.common.Normalizer;
import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.EmployeeRepository;
import com.restaurant.ops.employee.EmployeeStatus;
import com.restaurant.ops.employee.Position;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.security.SecureTokenService;
import com.restaurant.ops.security.SensitiveValueProtector;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DevelopmentEmployeeSeedInitializer implements ApplicationRunner {
  private static final String EMAIL_DOMAIN = "dev.example.com";
  private static final String DEFAULT_PASSWORD = "password123";
  private static final List<SeedEmployee> SEED_EMPLOYEES = List.of(
      seedWithPassword("Test Admin", "test_admin", "2065550100", "1000", "Test1234!", Position.OWNER),
      seed("Alex", "alex", "2065550101", "1101", Position.MANAGER),
      seed("Mini", "mini", "2065550102", "1102", Position.MANAGER),
      seed("Annie", "annie", "2065550103", "1103", Position.SERVER_TWO_STAR, Position.SHIFT_LEADER),
      seed("Emma", "emma", "2065550104", "1104", Position.SERVER_TWO_STAR, Position.SHIFT_LEADER, Position.HOST, Position.BARTENDER, Position.FOOD_RUNNER),
      seed("Hannah", "hannah", "2065550105", "1105", Position.SERVER_TWO_STAR, Position.SHIFT_LEADER, Position.FOOD_RUNNER, Position.HOST),
      seed("David", "david", "2065550106", "1106", Position.SERVER_TWO_STAR, Position.SHIFT_LEADER, Position.HOST, Position.BARTENDER, Position.FOOD_RUNNER),
      seed("Amy", "amy", "2065550107", "1107", Position.SERVER_ONE_STAR, Position.BARTENDER),
      seed("Julie", "julie", "2065550108", "1108", Position.SERVER_ONE_STAR),
      seed("Tony", "tony", "2065550109", "1109", Position.SERVER_ONE_STAR, Position.BARTENDER),
      seed("Ethan", "ethan", "2065550110", "1110", Position.BARTENDER, Position.FOOD_RUNNER),
      seed("Mary", "mary", "2065550111", "1111", Position.HOST),
      seed("Sela", "sela", "2065550112", "1112", Position.FOOD_RUNNER),
      seed("Alyssa", "alyssa", "2065550113", "1113", Position.FOOD_RUNNER),
      seed("Bianca", "bianca", "2065550114", "1114", Position.FOOD_RUNNER),
      seed("Tommy", "tommy", "2065550115", "1115", Position.FOOD_RUNNER),
      seed("Cystal", "cystal", "2065550116", "1116", Position.FOOD_RUNNER),
      seed("Yumi", "yumi", "2065550117", "1117", Position.FINANCIAL_MANAGER),
      seed("Alison", "alison", "2065550118", "1118", Position.OWNER),
      seed("Sia", "sia", "2065550119", "1119", Position.OWNER)
  );

  private final AppProperties properties;
  private final EmployeeRepository employees;
  private final Normalizer normalizer;
  private final PasswordEncoder passwordEncoder;
  private final SecureTokenService tokens;
  private final SensitiveValueProtector valueProtector;

  public DevelopmentEmployeeSeedInitializer(AppProperties properties, EmployeeRepository employees, Normalizer normalizer, PasswordEncoder passwordEncoder, SecureTokenService tokens, SensitiveValueProtector valueProtector) {
    this.properties = properties;
    this.employees = employees;
    this.normalizer = normalizer;
    this.passwordEncoder = passwordEncoder;
    this.tokens = tokens;
    this.valueProtector = valueProtector;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!properties.developmentSeed().enabled()) return;
    removeStaleSeedEmployees();
    SEED_EMPLOYEES.forEach(this::upsert);
  }

  private void removeStaleSeedEmployees() {
    Set<String> currentSeedEmails = new HashSet<>(seedEmails().stream().map(normalizer::email).toList());
    employees.findAll().stream()
        .filter(employee -> employee.getNormalizedEmail().endsWith("@" + EMAIL_DOMAIN))
        .filter(employee -> !currentSeedEmails.contains(employee.getNormalizedEmail()))
        .forEach(employees::delete);
  }

  private void upsert(SeedEmployee seed) {
    String email = normalizer.email(seed.email());
    Employee employee = employees.findByNormalizedEmail(email).orElseGet(Employee::new);
    employee.setEnglishName(seed.name());
    employee.setPreferredName(seed.name());
    employee.setNormalizedEmail(email);
    employee.setNormalizedPhone(normalizer.phone(seed.phone()));
    if (employee.getPasswordHash() == null || employee.getPasswordHash().isBlank() || seed.email().startsWith("test_admin@")) {
      employee.setPasswordHash(passwordEncoder.encode(seed.password()));
    }
    employee.setToastPinHash(tokens.hash(seed.toastPin()));
    employee.setToastPinCiphertext(valueProtector.protect(seed.toastPin()));
    employee.setHomeStore(StoreCode.SEATTLE);
    employee.setEligibleStores(EnumSet.allOf(StoreCode.class));
    employee.setPositions(seed.positions());
    employee.setStatus(EmployeeStatus.ACTIVE);
    employees.save(employee);
  }

  public static List<String> seedEmails() {
    return SEED_EMPLOYEES.stream().map(SeedEmployee::email).toList();
  }

  private static SeedEmployee seed(String name, String emailPrefix, String phone, String toastPin, Position... positions) {
    return seedWithPassword(name, emailPrefix, phone, toastPin, DEFAULT_PASSWORD, positions);
  }

  private static SeedEmployee seedWithPassword(String name, String emailPrefix, String phone, String toastPin, String password, Position... positions) {
    return new SeedEmployee(name, emailPrefix + "@" + EMAIL_DOMAIN, phone, toastPin, password, Set.of(positions));
  }

  private record SeedEmployee(String name, String email, String phone, String toastPin, String password, Set<Position> positions) {}
}
