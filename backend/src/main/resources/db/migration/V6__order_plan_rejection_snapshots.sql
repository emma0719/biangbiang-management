ALTER TABLE order_plan_sessions
  ADD COLUMN rejected_by_employee_id BIGINT NULL AFTER completed_at,
  ADD COLUMN rejected_by_name_snapshot VARCHAR(190) NULL AFTER rejected_by_employee_id,
  ADD COLUMN rejected_at TIMESTAMP(6) NULL AFTER rejected_by_name_snapshot,
  ADD COLUMN rejection_reason TEXT NULL AFTER rejected_at,
  ADD KEY ix_order_plans_rejected_by (rejected_by_employee_id),
  ADD CONSTRAINT fk_order_plans_rejected_by FOREIGN KEY (rejected_by_employee_id) REFERENCES employees(id);
