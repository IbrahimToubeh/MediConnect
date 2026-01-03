-- Migration script to create schedule management tables
-- Run this script to add the new schedule management functionality

-- Create day_availability table
CREATE TABLE IF NOT EXISTS day_availability (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    start_time VARCHAR(5),
    end_time VARCHAR(5),
    CONSTRAINT fk_day_availability_provider FOREIGN KEY (provider_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_provider_day UNIQUE (provider_id, day_of_week)
);

-- Create blocked_time_slots table
CREATE TABLE IF NOT EXISTS blocked_time_slots (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    blocked_date DATE NOT NULL,
    start_time VARCHAR(5) NOT NULL,
    end_time VARCHAR(5) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_blocked_slot_provider FOREIGN KEY (provider_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Add appointment_duration_minutes column to healthcare_provider table
-- Note: healthcare_provider is part of the users table (joined inheritance)
-- We need to add this to the healthcare_provider table
ALTER TABLE healthcare_provider 
ADD COLUMN IF NOT EXISTS appointment_duration_minutes INTEGER DEFAULT 30;

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_day_availability_provider ON day_availability(provider_id);
CREATE INDEX IF NOT EXISTS idx_day_availability_day ON day_availability(day_of_week);
CREATE INDEX IF NOT EXISTS idx_blocked_slots_provider ON blocked_time_slots(provider_id);
CREATE INDEX IF NOT EXISTS idx_blocked_slots_date ON blocked_time_slots(blocked_date);

-- Add comments for documentation
COMMENT ON TABLE day_availability IS 'Stores per-day availability schedule for healthcare providers';
COMMENT ON TABLE blocked_time_slots IS 'Stores blocked time slots when providers are unavailable';
COMMENT ON COLUMN healthcare_provider.appointment_duration_minutes IS 'Duration between appointments in minutes (15, 30, 45, 60, etc.)';

