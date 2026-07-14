package com.restaurant.ops.profile;

import com.restaurant.ops.employee.Employee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingContactChangeRepository extends JpaRepository<PendingContactChange, Long> {
  Optional<PendingContactChange> findByTokenHash(String tokenHash);
  List<PendingContactChange> findByEmployeeAndTypeAndVerifiedAtIsNullAndRevokedAtIsNull(Employee employee, PendingContactChange.Type type);
}
