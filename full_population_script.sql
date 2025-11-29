-- ============================================
-- COMPLETE DATABASE POPULATION SCRIPT
-- ============================================
-- This script includes:
-- 1. Core Users (Admins, Patients, Doctors)
-- 2. Patient Data (Profiles, Medical History)
-- 3. Provider Data (Profiles, Specializations, Availability)
-- 4. Appointments (Various statuses)
-- 5. Medical Records & Medications
-- 6. Social Media (Posts, Likes, Comments)
-- 7. Reviews & Ratings
-- ============================================

-- ============================================
-- 1. USERS & PROFILES
-- ============================================

-- Admin User
---das
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

-- Healthcare Provider Users
INSERT INTO users (username, password, role, first_name, last_name, email, gender, phone_number, city, state, country, account_status, registration_date, two_factor_enabled)
VALUES 
    ('doctor1', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyY5Y5Y5Y5Y5Y', 'HEALTHPROVIDER', 'Dr. Khalid', 'Al-Mahmoud', 'khalid.doctor@email.com', 'MALE', '+962796789012', 'Amman', 'Amman', 'Jordan', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '60 days', false),
    ('doctor2', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyY5Y5Y5Y5Y5Y', 'HEALTHPROVIDER', 'Dr. Fatima', 'Al-Said', 'fatima.doctor@email.com', 'FEMALE', '+962797890123', 'Amman', 'Amman', 'Jordan', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '50 days', false),
    ('doctor3', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyY5Y5Y5Y5Y5Y', 'HEALTHPROVIDER', 'Dr. Youssef', 'Al-Karim', 'youssef.doctor@email.com', 'MALE', '+962798901234', 'Irbid', 'Irbid', 'Jordan', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '40 days', false),
    ('doctor4', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyY5Y5Y5Y5Y5Y', 'HEALTHPROVIDER', 'Dr. Nour', 'Al-Hassan', 'nour.doctor@email.com', 'FEMALE', '+962799012345', 'Amman', 'Amman', 'Jordan', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '35 days', false),
    ('doctor5', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyY5Y5Y5Y5Y5Y', 'HEALTHPROVIDER', 'Dr. Rami', 'Al-Ahmad', 'rami.doctor@email.com', 'MALE', '+962790123456', 'Aqaba', 'Aqaba', 'Jordan', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '25 days', false)
ON CONFLICT (username) DO NOTHING;

-- Patient Details
INSERT INTO patient (id, blood_type, height, weight, allergies, medical_conditions, previous_surgeries, insurance_provider, insurance_number)
SELECT u.id, 'O_POSITIVE', 175.0, 75.0, 'Peanuts, Shellfish', 'Type 2 Diabetes, Hypertension', 'Appendectomy (2015)', 'Jordan Insurance Company', 'JIC-2024-001'
FROM users u WHERE u.username = 'patient1'
ON CONFLICT (id) DO NOTHING;

INSERT INTO patient (id, blood_type, height, weight, allergies, medical_conditions, previous_surgeries, insurance_provider, insurance_number)
SELECT u.id, 'A_POSITIVE', 165.0, 60.0, 'Penicillin', 'Asthma', NULL, 'Medicare Jordan', 'MJ-2024-002'
FROM users u WHERE u.username = 'patient2'
ON CONFLICT (id) DO NOTHING;

INSERT INTO patient (id, blood_type, height, weight, allergies, medical_conditions, previous_surgeries, insurance_provider, insurance_number)
SELECT u.id, 'B_POSITIVE', 180.0, 85.0, NULL, NULL, NULL, 'National Health Insurance', 'NHI-2024-003'
FROM users u WHERE u.username = 'patient3'
ON CONFLICT (id) DO NOTHING;

INSERT INTO patient (id, blood_type, height, weight, allergies, medical_conditions, previous_surgeries, insurance_provider, insurance_number)
SELECT u.id, 'AB_POSITIVE', 160.0, 55.0, 'Latex', NULL, NULL, 'Jordan Insurance Company', 'JIC-2024-004'
FROM users u WHERE u.username = 'patient4'
ON CONFLICT (id) DO NOTHING;

INSERT INTO patient (id, blood_type, height, weight, allergies, medical_conditions, previous_surgeries, insurance_provider, insurance_number)
SELECT u.id, 'O_NEGATIVE', 170.0, 70.0, NULL, 'Hypertension', 'Knee Surgery (2020)', 'Medicare Jordan', 'MJ-2024-005'
FROM users u WHERE u.username = 'patient5'
ON CONFLICT (id) DO NOTHING;

-- Provider Details
INSERT INTO healthcare_provider (id, license_number, clinic_name, bio, consultation_fee, available_time_start, available_time_end, admin_flagged)
SELECT u.id, 'MD-CARD-2024-001', 'Amman Heart Center', 'Experienced cardiologist with 15 years of practice.', 50.0, '09:00', '17:00', false
FROM users u WHERE u.username = 'doctor1'
ON CONFLICT (id) DO NOTHING;

INSERT INTO healthcare_provider (id, license_number, clinic_name, bio, consultation_fee, available_time_start, available_time_end, admin_flagged)
SELECT u.id, 'MD-PSY-2024-002', 'Mental Health Clinic Amman', 'Licensed psychiatrist specializing in anxiety and depression.', 75.0, '10:00', '18:00', false
FROM users u WHERE u.username = 'doctor2'
ON CONFLICT (id) DO NOTHING;

INSERT INTO healthcare_provider (id, license_number, clinic_name, bio, consultation_fee, available_time_start, available_time_end, admin_flagged)
SELECT u.id, 'MD-NEURO-2024-003', 'Irbid Neurology Center', 'Expert neurologist treating migraines and epilepsy.', 60.0, '08:00', '16:00', false
FROM users u WHERE u.username = 'doctor3'
ON CONFLICT (id) DO NOTHING;

INSERT INTO healthcare_provider (id, license_number, clinic_name, bio, consultation_fee, available_time_start, available_time_end, admin_flagged)
SELECT u.id, 'MD-DERM-2024-004', 'Skin Care Clinic Amman', 'Board-certified dermatologist.', 45.0, '09:00', '17:00', false
FROM users u WHERE u.username = 'doctor4'
ON CONFLICT (id) DO NOTHING;

INSERT INTO healthcare_provider (id, license_number, clinic_name, bio, consultation_fee, available_time_start, available_time_end, admin_flagged)
SELECT u.id, 'MD-FAM-2024-005', 'Aqaba Family Health Center', 'General practitioner providing comprehensive family healthcare.', 35.0, '08:00', '15:00', false
FROM users u WHERE u.username = 'doctor5'
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 2. PROVIDER CONFIGURATION
-- ============================================

-- Specializations
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'provider_specialization') THEN
        INSERT INTO provider_specialization (healthcare_provider_id, specialization)
        SELECT id, 'CARDIOLOGY' FROM healthcare_provider WHERE license_number = 'MD-CARD-2024-001' ON CONFLICT DO NOTHING;
        INSERT INTO provider_specialization (healthcare_provider_id, specialization)
        SELECT id, 'PSYCHIATRY' FROM healthcare_provider WHERE license_number = 'MD-PSY-2024-002' ON CONFLICT DO NOTHING;
        INSERT INTO provider_specialization (healthcare_provider_id, specialization)
        SELECT id, 'NEUROLOGY' FROM healthcare_provider WHERE license_number = 'MD-NEURO-2024-003' ON CONFLICT DO NOTHING;
        INSERT INTO provider_specialization (healthcare_provider_id, specialization)
        SELECT id, 'DERMATOLOGY' FROM healthcare_provider WHERE license_number = 'MD-DERM-2024-004' ON CONFLICT DO NOTHING;
        INSERT INTO provider_specialization (healthcare_provider_id, specialization)
        SELECT id, 'FAMILY_MEDICINE' FROM healthcare_provider WHERE license_number = 'MD-FAM-2024-005' ON CONFLICT DO NOTHING;
    END IF;
END $$;

-- Insurance Accepted
INSERT INTO provider_insurance (healthcare_provider_id, insurance_accepted)
SELECT id, 'Jordan Insurance Company' FROM healthcare_provider WHERE license_number = 'MD-CARD-2024-001' ON CONFLICT DO NOTHING;

-- Availability
INSERT INTO provider_availability (healthcare_provider_id, available_days)
SELECT id, 'MONDAY' FROM healthcare_provider WHERE license_number = 'MD-CARD-2024-001' ON CONFLICT DO NOTHING;

-- ============================================
-- 3. APPOINTMENTS
-- ============================================

-- Pending Appointment
INSERT INTO appointment_entity (patient_id, provider_id, appointment_date_time, status, type, reason, duration_minutes, share_medical_records, is_video_call, is_call_active, reminder_24h_sent, created_at, updated_at)
SELECT p.id, hp.id, (CURRENT_TIMESTAMP + INTERVAL '3 days')::TIMESTAMP, 'PENDING', 'CONSULTATION', 'Regular checkup', 30, true, false, false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p, healthcare_provider hp
WHERE p.id = (SELECT id FROM users WHERE username = 'patient1') AND hp.id = (SELECT id FROM users WHERE username = 'doctor1')
ON CONFLICT DO NOTHING;

-- Completed Appointment (for Medical Records)
INSERT INTO appointment_entity (patient_id, provider_id, appointment_date_time, status, type, reason, duration_minutes, share_medical_records, is_video_call, is_call_active, reminder_24h_sent, created_at, updated_at)
SELECT p.id, hp.id, (CURRENT_TIMESTAMP - INTERVAL '5 days')::TIMESTAMP, 'COMPLETED', 'CONSULTATION', 'Diabetes checkup', 30, true, false, false, true, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '5 days'
FROM patient p, healthcare_provider hp
WHERE p.id = (SELECT id FROM users WHERE username = 'patient1') AND hp.id = (SELECT id FROM users WHERE username = 'doctor4')
ON CONFLICT DO NOTHING;

-- ============================================
-- 4. MEDICAL RECORDS & MEDICATIONS
-- ============================================

-- Medical Record for Completed Appointment
INSERT INTO medical_record (patient_id, provider_id, appointment_id, visit_date, diagnosis, treatment, prescription, symptoms, notes, temperature, blood_pressure, heart_rate, weight, height, created_at)
SELECT 
    p.id,
    hp.id,
    ae.id,
    ae.appointment_date_time,
    'Type 2 Diabetes',
    'Metformin 500mg twice daily, Diet modification',
    'Metformin 500mg',
    'Increased thirst, frequent urination',
    'Patient needs to monitor blood sugar levels daily',
    37.2,
    '130/85',
    78,
    75.5,
    175.0,
    CURRENT_TIMESTAMP - INTERVAL '5 days'
FROM patient p
JOIN healthcare_provider hp ON hp.id = (SELECT id FROM users WHERE username = 'doctor4')
JOIN appointment_entity ae ON ae.patient_id = p.id AND ae.provider_id = hp.id AND ae.status = 'COMPLETED'
WHERE p.id = (SELECT id FROM users WHERE username = 'patient1')
ON CONFLICT DO NOTHING;

-- Medications
INSERT INTO medication (patient_id, medication_name, medication_dosage, medication_frequency, medication_start_date, in_use)
SELECT id, 'Metformin', '500mg', 'Twice daily', CURRENT_DATE - INTERVAL '6 months', true
FROM users WHERE username = 'patient1'
ON CONFLICT DO NOTHING;

INSERT INTO medication (patient_id, medication_name, medication_dosage, medication_frequency, medication_start_date, in_use)
SELECT id, 'Lisinopril', '10mg', 'Once daily', CURRENT_DATE - INTERVAL '1 year', true
FROM users WHERE username = 'patient1'
ON CONFLICT DO NOTHING;

-- ============================================
-- 5. REVIEWS & SOCIAL MEDIA
-- ============================================

-- Doctor Review
INSERT INTO doctor_review (appointment_id, patient_id, doctor_id, rating, notes, created_at)
SELECT ae.id, p.id, hp.id, 5, 'Dr. Nour was very professional and explained everything clearly.', CURRENT_TIMESTAMP - INTERVAL '4 days'
FROM appointment_entity ae
JOIN patient p ON ae.patient_id = p.id
JOIN healthcare_provider hp ON ae.provider_id = hp.id
WHERE ae.status = 'COMPLETED' AND p.id = (SELECT id FROM users WHERE username = 'patient1')
ON CONFLICT DO NOTHING;

-- Medical Posts
INSERT INTO medical_post (provider_id, content, privacy, created_at, admin_flagged)
SELECT id, 'Heart health is crucial! Remember to exercise at least 30 minutes a day.', 'PUBLIC', CURRENT_TIMESTAMP - INTERVAL '2 days', false
FROM users WHERE username = 'doctor1'
ON CONFLICT DO NOTHING;

-- Post Likes
INSERT INTO medical_post_like (post_id, like_giver_id, created_at)
SELECT mp.id, u.id, CURRENT_TIMESTAMP - INTERVAL '1 day'
FROM medical_post mp
CROSS JOIN users u
WHERE mp.provider_id = (SELECT id FROM users WHERE username = 'doctor1')
AND u.username = 'patient1'
ON CONFLICT DO NOTHING;

-- Post Comments
INSERT INTO medical_post_comment (post_id, commenter_id, content, created_at)
SELECT mp.id, u.id, 'Great advice, doctor!', CURRENT_TIMESTAMP - INTERVAL '1 day'
FROM medical_post mp
CROSS JOIN users u
WHERE mp.provider_id = (SELECT id FROM users WHERE username = 'doctor1')
AND u.username = 'patient1'
ON CONFLICT DO NOTHING;
