package com.restaurant.ops.config;

import com.restaurant.ops.common.Normalizer;
import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.EmployeeRepository;
import com.restaurant.ops.employee.Position;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.security.SecureTokenService;
import com.restaurant.ops.security.SensitiveValueProtector;
import java.util.EnumSet;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BootstrapManagerInitializer implements ApplicationRunner {
  private final AppProperties properties;
  private final EmployeeRepository employees;
  private final Normalizer normalizer;
  private final PasswordEncoder passwordEncoder;
  private final SecureTokenService tokens;
  private final SensitiveValueProtector valueProtector;

  public BootstrapManagerInitializer(AppProperties properties, EmployeeRepository employees, Normalizer normalizer, PasswordEncoder passwordEncoder, SecureTokenService tokens, SensitiveValueProtector valueProtector) {
    this.properties = properties;
    this.employees = employees;
    this.normalizer = normalizer;
    this.passwordEncoder = passwordEncoder;
    this.tokens = tokens;
    this.valueProtector = valueProtector;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!properties.bootstrap().enabled()) return;
    String email = normalizer.email(properties.bootstrap().email());
    if (employees.existsByNormalizedEmail(email)) return;
    Employee manager = new Employee();
    manager.setEnglishName(properties.bootstrap().englishName());
    manager.setPreferredName(properties.bootstrap().preferredName());
    manager.setNormalizedEmail(email);
    manager.setNormalizedPhone(normalizer.phone(properties.bootstrap().phone()));
    manager.setPasswordHash(passwordEncoder.encode(properties.bootstrap().password()));
    manager.setToastPinHash(tokens.hash(properties.bootstrap().toastPin()));
    manager.setToastPinCiphertext(valueProtector.protect(properties.bootstrap().toastPin()));
    manager.setHomeStore(StoreCode.SEATTLE);
    manager.setEligibleStores(EnumSet.allOf(StoreCode.class));
    manager.setPositions(EnumSet.of(Position.MANAGER));
    employees.save(manager);
  }
}
