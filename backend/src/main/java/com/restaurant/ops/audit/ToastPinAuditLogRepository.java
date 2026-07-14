package com.restaurant.ops.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ToastPinAuditLogRepository extends JpaRepository<ToastPinAuditLog, Long> {}
