package com.restaurant.ops.security;

import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.Position;
import com.restaurant.ops.employee.StoreCode;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {
  public boolean isManager(Employee employee) {
    return isBusinessPartner(employee);
  }

  public boolean isBusinessPartner(Employee employee) {
    return employee.getPositions().stream().anyMatch(Position::isBusinessPartner);
  }

  public boolean canEditFormTemplate(Employee employee) {
    return hasElevatedFormPermission(employee);
  }

  public boolean canModifySubmittedForm(Employee employee) {
    return hasElevatedFormPermission(employee);
  }

  public boolean canAccessStoreOperationalData(Employee employee, StoreCode store) {
    return employee.getHomeStore() == store;
  }

  public boolean canCoverShiftAtStore(Employee employee, StoreCode store) {
    return employee.getEligibleStores().contains(store);
  }

  public boolean hasWorkingPosition(Employee employee, Position position) {
    return qualifyingWorkingPositions(employee).contains(position);
  }

  public Set<Position> qualifyingWorkingPositions(Employee employee) {
    if (isBusinessPartner(employee)) {
      return Position.restaurantOperatingPositions();
    }
    return employee.getPositions().isEmpty() ? EnumSet.noneOf(Position.class) : EnumSet.copyOf(employee.getPositions());
  }

  public boolean canViewEmployeePrivateData(Employee actor, Employee target) {
    return isBusinessPartner(actor) || actor.getId().equals(target.getId());
  }

  public boolean canManageEmployee(Employee actor, Employee target) {
    if (!isBusinessPartner(actor) || actor == target) {
      return false;
    }
    return actor.getId() == null || target.getId() == null || !actor.getId().equals(target.getId());
  }

  private boolean hasElevatedFormPermission(Employee employee) {
    return isBusinessPartner(employee)
        || employee.getPositions().contains(Position.SHIFT_LEADER)
        || employee.getPositions().contains(Position.SERVER_TWO_STAR);
  }
}
