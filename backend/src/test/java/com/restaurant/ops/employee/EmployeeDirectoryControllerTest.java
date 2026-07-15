package com.restaurant.ops.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restaurant.ops.profile.ProfileDtos;
import com.restaurant.ops.profile.ProfileService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeDirectoryControllerTest {
  @Mock EmployeeRepository employees;
  @Mock ProfileService profileService;

  @Test
  void publicDirectoryOnlyReturnsActiveEmployeesForCoverageReplacementChoices() {
    Employee active = employee("Jordan", EmployeeStatus.ACTIVE);
    Employee inactive = employee("Inactive Pat", EmployeeStatus.DEACTIVATED);
    ProfileDtos.EmployeePublicResponse activeResponse = new ProfileDtos.EmployeePublicResponse(
        null,
        "Jordan",
        null,
        Set.of(Position.HOST),
        StoreCode.SEATTLE,
        EmployeeStatus.ACTIVE
    );
    when(employees.findAll()).thenReturn(List.of(active, inactive));
    when(profileService.publicResponse(active)).thenReturn(activeResponse);

    EmployeeDirectoryController controller = new EmployeeDirectoryController(employees, profileService);

    List<ProfileDtos.EmployeePublicResponse> response = controller.listPublicEmployees();

    assertThat(response).containsExactly(activeResponse);
    verify(profileService, never()).publicResponse(inactive);
  }

  private static Employee employee(String name, EmployeeStatus status) {
    Employee employee = new Employee();
    employee.setEnglishName(name);
    employee.setPreferredName(name);
    employee.setHomeStore(StoreCode.SEATTLE);
    employee.setStatus(status);
    employee.setPositions(Set.of(Position.HOST));
    return employee;
  }
}
