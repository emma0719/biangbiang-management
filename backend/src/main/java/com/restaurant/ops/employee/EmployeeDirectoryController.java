package com.restaurant.ops.employee;

import com.restaurant.ops.common.ApiException;
import com.restaurant.ops.profile.ProfileDtos;
import com.restaurant.ops.profile.ProfileService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
public class EmployeeDirectoryController {
  private final EmployeeRepository employees;
  private final ProfileService profileService;

  public EmployeeDirectoryController(EmployeeRepository employees, ProfileService profileService) {
    this.employees = employees;
    this.profileService = profileService;
  }

  @GetMapping
  List<ProfileDtos.EmployeePublicResponse> listPublicEmployees() {
    return employees.findAll().stream()
        .filter(employee -> employee.getStatus() == EmployeeStatus.ACTIVE)
        .map(profileService::publicResponse)
        .toList();
  }

  @GetMapping("/{id}")
  ProfileDtos.EmployeePublicResponse getPublicEmployee(@PathVariable Long id) {
    return employees.findById(id)
        .map(profileService::publicResponse)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND"));
  }
}
