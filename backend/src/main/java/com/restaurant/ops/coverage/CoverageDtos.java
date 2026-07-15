package com.restaurant.ops.coverage;

import com.restaurant.ops.coverage.CoverageEnums.CoverageStatus;
import com.restaurant.ops.coverage.CoverageEnums.CoverageType;
import com.restaurant.ops.coverage.CoverageEnums.ShiftType;
import com.restaurant.ops.employee.Position;
import com.restaurant.ops.employee.StoreCode;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public class CoverageDtos {
  private CoverageDtos() {}

  public record CreateCoverageRequest(
      @NotNull StoreCode store,
      @NotNull CoverageType coverageType,
      @NotNull LocalDate shiftDate,
      LocalTime startTime,
      LocalTime endTime,
      @NotNull ShiftType shiftType,
      @NotNull Position position,
      String reason,
      Long replacementEmployeeId
  ) {}

  public record RejectCoverageRequest(String managerNote) {}

  public record EmployeeSummary(Long id, String displayName) {}

  public record CoverageRequestResponse(
      Long id,
      StoreCode store,
      EmployeeSummary requestedBy,
      EmployeeSummary replacementEmployee,
      boolean replacementAssigned,
      CoverageType coverageType,
      LocalDate shiftDate,
      LocalTime startTime,
      LocalTime endTime,
      ShiftType shiftType,
      Position position,
      String reason,
      String managerNote,
      CoverageStatus status,
      EmployeeSummary approvedBy,
      Instant approvedAt,
      EmployeeSummary reviewedBy,
      Instant reviewedAt,
      Instant createdAt,
      Instant updatedAt
  ) {}
}
