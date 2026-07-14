CREATE TABLE employees (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  english_name VARCHAR(120) NOT NULL,
  preferred_name VARCHAR(120) NOT NULL,
  normalized_email VARCHAR(190) NOT NULL,
  normalized_phone VARCHAR(32) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  toast_pin_hash VARCHAR(128) NOT NULL,
  toast_pin_ciphertext VARCHAR(255) NOT NULL,
  home_store VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  profile_photo_key VARCHAR(255) NULL,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  last_login_at TIMESTAMP(6) NULL,
  UNIQUE KEY uk_employees_email (normalized_email),
  UNIQUE KEY uk_employees_phone (normalized_phone),
  UNIQUE KEY uk_employees_toast_pin_hash (toast_pin_hash)
);

CREATE TABLE employee_positions (
  employee_id BIGINT NOT NULL,
  position VARCHAR(64) NOT NULL,
  PRIMARY KEY (employee_id, position),
  CONSTRAINT fk_employee_positions_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE employee_eligible_stores (
  employee_id BIGINT NOT NULL,
  store VARCHAR(32) NOT NULL,
  PRIMARY KEY (employee_id, store),
  CONSTRAINT fk_employee_eligible_stores_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE invitations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  creator_id BIGINT NOT NULL,
  token_hash VARCHAR(128) NOT NULL,
  token_version INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  used_at TIMESTAMP(6) NULL,
  revoked_at TIMESTAMP(6) NULL,
  activated_employee_id BIGINT NULL,
  UNIQUE KEY uk_invitations_token_hash (token_hash),
  INDEX ix_invitations_status (status),
  CONSTRAINT fk_invitations_creator FOREIGN KEY (creator_id) REFERENCES employees(id),
  CONSTRAINT fk_invitations_activated_employee FOREIGN KEY (activated_employee_id) REFERENCES employees(id)
);

CREATE TABLE invitation_positions (
  invitation_id BIGINT NOT NULL,
  position VARCHAR(64) NOT NULL,
  PRIMARY KEY (invitation_id, position),
  CONSTRAINT fk_invitation_positions_invitation FOREIGN KEY (invitation_id) REFERENCES invitations(id)
);

CREATE TABLE refresh_tokens (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_id BIGINT NOT NULL,
  token_hash VARCHAR(128) NOT NULL,
  family_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  revoked_at TIMESTAMP(6) NULL,
  replaced_by_token_hash VARCHAR(128) NULL,
  reused_at TIMESTAMP(6) NULL,
  UNIQUE KEY uk_refresh_tokens_hash (token_hash),
  INDEX ix_refresh_tokens_employee (employee_id),
  CONSTRAINT fk_refresh_tokens_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE password_reset_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_id BIGINT NOT NULL,
  delivery_method VARCHAR(16) NOT NULL,
  token_hash VARCHAR(128) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  used_at TIMESTAMP(6) NULL,
  revoked_at TIMESTAMP(6) NULL,
  UNIQUE KEY uk_password_reset_token_hash (token_hash),
  INDEX ix_password_reset_employee (employee_id),
  CONSTRAINT fk_password_reset_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE pending_contact_changes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_id BIGINT NOT NULL,
  type VARCHAR(16) NOT NULL,
  normalized_value VARCHAR(190) NOT NULL,
  token_hash VARCHAR(128) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  verified_at TIMESTAMP(6) NULL,
  revoked_at TIMESTAMP(6) NULL,
  UNIQUE KEY uk_contact_change_token_hash (token_hash),
  INDEX ix_contact_change_employee_type (employee_id, type),
  CONSTRAINT fk_contact_change_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE toast_pin_audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_id BIGINT NOT NULL,
  actor_id BIGINT NOT NULL,
  old_masked_value VARCHAR(32) NOT NULL,
  new_masked_value VARCHAR(32) NOT NULL,
  changed_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT fk_toast_pin_audit_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
  CONSTRAINT fk_toast_pin_audit_actor FOREIGN KEY (actor_id) REFERENCES employees(id)
);

CREATE TABLE security_audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_id BIGINT NULL,
  actor_id BIGINT NULL,
  event_type VARCHAR(80) NOT NULL,
  metadata JSON NULL,
  created_at TIMESTAMP(6) NOT NULL,
  INDEX ix_security_audit_employee (employee_id),
  INDEX ix_security_audit_actor (actor_id)
);
