package com.restaurant.ops.auth;

import com.restaurant.ops.employee.Employee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByTokenHash(String tokenHash);
  List<RefreshToken> findByEmployeeAndRevokedAtIsNull(Employee employee);
  List<RefreshToken> findByFamilyIdAndRevokedAtIsNull(String familyId);
}
