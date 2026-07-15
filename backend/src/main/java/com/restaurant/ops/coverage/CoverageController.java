package com.restaurant.ops.coverage;

import com.restaurant.ops.auth.AppPrincipal;
import com.restaurant.ops.coverage.CoverageDtos.CoverageRequestResponse;
import com.restaurant.ops.coverage.CoverageDtos.CreateCoverageRequest;
import com.restaurant.ops.coverage.CoverageDtos.RejectCoverageRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CoverageController {
  private final CoverageService service;

  CoverageController(CoverageService service) {
    this.service = service;
  }

  @PostMapping("/api/coverage-requests")
  CoverageRequestResponse create(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody CreateCoverageRequest request) {
    return service.create(principal.employee(), request);
  }

  @GetMapping("/api/coverage-requests/mine")
  List<CoverageRequestResponse> mine(@AuthenticationPrincipal AppPrincipal principal) {
    return service.mine(principal.employee());
  }

  @GetMapping("/api/coverage-requests/involving-me")
  List<CoverageRequestResponse> involvingMe(@AuthenticationPrincipal AppPrincipal principal) {
    return service.involvingMe(principal.employee());
  }

  @GetMapping("/api/coverage-requests/pool")
  List<CoverageRequestResponse> pool(@AuthenticationPrincipal AppPrincipal principal) {
    return service.pool(principal.employee());
  }

  @PostMapping("/api/coverage-requests/{id}/claim")
  CoverageRequestResponse claim(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    return service.claim(principal.employee(), id);
  }

  @PostMapping("/api/coverage-requests/{id}/cancel")
  CoverageRequestResponse cancel(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    return service.cancel(principal.employee(), id);
  }

  @GetMapping("/api/coverage-requests/pending")
  List<CoverageRequestResponse> pending(@AuthenticationPrincipal AppPrincipal principal) {
    return service.pending(principal.employee());
  }

  @GetMapping("/api/coverage-requests/upcoming")
  List<CoverageRequestResponse> upcoming(@AuthenticationPrincipal AppPrincipal principal, @RequestParam(defaultValue = "14") int days) {
    return service.upcoming(principal.employee(), days);
  }

  @GetMapping("/api/coverage-requests/history")
  List<CoverageRequestResponse> history(@AuthenticationPrincipal AppPrincipal principal) {
    return service.history(principal.employee());
  }

  @PostMapping("/api/coverage-requests/{id}/approve")
  CoverageRequestResponse approve(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    return service.approve(principal.employee(), id);
  }

  @PostMapping("/api/coverage-requests/{id}/reject")
  CoverageRequestResponse reject(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id, @RequestBody(required = false) RejectCoverageRequest request) {
    return service.reject(principal.employee(), id, request);
  }
}
