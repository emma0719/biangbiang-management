package com.restaurant.ops.coverage;

import com.restaurant.ops.common.ApiException;
import com.restaurant.ops.coverage.CoverageDtos.CoverageRequestResponse;
import com.restaurant.ops.coverage.CoverageDtos.CreateCoverageRequest;
import com.restaurant.ops.coverage.CoverageDtos.EmployeeSummary;
import com.restaurant.ops.coverage.CoverageDtos.RejectCoverageRequest;
import com.restaurant.ops.coverage.CoverageEnums.CoverageStatus;
import com.restaurant.ops.coverage.CoverageEnums.CoverageType;
import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.EmployeeRepository;
import com.restaurant.ops.employee.EmployeeStatus;
import com.restaurant.ops.security.AuthorizationService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoverageService {
  private final CoverageRequestRepository requests;
  private final CoverageAuditEventRepository auditEvents;
  private final EmployeeRepository employees;
  private final AuthorizationService authorization;
  private final CoverageAuditRecorder auditRecorder;
  private final Clock clock = Clock.systemDefaultZone();

  public CoverageService(CoverageRequestRepository requests, CoverageAuditEventRepository auditEvents, EmployeeRepository employees, AuthorizationService authorization, CoverageAuditRecorder auditRecorder) {
    this.requests = requests;
    this.auditEvents = auditEvents;
    this.employees = employees;
    this.authorization = authorization;
    this.auditRecorder = auditRecorder;
  }

  @Transactional
  public CoverageRequestResponse create(Employee actor, CreateCoverageRequest request) {
    requireActive(actor);
    validateTimes(request);
    CoverageRequest coverage = new CoverageRequest();
    coverage.setStore(request.store());
    coverage.setRequestedBy(actor);
    coverage.setCoverageType(request.coverageType());
    coverage.setShiftDate(request.shiftDate());
    coverage.setStartTime(request.startTime());
    coverage.setEndTime(request.endTime());
    coverage.setShiftType(request.shiftType());
    coverage.setPosition(request.position());
    coverage.setReason(blankToNull(request.reason()));
    if (request.coverageType() == CoverageType.PUBLIC) {
      if (request.replacementEmployeeId() != null) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "COVERAGE_PUBLIC_REPLACEMENT_NOT_ALLOWED");
      }
      coverage.setStatus(CoverageStatus.OPEN);
    } else if (request.coverageType() == CoverageType.DIRECT) {
      Employee replacement = activeEmployee(request.replacementEmployeeId(), "COVERAGE_REPLACEMENT_REQUIRED");
      if (sameEmployee(actor, replacement)) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "COVERAGE_REPLACEMENT_CANNOT_BE_SELF");
      }
      coverage.setReplacementEmployee(replacement);
      coverage.setStatus(CoverageStatus.PENDING_APPROVAL);
    } else {
      throw new ApiException(HttpStatus.BAD_REQUEST, "COVERAGE_TYPE_REQUIRED");
    }
    CoverageRequest saved = requests.save(coverage);
    auditRecorder.record("COVERAGE_REQUEST_CREATED", actor, saved, null, null);
    return response(actor, saved);
  }

  @Transactional(readOnly = true)
  public List<CoverageRequestResponse> mine(Employee actor) {
    requireActive(actor);
    return requests.findByRequestedByOrderByShiftDateDescIdDesc(actor).stream().map(request -> response(actor, request)).toList();
  }

  @Transactional(readOnly = true)
  public List<CoverageRequestResponse> involvingMe(Employee actor) {
    requireActive(actor);
    return requests.findByReplacementEmployeeOrderByShiftDateDescIdDesc(actor).stream().map(request -> response(actor, request)).toList();
  }

  @Transactional(readOnly = true)
  public List<CoverageRequestResponse> pool(Employee actor) {
    requireActive(actor);
    return requests.findByCoverageTypeAndStatusOrderByShiftDateAscIdAsc(CoverageType.PUBLIC, CoverageStatus.OPEN).stream()
        .map(request -> response(actor, request))
        .toList();
  }

  @Transactional
  public CoverageRequestResponse claim(Employee actor, Long id) {
    requireActive(actor);
    CoverageRequest before = get(id);
    if (sameEmployee(actor, before.getRequestedBy())) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "COVERAGE_CANNOT_CLAIM_OWN_REQUEST");
    }
    int updated = requests.claimOpenRequest(
        id,
        actor,
        actor.getId(),
        CoverageType.PUBLIC,
        CoverageStatus.OPEN,
        CoverageStatus.PENDING_APPROVAL,
        Instant.now(clock)
    );
    if (updated != 1) {
      CoverageRequest current = get(id);
      if (current.getStatus() != CoverageStatus.OPEN || current.getReplacementEmployee() != null) {
        throw new ApiException(HttpStatus.CONFLICT, "COVERAGE_REQUEST_ALREADY_CLAIMED");
      }
      throw new ApiException(HttpStatus.BAD_REQUEST, "COVERAGE_REQUEST_NOT_CLAIMABLE");
    }
    CoverageRequest claimed = get(id);
    auditRecorder.record("COVERAGE_REQUEST_CLAIMED", actor, claimed, before.getStatus(), null);
    return response(actor, claimed);
  }

  @Transactional
  public CoverageRequestResponse cancel(Employee actor, Long id) {
    requireActive(actor);
    CoverageRequest request = get(id);
    if (!sameEmployee(actor, request.getRequestedBy())) {
      throw new ApiException(HttpStatus.FORBIDDEN, "COVERAGE_CANCEL_OWN_REQUEST_REQUIRED");
    }
    if (request.getStatus() != CoverageStatus.OPEN && request.getStatus() != CoverageStatus.PENDING_APPROVAL) {
      throw new ApiException(HttpStatus.CONFLICT, "COVERAGE_REQUEST_NOT_CANCELLABLE");
    }
    CoverageStatus oldStatus = request.getStatus();
    request.setStatus(CoverageStatus.CANCELLED);
    CoverageRequest saved = requests.save(request);
    auditRecorder.record("COVERAGE_REQUEST_CANCELLED", actor, saved, oldStatus, null);
    return response(actor, saved);
  }

  @Transactional(readOnly = true)
  public List<CoverageRequestResponse> pending(Employee actor) {
    requireBusinessPartner(actor);
    return requests.findByStatusOrderByShiftDateAscIdAsc(CoverageStatus.PENDING_APPROVAL).stream().map(request -> response(actor, request)).toList();
  }

  @Transactional(readOnly = true)
  public List<CoverageRequestResponse> upcoming(Employee actor, int days) {
    requireBusinessPartner(actor);
    int boundedDays = Math.max(0, Math.min(days, 90));
    LocalDate start = LocalDate.now(ZoneId.systemDefault());
    LocalDate end = start.plusDays(boundedDays);
    return requests.findByStatusAndShiftDateBetweenOrderByShiftDateAscIdAsc(CoverageStatus.APPROVED, start, end).stream()
        .map(request -> response(actor, request))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CoverageRequestResponse> history(Employee actor) {
    requireBusinessPartner(actor);
    return requests.findAllByOrderByCreatedAtDescIdDesc().stream().map(request -> response(actor, request)).toList();
  }

  @Transactional
  public CoverageRequestResponse approve(Employee actor, Long id) {
    requireBusinessPartner(actor);
    CoverageRequest request = get(id);
    if (request.getStatus() != CoverageStatus.PENDING_APPROVAL) {
      throw new ApiException(HttpStatus.CONFLICT, "COVERAGE_REQUEST_NOT_PENDING_APPROVAL");
    }
    CoverageStatus oldStatus = request.getStatus();
    request.setStatus(CoverageStatus.APPROVED);
    request.setApprovedBy(actor);
    request.setApprovedAt(Instant.now(clock));
    request.setReviewedBy(actor);
    request.setReviewedAt(request.getApprovedAt());
    CoverageRequest saved = requests.save(request);
    auditRecorder.record("COVERAGE_REQUEST_APPROVED", actor, saved, oldStatus, null);
    return response(actor, saved);
  }

  @Transactional
  public CoverageRequestResponse reject(Employee actor, Long id, RejectCoverageRequest review) {
    requireBusinessPartner(actor);
    CoverageRequest request = get(id);
    if (request.getStatus() != CoverageStatus.PENDING_APPROVAL) {
      throw new ApiException(HttpStatus.CONFLICT, "COVERAGE_REQUEST_NOT_PENDING_APPROVAL");
    }
    CoverageStatus oldStatus = request.getStatus();
    request.setStatus(CoverageStatus.REJECTED);
    request.setReviewedBy(actor);
    request.setReviewedAt(Instant.now(clock));
    request.setManagerNote(review == null ? null : blankToNull(review.managerNote()));
    CoverageRequest saved = requests.save(request);
    auditRecorder.record("COVERAGE_REQUEST_REJECTED", actor, saved, oldStatus, saved.getManagerNote());
    return response(actor, saved);
  }

  @Transactional(readOnly = true)
  public List<CoverageAuditEvent> auditEvents(Long requestId) {
    return auditEvents.findByRequestIdOrderByCreatedAtAsc(requestId);
  }

  private CoverageRequest get(Long id) {
    return requests.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COVERAGE_REQUEST_NOT_FOUND"));
  }

  private void requireActive(Employee actor) {
    if (actor == null || actor.getStatus() != EmployeeStatus.ACTIVE) {
      throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_ACTIVE_EMPLOYEE_REQUIRED");
    }
  }

  private void requireBusinessPartner(Employee actor) {
    requireActive(actor);
    if (!authorization.isBusinessPartner(actor)) {
      throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_BUSINESS_PARTNER_REQUIRED");
    }
  }

  private Employee activeEmployee(Long id, String missingCode) {
    if (id == null) {
      throw new ApiException(HttpStatus.BAD_REQUEST, missingCode);
    }
    return employees.findById(id)
        .filter(employee -> employee.getStatus() == EmployeeStatus.ACTIVE)
        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "COVERAGE_REPLACEMENT_ACTIVE_EMPLOYEE_REQUIRED"));
  }

  private void validateTimes(CreateCoverageRequest request) {
    if (request.startTime() != null && request.endTime() != null && !request.endTime().isAfter(request.startTime())) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "COVERAGE_END_TIME_MUST_BE_AFTER_START_TIME");
    }
  }

  private boolean sameEmployee(Employee left, Employee right) {
    return left != null && right != null && left.getId() != null && left.getId().equals(right.getId());
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private CoverageRequestResponse response(Employee actor, CoverageRequest request) {
    boolean canSeeReplacement = authorization.isBusinessPartner(actor)
        || sameEmployee(actor, request.getRequestedBy())
        || sameEmployee(actor, request.getReplacementEmployee());
    return new CoverageRequestResponse(
        request.getId(),
        request.getStore(),
        employeeSummary(request.getRequestedBy()),
        canSeeReplacement ? employeeSummary(request.getReplacementEmployee()) : null,
        request.getReplacementEmployee() != null,
        request.getCoverageType(),
        request.getShiftDate(),
        request.getStartTime(),
        request.getEndTime(),
        request.getShiftType(),
        request.getPosition(),
        request.getReason(),
        canSeeReplacement || authorization.isBusinessPartner(actor) ? request.getManagerNote() : null,
        request.getStatus(),
        employeeSummary(request.getApprovedBy()),
        request.getApprovedAt(),
        employeeSummary(request.getReviewedBy()),
        request.getReviewedAt(),
        request.getCreatedAt(),
        request.getUpdatedAt()
    );
  }

  private EmployeeSummary employeeSummary(Employee employee) {
    return employee == null ? null : new EmployeeSummary(employee.getId(), employee.getDisplayName());
  }
}
