ALTER TABLE order_plan_sessions
  MODIFY source_inventory_session_id BIGINT NULL,
  MODIFY inventory_business_date DATE NULL,
  ADD COLUMN order_business VARCHAR(32) NOT NULL DEFAULT 'BIANGBIANG_FRONT' AFTER location_code,
  ADD COLUMN reviewed_by_employee_id BIGINT NULL AFTER submitted_at,
  ADD COLUMN reviewed_by_name_snapshot VARCHAR(190) NULL AFTER reviewed_by_employee_id,
  ADD COLUMN reviewed_at TIMESTAMP(6) NULL AFTER reviewed_by_name_snapshot,
  ADD COLUMN review_note TEXT NULL AFTER reviewed_at,
  ADD KEY ix_order_sessions_reviewed_by (reviewed_by_employee_id),
  ADD CONSTRAINT fk_order_sessions_reviewed_by FOREIGN KEY (reviewed_by_employee_id) REFERENCES employees(id);
