package com.restaurant.ops.ordering;

import com.restaurant.ops.employee.Employee;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class OrderingAuditRecorder {
  private final OrderAuditEventRepository auditEvents;

  OrderingAuditRecorder(OrderAuditEventRepository auditEvents) {
    this.auditEvents = auditEvents;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordPdfGenerationFailure(Long orderId, Employee actor, String status, String reason) {
    OrderAuditEvent event = new OrderAuditEvent();
    event.setEntityType("PURCHASE_ORDER");
    event.setEntityId(orderId);
    event.setAction("APPROVED_PDF_GENERATION_FAILED");
    event.setActor(actor);
    event.setActorNameSnapshot(actor.getDisplayName());
    event.setOldValue(status);
    event.setNewValue(status);
    event.setReason(reason);
    auditEvents.save(event);
  }
}
