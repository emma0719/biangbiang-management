package com.restaurant.ops.employee;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public class ManagerEmployeeDtos {
  public record UpdatePositionsRequest(@NotEmpty Set<Position> positions) {}
  public record UpdateToastPinRequest(@NotNull String toastPin) {}
}
