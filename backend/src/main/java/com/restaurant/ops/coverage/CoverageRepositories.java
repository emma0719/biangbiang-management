package com.restaurant.ops.coverage;

import com.restaurant.ops.coverage.CoverageEnums.CoverageStatus;
import com.restaurant.ops.coverage.CoverageEnums.CoverageType;
import com.restaurant.ops.employee.Employee;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CoverageRequestRepository extends JpaRepository<CoverageRequest, Long> {
  List<CoverageRequest> findByRequestedByOrderByShiftDateDescIdDesc(Employee employee);
  List<CoverageRequest> findByReplacementEmployeeOrderByShiftDateDescIdDesc(Employee employee);
  List<CoverageRequest> findByCoverageTypeAndStatusOrderByShiftDateAscIdAsc(CoverageType coverageType, CoverageStatus status);
  List<CoverageRequest> findByStatusOrderByShiftDateAscIdAsc(CoverageStatus status);
  List<CoverageRequest> findByStatusAndShiftDateBetweenOrderByShiftDateAscIdAsc(CoverageStatus status, LocalDate start, LocalDate end);
  List<CoverageRequest> findAllByOrderByCreatedAtDescIdDesc();

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update CoverageRequest request
         set request.replacementEmployee = :replacement,
             request.status = :pendingStatus,
             request.updatedAt = :now
       where request.id = :id
         and request.coverageType = :publicType
         and request.status = :openStatus
         and request.replacementEmployee is null
         and request.requestedBy.id <> :replacementId
      """)
  int claimOpenRequest(
      @Param("id") Long id,
      @Param("replacement") Employee replacement,
      @Param("replacementId") Long replacementId,
      @Param("publicType") CoverageType publicType,
      @Param("openStatus") CoverageStatus openStatus,
      @Param("pendingStatus") CoverageStatus pendingStatus,
      @Param("now") Instant now
  );
}

interface CoverageAuditEventRepository extends JpaRepository<CoverageAuditEvent, Long> {
  List<CoverageAuditEvent> findByRequestIdOrderByCreatedAtAsc(Long requestId);
}
