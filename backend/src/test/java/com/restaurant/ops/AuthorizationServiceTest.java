package com.restaurant.ops;

import static org.assertj.core.api.Assertions.assertThat;

import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.Position;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.security.AuthorizationService;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class AuthorizationServiceTest {
  private final AuthorizationService authorization = new AuthorizationService();

  @Test
  void serverTwoStarAndShiftLeaderCanEditForms() {
    Employee serverTwo = employee(Position.SERVER_TWO_STAR, StoreCode.SEATTLE, StoreCode.SEATTLE);
    Employee shiftLeader = employee(Position.SHIFT_LEADER, StoreCode.SEATTLE, StoreCode.SEATTLE);

    assertThat(authorization.canEditFormTemplate(serverTwo)).isTrue();
    assertThat(authorization.canModifySubmittedForm(shiftLeader)).isTrue();
  }

  @Test
  void eligibilityDoesNotGrantOperationalStoreAccess() {
    Employee employee = employee(Position.HOST, StoreCode.SEATTLE, StoreCode.SEATTLE, StoreCode.REDMOND);

    assertThat(authorization.canCoverShiftAtStore(employee, StoreCode.REDMOND)).isTrue();
    assertThat(authorization.canAccessStoreOperationalData(employee, StoreCode.REDMOND)).isFalse();
    assertThat(authorization.canAccessStoreOperationalData(employee, StoreCode.SEATTLE)).isTrue();
  }

  @Test
  void managerQualifiesAsManager() {
    Employee manager = employee(Position.MANAGER, StoreCode.REDMOND, StoreCode.SEATTLE, StoreCode.REDMOND);

    assertThat(authorization.isManager(manager)).isTrue();
    assertThat(authorization.canEditFormTemplate(manager)).isTrue();
    assertThat(authorization.hasWorkingPosition(manager, Position.HOST)).isTrue();
    assertThat(authorization.qualifyingWorkingPositions(manager)).containsExactlyInAnyOrder(Position.restaurantOperatingPositions().toArray(Position[]::new));
  }

  @Test
  void businessPartnerPositionsHaveTheSamePermissionsButShiftLeaderDoesNot() {
    Employee owner = employee(Position.OWNER, StoreCode.SEATTLE, StoreCode.SEATTLE);
    Employee manager = employee(Position.MANAGER, StoreCode.SEATTLE, StoreCode.SEATTLE);
    Employee financialManager = employee(Position.FINANCIAL_MANAGER, StoreCode.SEATTLE, StoreCode.SEATTLE);
    Employee shiftLeader = employee(Position.SHIFT_LEADER, StoreCode.SEATTLE, StoreCode.SEATTLE);
    Employee host = employee(Position.HOST, StoreCode.SEATTLE, StoreCode.SEATTLE);

    for (Employee businessPartner : java.util.List.of(owner, manager, financialManager)) {
      assertThat(authorization.isBusinessPartner(businessPartner)).isTrue();
      assertThat(authorization.isManager(businessPartner)).isTrue();
      assertThat(authorization.canEditFormTemplate(businessPartner)).isTrue();
      assertThat(authorization.canModifySubmittedForm(businessPartner)).isTrue();
      assertThat(authorization.canViewEmployeePrivateData(businessPartner, host)).isTrue();
      assertThat(authorization.canManageEmployee(businessPartner, host)).isTrue();
      assertThat(authorization.qualifyingWorkingPositions(businessPartner)).containsExactlyInAnyOrder(Position.restaurantOperatingPositions().toArray(Position[]::new));
    }

    assertThat(owner.getPositions()).containsExactly(Position.OWNER);
    assertThat(manager.getPositions()).containsExactly(Position.MANAGER);
    assertThat(financialManager.getPositions()).containsExactly(Position.FINANCIAL_MANAGER);
    assertThat(authorization.isBusinessPartner(shiftLeader)).isFalse();
    assertThat(authorization.canManageEmployee(shiftLeader, host)).isFalse();
  }

  @Test
  void oneStarAndTwoStarAreMutuallyExclusive() {
    Employee employee = new Employee();
    employee.setPositions(EnumSet.of(Position.SERVER_ONE_STAR, Position.SERVER_TWO_STAR));

    assertThat(employee.getPositions()).contains(Position.SERVER_TWO_STAR).doesNotContain(Position.SERVER_ONE_STAR);
  }

  @Test
  void assigningOneStarRemovesTwoStar() {
    Employee employee = new Employee();
    employee.setPositions(EnumSet.of(Position.SERVER_TWO_STAR));

    employee.assignPosition(Position.SERVER_ONE_STAR);

    assertThat(employee.getPositions()).contains(Position.SERVER_ONE_STAR).doesNotContain(Position.SERVER_TWO_STAR);
  }

  private Employee employee(Position position, StoreCode homeStore, StoreCode... eligibleStores) {
    Employee employee = new Employee();
    employee.setHomeStore(homeStore);
    employee.setPositions(EnumSet.of(position));
    employee.setEligibleStores(EnumSet.copyOf(java.util.List.of(eligibleStores)));
    return employee;
  }
}
