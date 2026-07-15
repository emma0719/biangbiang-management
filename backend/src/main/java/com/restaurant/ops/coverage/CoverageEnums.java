package com.restaurant.ops.coverage;

public class CoverageEnums {
  private CoverageEnums() {}

  public enum CoverageType {
    PUBLIC,
    DIRECT
  }

  public enum CoverageStatus {
    OPEN,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    CANCELLED
  }

  public enum ShiftType {
    LUNCH,
    DINNER,
    DOUBLE
  }
}
