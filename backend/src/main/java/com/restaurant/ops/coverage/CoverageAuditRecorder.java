package com.restaurant.ops.coverage;

import com.restaurant.ops.coverage.CoverageEnums.CoverageStatus;
import com.restaurant.ops.employee.Employee;
import org.springframework.stereotype.Service;

@Service
class CoverageAuditRecorder {
  private final CoverageAuditEventRepository auditEvents;

  CoverageAuditRecorder(CoverageAuditEventRepository auditEvents) {
    this.auditEvents = auditEvents;
  }

  void record(String action, Employee actor, CoverageRequest request, CoverageStatus oldStatus, String details) {
    CoverageAuditEvent event = new CoverageAuditEvent();
    event.setRequestId(request.getId());
    event.setAction(action);
    event.setActor(actor);
    event.setActorNameSnapshot(actor.getDisplayName());
    event.setRequestedByEmployeeId(request.getRequestedBy().getId());
    event.setRequestedByNameSnapshot(request.getRequestedBy().getDisplayName());
    if (request.getReplacementEmployee() != null) {
      event.setReplacementEmployeeId(request.getReplacementEmployee().getId());
      event.setReplacementNameSnapshot(request.getReplacementEmployee().getDisplayName());
    }
    event.setStore(request.getStore());
    event.setShiftDate(request.getShiftDate());
    event.setOldStatus(oldStatus);
    event.setNewStatus(request.getStatus());
    event.setDetails(details);
    auditEvents.save(event);
  }
}
