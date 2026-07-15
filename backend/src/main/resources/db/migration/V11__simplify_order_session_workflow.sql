ALTER TABLE order_plan_sessions
  ADD COLUMN last_modified_by_employee_id BIGINT NULL AFTER submitted_at,
  ADD COLUMN last_modified_by_name_snapshot VARCHAR(190) NULL AFTER last_modified_by_employee_id,
  ADD KEY ix_order_sessions_last_modified_by (last_modified_by_employee_id),
  ADD CONSTRAINT fk_order_sessions_last_modified_by FOREIGN KEY (last_modified_by_employee_id) REFERENCES employees(id);
