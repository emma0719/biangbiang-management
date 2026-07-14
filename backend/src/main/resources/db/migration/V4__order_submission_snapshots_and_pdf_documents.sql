ALTER TABLE inventory_count_sessions
  ADD COLUMN completed_by_name_snapshot VARCHAR(190) NULL AFTER completed_by_employee_id;

ALTER TABLE purchase_orders
  ADD COLUMN source_inventory_session_id BIGINT NULL AFTER vendor_id,
  ADD COLUMN submitted_by_name_snapshot VARCHAR(190) NULL AFTER submitted_by_employee_id,
  ADD COLUMN approved_by_name_snapshot VARCHAR(190) NULL AFTER approved_by_employee_id,
  ADD COLUMN rejected_by_employee_id BIGINT NULL AFTER approved_by_name_snapshot,
  ADD COLUMN rejected_by_name_snapshot VARCHAR(190) NULL AFTER rejected_by_employee_id,
  ADD COLUMN rejected_at TIMESTAMP(6) NULL AFTER received_at,
  ADD KEY ix_purchase_orders_source_inventory (source_inventory_session_id),
  ADD CONSTRAINT fk_purchase_orders_source_inventory FOREIGN KEY (source_inventory_session_id) REFERENCES inventory_count_sessions(id),
  ADD CONSTRAINT fk_purchase_orders_rejected_by FOREIGN KEY (rejected_by_employee_id) REFERENCES employees(id);

CREATE TABLE purchase_order_pdf_documents (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  purchase_order_id BIGINT NOT NULL,
  version_number INT NOT NULL,
  filename VARCHAR(255) NOT NULL,
  mime_type VARCHAR(120) NOT NULL,
  generated_at TIMESTAMP(6) NOT NULL,
  generated_by_employee_id BIGINT NOT NULL,
  checksum_sha256 VARCHAR(64) NOT NULL,
  content LONGBLOB NOT NULL,
  current_version BOOLEAN NOT NULL DEFAULT TRUE,
  superseded_at TIMESTAMP(6) NULL,
  UNIQUE KEY uk_purchase_order_pdf_version (purchase_order_id, version_number),
  KEY ix_purchase_order_pdf_order_current (purchase_order_id, current_version),
  CONSTRAINT fk_purchase_order_pdf_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id),
  CONSTRAINT fk_purchase_order_pdf_generated_by FOREIGN KEY (generated_by_employee_id) REFERENCES employees(id)
);

ALTER TABLE order_audit_events
  ADD COLUMN actor_name_snapshot VARCHAR(190) NULL AFTER actor_employee_id,
  ADD COLUMN pdf_document_id BIGINT NULL AFTER reason,
  ADD COLUMN pdf_version INT NULL AFTER pdf_document_id,
  ADD CONSTRAINT fk_order_audit_pdf_document FOREIGN KEY (pdf_document_id) REFERENCES purchase_order_pdf_documents(id);
