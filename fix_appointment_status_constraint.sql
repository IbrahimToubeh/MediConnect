-- Fix appointment_entity_status_check constraint to allow enum values

-- Drop the existing constraint if it exists
-- Normalize existing data to the new 4-value status set
-- Map old statuses to the new ones
UPDATE appointment_entity SET status = 'PENDING'     WHERE status IN ('SCHEDULED');
UPDATE appointment_entity SET status = 'CONFIRMED'   WHERE status IN ('IN_PROGRESS','COMPLETED');
UPDATE appointment_entity SET status = 'CANCELLED'   WHERE status IN ('NO_SHOW');

-- Drop and recreate the constraint with the correct enum values
ALTER TABLE appointment_entity 
DROP CONSTRAINT IF EXISTS appointment_entity_status_check;

ALTER TABLE appointment_entity 
ADD CONSTRAINT appointment_entity_status_check 
CHECK (status IN ('PENDING', 'CONFIRMED', 'RESCHEDULED', 'CANCELLED'));

