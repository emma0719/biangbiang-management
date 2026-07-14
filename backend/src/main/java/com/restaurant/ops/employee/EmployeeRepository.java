package com.restaurant.ops.employee;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
  Optional<Employee> findByNormalizedEmail(String normalizedEmail);
  Optional<Employee> findByNormalizedPhone(String normalizedPhone);
  boolean existsByNormalizedEmail(String normalizedEmail);
  boolean existsByNormalizedPhone(String normalizedPhone);
  boolean existsByToastPinHash(String toastPinHash);
}
