-- ============================================
-- COMPLETE TEST DATA SCRIPT FOR MEDICONNECT
-- ============================================
-- This script includes:
-- 1. All Flyway migration DDL
-- 2. Comprehensive test data for thorough testing
-- ============================================

-- ============================================
-- PART 1: FLYWAY MIGRATION DDL
-- ============================================

-- Migration V2: Activity Tables
CREATE TABLE IF NOT EXISTS login_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_token VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent TEXT,
    location VARCHAR(255),
    device VARCHAR(100),
    browser VARCHAR(100),
    login_time TIMESTAMP NOT NULL,
    logout_time TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_time TIMESTAMP,
    CONSTRAINT unique_session_token UNIQUE (session_token)
);

CREATE TABLE IF NOT EXISTS account_activities (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    ip_address VARCHAR(45),
    location VARCHAR(255),
    device VARCHAR(100),
    user_agent TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    additional_data TEXT
);

CREATE INDEX IF NOT EXISTS idx_login_sessions_user_id ON login_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_login_sessions_session_token ON login_sessions(session_token);
CREATE INDEX IF NOT EXISTS idx_login_sessions_is_active ON login_sessions(is_active);
CREATE INDEX IF NOT EXISTS idx_login_sessions_login_time ON login_sessions(login_time);
CREATE INDEX IF NOT EXISTS idx_account_activities_user_id ON account_activities(user_id);
CREATE INDEX IF NOT EXISTS idx_account_activities_type ON account_activities(type);
CREATE INDEX IF NOT EXISTS idx_account_activities_timestamp ON account_activities(timestamp);

-- Migration V3: Notification Preferences
CREATE TABLE IF NOT EXISTS user_notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    push_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    post_likes BOOLEAN NOT NULL DEFAULT TRUE,
    post_comments BOOLEAN NOT NULL DEFAULT TRUE,
    comment_replies BOOLEAN NOT NULL DEFAULT TRUE,
    appointment_reminders BOOLEAN NOT NULL DEFAULT TRUE,
    prescription_updates BOOLEAN NOT NULL DEFAULT TRUE,
    lab_results BOOLEAN NOT NULL DEFAULT TRUE,
    medication_reminders BOOLEAN NOT NULL DEFAULT TRUE,
    security_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    login_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    password_change_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    system_updates BOOLEAN NOT NULL DEFAULT TRUE,
    maintenance_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_preferences_user_id ON user_notification_preferences(user_id);

-- Migration V4: Privacy Settings
CREATE TABLE IF NOT EXISTS user_privacy_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    profile_visibility VARCHAR(20) NOT NULL DEFAULT 'public',
    show_email BOOLEAN NOT NULL DEFAULT FALSE,
    show_phone BOOLEAN NOT NULL DEFAULT FALSE,
    show_address BOOLEAN NOT NULL DEFAULT FALSE,
    show_medical_history BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_privacy_settings_user_id ON user_privacy_settings(user_id);

-- ============================================
-- PART 2: TEST DATA INSERTION
-- ============================================

-- Note: BCrypt password hash for "password123" (strength 12)
-- You can generate your own at: https://bcrypt-generator.com/
-- Or use the application's password encoder

-- Clear existing test data (optional - comment out if you want to keep existing data)
-- DELETE FROM appointment_entity;
-- DELETE FROM medical_record;
-- DELETE FROM patient;
-- DELETE FROM healthcare_provider;
-- DELETE FROM users WHERE role != 'ADMIN';

-- ============================================
-- 1. USERS (Base Table)
-- ============================================

-- Admin User (if not exists)
INSERT INTO users (username, password, role, first_name, last_name, email, account_status, registration_date, two_factor_enabled)
VALUES 
    ('admin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyY5Y5Y5Y5Y5Y', 'ADMIN', 'System', 'Administrator', 'admin@mediconnect.com', 'ACTIVE', CURRENT_TIMESTAMP, false)
ON CONFLICT (username) DO NOTHING;

-- Patient Users
INSERT INTO users (username, password, role, first_name, last_name, email, gender, phone_number, city, state, country, account_status, registration_date, two_factor_enabled)
VALUES 
    ('patient1', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyY5Y5Y5Y5Y5Y', 'PATIENT', 'Ahmed', 'Al-Mansour', 'ahmed.patient@email.com', 'MALE', '+962791234567', 'Amman', 'Amman', 'Jordan', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '30 days', false),
    ('patient2', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyY5Y5Y5Y5Y5Y', 'PATIENT', 'Sara', 'Al-Zahra', 'sara.patient@email.com', 'FEMALE', '+962792345678', 'Irbid', 'Irbid', 'Jordan', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '20 days', false),
    ('patient3', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyY5Y5Y5Y5Y5Y', 'PATIENT', 'Mohammed', 'Al-Hashimi', 'mohammed.patient@email.com', 'MALE', '+962793456789', 'Aqaba', 'Aqaba', 'Jordan', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '15 days', false),
    ('patient4', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyY5Y5Y5Y5Y5Y', 'PATIENT', 'Layla', 'Al-Rashid', 'layla.patient@email.com', 'FEMALE', '+962794567890', 'Amman', 'Amman', 'Jordan', 'PENDING', CURRENT_TIMESTAMP - INTERVAL '5 days', false),
    ('patient5', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyY5Y5Y5Y5Y5Y', 'PATIENT', 'Omar', 'Al-Farouk', 'omar.patient@email.com', 'MALE', '+962795678901', 'Amman', 'Amman', 'Jordan', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '10 days', false)
ON CONFLICT (username) DO NOTHING;

-- Healthcare Provider Users (Doctors)
INSERT INTO users (username, password, role, first_name, last_name, email, gender, phone_number, city, state, country, account_status, registration_date, two_factor_enabled)
VALUES 
    ('doctor1', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyY5Y5Y5Y5Y5Y', 'HEALTHPROVIDER', 'Dr. Khalid', 'Al-Mahmoud', 'khalid.doctor@email.com', 'MALE', '+962796789012', 'Amman', 'Amman', 'Jordan', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '60 days', false),
    ('doctor2', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyY5Y5Y5Y5Y5Y', 'HEALTHPROVIDER', 'Dr. Fatima', 'Al-Said', 'fatima.doctor@email.com', 'FEMALE', '+962797890123', 'Amman', 'Amman', 'Jordan', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '50 days', false),
    ('doctor3', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyY5Y5Y5Y5Y5Y', 'HEALTHPROVIDER', 'Dr. Youssef', 'Al-Karim', 'youssef.doctor@email.com', 'MALE', '+962798901234', 'Irbid', 'Irbid', 'Jordan', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '40 days', false),
    ('doctor4', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyY5Y5Y5Y5Y5Y', 'HEALTHPROVIDER', 'Dr. Nour', 'Al-Hassan', 'nour.doctor@email.com', 'FEMALE', '+962799012345', 'Amman', 'Amman', 'Jordan', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '35 days', false),
    ('doctor5', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyY5Y5Y5Y5Y5Y', 'HEALTHPROVIDER', 'Dr. Rami', 'Al-Ahmad', 'rami.doctor@email.com', 'MALE', '+962790123456', 'Aqaba', 'Aqaba', 'Jordan', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '25 days', false)
ON CONFLICT (username) DO NOTHING;

-- ============================================
-- 2. PATIENTS (Extends Users)
-- ============================================

INSERT INTO patient (id, blood_type, height, weight, allergies, medical_conditions, previous_surgeries, insurance_provider, insurance_number)
SELECT 
    u.id,
    'O_POSITIVE',
    175.0,
    75.0,
    'Peanuts, Shellfish',
    'Type 2 Diabetes, Hypertension',
    'Appendectomy (2015)',
    'Jordan Insurance Company',
    'JIC-2024-001'
FROM users u WHERE u.username = 'patient1'
ON CONFLICT (id) DO NOTHING;

INSERT INTO patient (id, blood_type, height, weight, allergies, medical_conditions, previous_surgeries, insurance_provider, insurance_number)
SELECT 
    u.id,
    'A_POSITIVE',
    165.0,
    60.0,
    'Penicillin',
    'Asthma',
    NULL,
    'Medicare Jordan',
    'MJ-2024-002'
FROM users u WHERE u.username = 'patient2'
ON CONFLICT (id) DO NOTHING;

INSERT INTO patient (id, blood_type, height, weight, allergies, medical_conditions, previous_surgeries, insurance_provider, insurance_number)
SELECT 
    u.id,
    'B_POSITIVE',
    180.0,
    85.0,
    NULL,
    NULL,
    NULL,
    'National Health Insurance',
    'NHI-2024-003'
FROM users u WHERE u.username = 'patient3'
ON CONFLICT (id) DO NOTHING;

INSERT INTO patient (id, blood_type, height, weight, allergies, medical_conditions, previous_surgeries, insurance_provider, insurance_number)
SELECT 
    u.id,
    'AB_POSITIVE',
    160.0,
    55.0,
    'Latex',
    NULL,
    NULL,
    'Jordan Insurance Company',
    'JIC-2024-004'
FROM users u WHERE u.username = 'patient4'
ON CONFLICT (id) DO NOTHING;

INSERT INTO patient (id, blood_type, height, weight, allergies, medical_conditions, previous_surgeries, insurance_provider, insurance_number)
SELECT 
    u.id,
    'O_NEGATIVE',
    170.0,
    70.0,
    NULL,
    'Hypertension',
    'Knee Surgery (2020)',
    'Medicare Jordan',
    'MJ-2024-005'
FROM users u WHERE u.username = 'patient5'
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 3. HEALTHCARE PROVIDERS (Extends Users)
-- ============================================

-- Doctor 1: Cardiologist
INSERT INTO healthcare_provider (id, license_number, clinic_name, bio, consultation_fee, available_time_start, available_time_end, admin_flagged)
SELECT 
    u.id,
    'MD-CARD-2024-001',
    'Amman Heart Center',
    'Experienced cardiologist with 15 years of practice. Specializes in heart disease prevention and treatment.',
    50.0,
    '09:00',
    '17:00',
    false
FROM users u WHERE u.username = 'doctor1'
ON CONFLICT (id) DO NOTHING;

-- Doctor 2: Psychiatrist (for video calls)
INSERT INTO healthcare_provider (id, license_number, clinic_name, bio, consultation_fee, available_time_start, available_time_end, admin_flagged)
SELECT 
    u.id,
    'MD-PSY-2024-002',
    'Mental Health Clinic Amman',
    'Licensed psychiatrist specializing in anxiety, depression, and mental health counseling. Offers online consultations.',
    75.0,
    '10:00',
    '18:00',
    false
FROM users u WHERE u.username = 'doctor2'
ON CONFLICT (id) DO NOTHING;

-- Doctor 3: Neurologist
INSERT INTO healthcare_provider (id, license_number, clinic_name, bio, consultation_fee, available_time_start, available_time_end, admin_flagged)
SELECT 
    u.id,
    'MD-NEURO-2024-003',
    'Irbid Neurology Center',
    'Expert neurologist treating migraines, epilepsy, and neurological disorders.',
    60.0,
    '08:00',
    '16:00',
    false
FROM users u WHERE u.username = 'doctor3'
ON CONFLICT (id) DO NOTHING;

-- Doctor 4: Dermatologist
INSERT INTO healthcare_provider (id, license_number, clinic_name, bio, consultation_fee, available_time_start, available_time_end, admin_flagged)
SELECT 
    u.id,
    'MD-DERM-2024-004',
    'Skin Care Clinic Amman',
    'Board-certified dermatologist with expertise in skin conditions, acne treatment, and cosmetic dermatology.',
    45.0,
    '09:00',
    '17:00',
    false
FROM users u WHERE u.username = 'doctor4'
ON CONFLICT (id) DO NOTHING;

-- Doctor 5: Family Medicine
INSERT INTO healthcare_provider (id, license_number, clinic_name, bio, consultation_fee, available_time_start, available_time_end, admin_flagged)
SELECT 
    u.id,
    'MD-FAM-2024-005',
    'Aqaba Family Health Center',
    'General practitioner providing comprehensive family healthcare services.',
    35.0,
    '08:00',
    '15:00',
    false
FROM users u WHERE u.username = 'doctor5'
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 4. PROVIDER SPECIALIZATIONS
-- ============================================

-- Create table if it doesn't exist
-- Note: If this fails, you may need to run your Spring Boot app once to let Hibernate create the tables
DO $$
BEGIN
    -- Create provider_specialization table
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'provider_specialization') THEN
        EXECUTE 'CREATE TABLE provider_specialization (
            healthcare_provider_id BIGINT NOT NULL REFERENCES healthcare_provider(id) ON DELETE CASCADE,
            specialization VARCHAR(255) NOT NULL,
            PRIMARY KEY (healthcare_provider_id, specialization)
        )';
    END IF;
END $$;

-- Insert specializations (only if table exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'provider_specialization') THEN
        INSERT INTO provider_specialization (healthcare_provider_id, specialization)
        SELECT id, 'CARDIOLOGY' FROM healthcare_provider WHERE license_number = 'MD-CARD-2024-001'
        ON CONFLICT DO NOTHING;
        
        INSERT INTO provider_specialization (healthcare_provider_id, specialization)
        SELECT id, 'PSYCHIATRY' FROM healthcare_provider WHERE license_number = 'MD-PSY-2024-002'
        ON CONFLICT DO NOTHING;
        
        INSERT INTO provider_specialization (healthcare_provider_id, specialization)
        SELECT id, 'NEUROLOGY' FROM healthcare_provider WHERE license_number = 'MD-NEURO-2024-003'
        ON CONFLICT DO NOTHING;
        
        INSERT INTO provider_specialization (healthcare_provider_id, specialization)
        SELECT id, 'DERMATOLOGY' FROM healthcare_provider WHERE license_number = 'MD-DERM-2024-004'
        ON CONFLICT DO NOTHING;
        
        INSERT INTO provider_specialization (healthcare_provider_id, specialization)
        SELECT id, 'FAMILY_MEDICINE' FROM healthcare_provider WHERE license_number = 'MD-FAM-2024-005'
        ON CONFLICT DO NOTHING;
    ELSE
        RAISE NOTICE 'Table provider_specialization does not exist. Please run your Spring Boot app once to create tables, or create the table manually.';
    END IF;
END $$;

-- ============================================
-- 5. PROVIDER INSURANCE ACCEPTED
-- ============================================

INSERT INTO provider_insurance (healthcare_provider_id, insurance_accepted)
SELECT id, 'Jordan Insurance Company' FROM healthcare_provider WHERE license_number = 'MD-CARD-2024-001'
ON CONFLICT DO NOTHING;

INSERT INTO provider_insurance (healthcare_provider_id, insurance_accepted)
SELECT id, 'Medicare Jordan' FROM healthcare_provider WHERE license_number = 'MD-CARD-2024-001'
ON CONFLICT DO NOTHING;

INSERT INTO provider_insurance (healthcare_provider_id, insurance_accepted)
SELECT id, 'Jordan Insurance Company' FROM healthcare_provider WHERE license_number = 'MD-PSY-2024-002'
ON CONFLICT DO NOTHING;

INSERT INTO provider_insurance (healthcare_provider_id, insurance_accepted)
SELECT id, 'National Health Insurance' FROM healthcare_provider WHERE license_number = 'MD-PSY-2024-002'
ON CONFLICT DO NOTHING;

INSERT INTO provider_insurance (healthcare_provider_id, insurance_accepted)
SELECT id, 'Medicare Jordan' FROM healthcare_provider WHERE license_number = 'MD-NEURO-2024-003'
ON CONFLICT DO NOTHING;

INSERT INTO provider_insurance (healthcare_provider_id, insurance_accepted)
SELECT id, 'Jordan Insurance Company' FROM healthcare_provider WHERE license_number = 'MD-DERM-2024-004'
ON CONFLICT DO NOTHING;

INSERT INTO provider_insurance (healthcare_provider_id, insurance_accepted)
SELECT id, 'Medicare Jordan' FROM healthcare_provider WHERE license_number = 'MD-DERM-2024-004'
ON CONFLICT DO NOTHING;

INSERT INTO provider_insurance (healthcare_provider_id, insurance_accepted)
SELECT id, 'National Health Insurance' FROM healthcare_provider WHERE license_number = 'MD-FAM-2024-005'
ON CONFLICT DO NOTHING;

-- ============================================
-- 6. PROVIDER AVAILABILITY
-- ============================================

INSERT INTO provider_availability (healthcare_provider_id, available_days)
SELECT id, 'MONDAY' FROM healthcare_provider WHERE license_number = 'MD-CARD-2024-001'
ON CONFLICT DO NOTHING;

INSERT INTO provider_availability (healthcare_provider_id, available_days)
SELECT id, 'TUESDAY' FROM healthcare_provider WHERE license_number = 'MD-CARD-2024-001'
ON CONFLICT DO NOTHING;

INSERT INTO provider_availability (healthcare_provider_id, available_days)
SELECT id, 'WEDNESDAY' FROM healthcare_provider WHERE license_number = 'MD-CARD-2024-001'
ON CONFLICT DO NOTHING;

INSERT INTO provider_availability (healthcare_provider_id, available_days)
SELECT id, 'THURSDAY' FROM healthcare_provider WHERE license_number = 'MD-CARD-2024-001'
ON CONFLICT DO NOTHING;

INSERT INTO provider_availability (healthcare_provider_id, available_days)
SELECT id, 'MONDAY' FROM healthcare_provider WHERE license_number = 'MD-PSY-2024-002'
ON CONFLICT DO NOTHING;

INSERT INTO provider_availability (healthcare_provider_id, available_days)
SELECT id, 'TUESDAY' FROM healthcare_provider WHERE license_number = 'MD-PSY-2024-002'
ON CONFLICT DO NOTHING;

INSERT INTO provider_availability (healthcare_provider_id, available_days)
SELECT id, 'WEDNESDAY' FROM healthcare_provider WHERE license_number = 'MD-PSY-2024-002'
ON CONFLICT DO NOTHING;

INSERT INTO provider_availability (healthcare_provider_id, available_days)
SELECT id, 'THURSDAY' FROM healthcare_provider WHERE license_number = 'MD-PSY-2024-002'
ON CONFLICT DO NOTHING;

INSERT INTO provider_availability (healthcare_provider_id, available_days)
SELECT id, 'SUNDAY' FROM healthcare_provider WHERE license_number = 'MD-NEURO-2024-003'
ON CONFLICT DO NOTHING;

INSERT INTO provider_availability (healthcare_provider_id, available_days)
SELECT id, 'MONDAY' FROM healthcare_provider WHERE license_number = 'MD-NEURO-2024-003'
ON CONFLICT DO NOTHING;

INSERT INTO provider_availability (healthcare_provider_id, available_days)
SELECT id, 'TUESDAY' FROM healthcare_provider WHERE license_number = 'MD-NEURO-2024-003'
ON CONFLICT DO NOTHING;

INSERT INTO provider_availability (healthcare_provider_id, available_days)
SELECT id, 'WEDNESDAY' FROM healthcare_provider WHERE license_number = 'MD-NEURO-2024-003'
ON CONFLICT DO NOTHING;

-- ============================================
-- 7. APPOINTMENTS (Various Statuses & Times)
-- ============================================

-- PENDING Appointment (Future - with medical records sharing)
INSERT INTO appointment_entity (patient_id, provider_id, appointment_date_time, status, type, reason, duration_minutes, share_medical_records, is_video_call, is_call_active, reminder_24h_sent, created_at, updated_at)
SELECT 
    p.id,
    hp.id,
    (CURRENT_TIMESTAMP + INTERVAL '3 days' + INTERVAL '11 hours' + INTERVAL '30 minutes')::TIMESTAMP,
    'PENDING',
    'CONSULTATION',
    'Regular checkup for annual health assessment',
    30,
    true,
    false,
    false,
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM patient p
CROSS JOIN healthcare_provider hp
WHERE p.id = (SELECT id FROM users WHERE username = 'patient1')
  AND hp.id = (SELECT id FROM users WHERE username = 'doctor1')
ON CONFLICT DO NOTHING;

-- CONFIRMED Appointment (Future - without medical records)
INSERT INTO appointment_entity (patient_id, provider_id, appointment_date_time, status, type, reason, duration_minutes, share_medical_records, is_video_call, is_call_active, reminder_24h_sent, created_at, updated_at)
SELECT 
    p.id,
    hp.id,
    (CURRENT_TIMESTAMP + INTERVAL '5 days' + INTERVAL '14 hours')::TIMESTAMP,
    'CONFIRMED',
    'CONSULTATION',
    'Follow-up appointment for diabetes management',
    30,
    false,
    false,
    false,
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM patient p
CROSS JOIN healthcare_provider hp
WHERE p.id = (SELECT id FROM users WHERE username = 'patient1')
  AND hp.id = (SELECT id FROM users WHERE username = 'doctor1')
ON CONFLICT DO NOTHING;

-- Video Call Appointment (Psychiatry - Future)
INSERT INTO appointment_entity (patient_id, provider_id, appointment_date_time, status, type, reason, duration_minutes, share_medical_records, is_video_call, is_call_active, reminder_24h_sent, created_at, updated_at)
SELECT 
    p.id,
    hp.id,
    (CURRENT_TIMESTAMP + INTERVAL '7 days' + INTERVAL '10 hours')::TIMESTAMP,
    'PENDING',
    'CONSULTATION',
    'Mental health consultation',
    30,
    true,
    true,
    false,
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM patient p
CROSS JOIN healthcare_provider hp
WHERE p.id = (SELECT id FROM users WHERE username = 'patient2')
  AND hp.id = (SELECT id FROM users WHERE username = 'doctor2')
ON CONFLICT DO NOTHING;

-- RESCHEDULED Appointment
INSERT INTO appointment_entity (patient_id, provider_id, appointment_date_time, status, type, reason, duration_minutes, share_medical_records, is_video_call, is_call_active, reminder_24h_sent, created_at, updated_at)
SELECT 
    p.id,
    hp.id,
    (CURRENT_TIMESTAMP + INTERVAL '10 days' + INTERVAL '15 hours')::TIMESTAMP,
    'RESCHEDULED',
    'CONSULTATION',
    'Rescheduled appointment',
    30,
    false,
    false,
    false,
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM patient p
CROSS JOIN healthcare_provider hp
WHERE p.id = (SELECT id FROM users WHERE username = 'patient3')
  AND hp.id = (SELECT id FROM users WHERE username = 'doctor3')
ON CONFLICT DO NOTHING;

-- COMPLETED Appointment (Past)
INSERT INTO appointment_entity (patient_id, provider_id, appointment_date_time, status, type, reason, duration_minutes, share_medical_records, is_video_call, is_call_active, reminder_24h_sent, created_at, updated_at)
SELECT 
    p.id,
    hp.id,
    (CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '9 hours')::TIMESTAMP,
    'COMPLETED',
    'CONSULTATION',
    'Completed checkup',
    30,
    true,
    false,
    false,
    true,
    CURRENT_TIMESTAMP - INTERVAL '10 days',
    CURRENT_TIMESTAMP - INTERVAL '5 days'
FROM patient p
CROSS JOIN healthcare_provider hp
WHERE p.id = (SELECT id FROM users WHERE username = 'patient1')
  AND hp.id = (SELECT id FROM users WHERE username = 'doctor4')
ON CONFLICT DO NOTHING;

-- CANCELLED Appointment
INSERT INTO appointment_entity (patient_id, provider_id, appointment_date_time, status, type, reason, duration_minutes, share_medical_records, is_video_call, is_call_active, reminder_24h_sent, created_at, updated_at)
SELECT 
    p.id,
    hp.id,
    (CURRENT_TIMESTAMP + INTERVAL '2 days' + INTERVAL '13 hours')::TIMESTAMP,
    'CANCELLED',
    'CONSULTATION',
    'Cancelled appointment',
    30,
    false,
    false,
    false,
    false,
    CURRENT_TIMESTAMP - INTERVAL '3 days',
    CURRENT_TIMESTAMP - INTERVAL '1 day'
FROM patient p
CROSS JOIN healthcare_provider hp
WHERE p.id = (SELECT id FROM users WHERE username = 'patient5')
  AND hp.id = (SELECT id FROM users WHERE username = 'doctor5')
ON CONFLICT DO NOTHING;

-- ============================================
-- 8. NOTIFICATION PREFERENCES
-- ============================================

INSERT INTO user_notification_preferences (user_id, email_notifications, push_notifications, appointment_reminders, security_alerts)
SELECT id, true, true, true, true FROM users WHERE username = 'patient1'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO user_notification_preferences (user_id, email_notifications, push_notifications, appointment_reminders, security_alerts)
SELECT id, true, true, true, true FROM users WHERE username = 'doctor1'
ON CONFLICT (user_id) DO NOTHING;

-- ============================================
-- 9. PRIVACY SETTINGS
-- ============================================

INSERT INTO user_privacy_settings (user_id, profile_visibility, show_email, show_phone)
SELECT id, 'public', false, false FROM users WHERE username = 'patient1'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO user_privacy_settings (user_id, profile_visibility, show_email, show_phone)
SELECT id, 'public', true, true FROM users WHERE username = 'doctor1'
ON CONFLICT (user_id) DO NOTHING;

-- ============================================
-- VERIFICATION QUERIES
-- ============================================

-- Check inserted data
SELECT 'Users' AS table_name, COUNT(*) AS count FROM users
UNION ALL
SELECT 'Patients', COUNT(*) FROM patient
UNION ALL
SELECT 'Healthcare Providers', COUNT(*) FROM healthcare_provider
UNION ALL
SELECT 'Appointments', COUNT(*) FROM appointment_entity
UNION ALL
SELECT 'Provider Specializations', COUNT(*) FROM provider_specialization
UNION ALL
SELECT 'Provider Insurance', COUNT(*) FROM provider_insurance
UNION ALL
SELECT 'Provider Availability', COUNT(*) FROM provider_availability
ORDER BY table_name;

-- ============================================
-- TEST CREDENTIALS SUMMARY
-- ============================================
-- All users have password: password123
-- 
-- Admin:
--   Username: admin
--   Email: admin@mediconnect.com
--
-- Patients:
--   patient1 / ahmed.patient@email.com
--   patient2 / sara.patient@email.com
--   patient3 / mohammed.patient@email.com
--   patient4 / layla.patient@email.com (PENDING status)
--   patient5 / omar.patient@email.com
--
-- Doctors:
--   doctor1 / khalid.doctor@email.com (Cardiologist)
--   doctor2 / fatima.doctor@email.com (Psychiatrist - for video calls)
--   doctor3 / youssef.doctor@email.com (Neurologist)
--   doctor4 / nour.doctor@email.com (Dermatologist)
--   doctor5 / rami.doctor@email.com (Family Medicine)
-- ============================================

