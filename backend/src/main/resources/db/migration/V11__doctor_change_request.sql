ALTER TABLE appointments
  ADD COLUMN doctor_change_requested_at DATETIME NULL,
  ADD COLUMN doctor_change_note VARCHAR(255) NULL;
