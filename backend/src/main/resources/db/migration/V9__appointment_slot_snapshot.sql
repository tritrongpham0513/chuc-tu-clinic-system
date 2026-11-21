ALTER TABLE appointments
  ADD COLUMN slot_start_snapshot TIME NULL AFTER time_slot_id,
  ADD COLUMN slot_end_snapshot   TIME NULL AFTER slot_start_snapshot;

UPDATE appointments a
JOIN time_slots t ON t.id = a.time_slot_id
SET a.slot_start_snapshot = t.start_time,
    a.slot_end_snapshot   = t.end_time
WHERE a.slot_start_snapshot IS NULL;
