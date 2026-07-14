package com.restaurant.ops.auth;

import com.restaurant.ops.employee.Employee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetRepository extends JpaRepository<PasswordResetRequest, Long> {
  Optional<PasswordResetRequest> findByTokenHash(String tokenHash);
  List<PasswordResetRequest> findByEmployeeAndUsedAtIsNullAndRevokedAtIsNull(Employee employee);
}
