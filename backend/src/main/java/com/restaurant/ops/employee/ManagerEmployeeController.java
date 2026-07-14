package com.restaurant.ops.employee;

import com.restaurant.ops.auth.AppPrincipal;
import com.restaurant.ops.auth.AuthService;
import com.restaurant.ops.common.ApiException;
import com.restaurant.ops.profile.ProfileDtos;
import com.restaurant.ops.profile.ProfileService;
import com.restaurant.ops.security.AuthorizationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/manager/employees")
public class ManagerEmployeeController {
  private final EmployeeRepository employees;
  private final AuthorizationService authorization;
  private final ProfileService profileService;
  private final AuthService authService;

  public ManagerEmployeeController(EmployeeRepository employees, AuthorizationService authorization, ProfileService profileService, AuthService authService) {
    this.employees = employees;
    this.authorization = authorization;
    this.profileService = profileService;
    this.authService = authService;
  }

  @GetMapping
  List<ProfileDtos.EmployeePrivateResponse> list(@AuthenticationPrincipal AppPrincipal principal) {
    requireManager(principal);
    return employees.findAll().stream().map(profileService::privateResponse).toList();
  }

  @GetMapping("/{id}")
  ProfileDtos.EmployeePrivateResponse detail(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    requireManager(principal);
    return profileService.privateResponse(find(id));
  }

  @PatchMapping("/{id}/positions")
  @Transactional
  ProfileDtos.EmployeePrivateResponse updatePositions(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id, @Valid @RequestBody ManagerEmployeeDtos.UpdatePositionsRequest request) {
    requireManager(principal);
    Employee target = find(id);
    target.setPositions(request.positions());
    return profileService.privateResponse(target);
  }

  @PatchMapping("/{id}/toast-pin")
  Map<String, String> updateToastPin(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id, @Valid @RequestBody ManagerEmployeeDtos.UpdateToastPinRequest request) {
    requireManager(principal);
    profileService.updateToastPin(principal.employee(), find(id), request.toastPin());
    return Map.of("status", "ok");
  }

  @PatchMapping("/{id}/stores")
  @Transactional
  ProfileDtos.EmployeePrivateResponse updateStores(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id, @Valid @RequestBody ProfileDtos.ManagerUpdateStoresRequest request) {
    requireManager(principal);
    return profileService.privateResponse(profileService.managerUpdateStores(principal.employee(), find(id), request));
  }

  @PostMapping("/{id}/deactivate")
  @Transactional
  Map<String, String> deactivate(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    requireManager(principal);
    Employee target = find(id);
    target.setStatus(EmployeeStatus.DEACTIVATED);
    authService.logoutAll(target);
    return Map.of("status", "ok");
  }

  @PostMapping("/{id}/logout-all")
  Map<String, String> forceLogout(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    requireManager(principal);
    authService.logoutAll(find(id));
    return Map.of("status", "ok");
  }

  private Employee find(Long id) {
    return employees.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND"));
  }

  private void requireManager(AppPrincipal principal) {
    if (!authorization.isManager(principal.employee())) {
      throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_MANAGER_REQUIRED");
    }
  }
}
