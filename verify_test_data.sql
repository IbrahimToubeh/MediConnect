-- ============================================
-- VERIFICATION SCRIPT - Check Test Data
-- ============================================
-- Run this after executing complete_test_data.sql
-- to verify all data was inserted correctly
-- ============================================

-- ============================================
-- 1. CHECK TABLE COUNTS
-- ============================================
SELECT '=== TABLE RECORD COUNTS ===' AS info;

SELECT 
    'users' AS table_name, 
    COUNT(*) AS record_count 
FROM users
UNION ALL
SELECT 'patient', COUNT(*) FROM patient
UNION ALL
SELECT 'healthcare_provider', COUNT(*) FROM healthcare_provider
UNION ALL
SELECT 'appointment_entity', COUNT(*) FROM appointment_entity
UNION ALL
SELECT 'provider_specialization', COUNT(*) FROM provider_specialization
UNION ALL
SELECT 'provider_insurance', COUNT(*) FROM provider_insurance
UNION ALL
SELECT 'provider_availability', COUNT(*) FROM provider_availability
UNION ALL
SELECT 'user_notification_preferences', COUNT(*) FROM user_notification_preferences
UNION ALL
SELECT 'user_privacy_settings', COUNT(*) FROM user_privacy_settings
UNION ALL
SELECT 'login_sessions', COUNT(*) FROM login_sessions
UNION ALL
SELECT 'account_activities', COUNT(*) FROM account_activities
ORDER BY table_name;

-- ============================================
-- 2. CHECK USERS (Should have 11: 1 admin + 5 patients + 5 doctors)
-- ============================================
SELECT '=== USERS VERIFICATION ===' AS info;

SELECT 
    id,
    username,
    role,
    first_name || ' ' || last_name AS full_name,
    email,
    account_status,
    registration_date
FROM users
ORDER BY role, username;

-- ============================================
-- 3. CHECK PATIENTS (Should have 5)
-- ============================================
SELECT '=== PATIENTS VERIFICATION ===' AS info;

SELECT 
    u.id,
    u.username,
    u.first_name || ' ' || u.last_name AS full_name,
    u.email,
    u.city,
    u.state,
    u.country,
    p.blood_type,
    p.insurance_provider,
    p.insurance_number,
    u.account_status
FROM users u
INNER JOIN patient p ON u.id = p.id
ORDER BY u.username;

-- ============================================
-- 4. CHECK HEALTHCARE PROVIDERS (Should have 5)
-- ============================================
SELECT '=== DOCTORS VERIFICATION ===' AS info;

SELECT 
    u.id,
    u.username,
    u.first_name || ' ' || u.last_name AS full_name,
    u.email,
    u.city,
    u.state,
    u.country,
    hp.license_number,
    hp.clinic_name,
    hp.consultation_fee,
    hp.available_time_start,
    hp.available_time_end,
    u.account_status
FROM users u
INNER JOIN healthcare_provider hp ON u.id = hp.id
ORDER BY u.username;

-- ============================================
-- 5. CHECK PROVIDER SPECIALIZATIONS
-- ============================================
SELECT '=== DOCTOR SPECIALIZATIONS ===' AS info;

SELECT 
    u.username AS doctor_username,
    u.first_name || ' ' || u.last_name AS doctor_name,
    ps.specialization
FROM healthcare_provider hp
INNER JOIN users u ON hp.id = u.id
LEFT JOIN provider_specialization ps ON hp.id = ps.healthcare_provider_id
ORDER BY u.username, ps.specialization;

-- ============================================
-- 6. CHECK PROVIDER INSURANCE
-- ============================================
SELECT '=== DOCTOR INSURANCE ACCEPTED ===' AS info;

SELECT 
    u.username AS doctor_username,
    u.first_name || ' ' || u.last_name AS doctor_name,
    pi.insurance_accepted
FROM healthcare_provider hp
INNER JOIN users u ON hp.id = u.id
LEFT JOIN provider_insurance pi ON hp.id = pi.healthcare_provider_id
ORDER BY u.username, pi.insurance_accepted;

-- ============================================
-- 7. CHECK APPOINTMENTS (Should have 6)
-- ============================================
SELECT '=== APPOINTMENTS VERIFICATION ===' AS info;

SELECT 
    ae.id,
    pu.username AS patient_username,
    pu.first_name || ' ' || pu.last_name AS patient_name,
    du.username AS doctor_username,
    du.first_name || ' ' || du.last_name AS doctor_name,
    ae.appointment_date_time,
    ae.status,
    ae.type,
    ae.reason,
    ae.share_medical_records,
    ae.is_video_call,
    ae.duration_minutes,
    ae.created_at
FROM appointment_entity ae
LEFT JOIN patient p ON ae.patient_id = p.id
LEFT JOIN users pu ON p.id = pu.id
LEFT JOIN healthcare_provider hp ON ae.provider_id = hp.id
LEFT JOIN users du ON hp.id = du.id
ORDER BY ae.appointment_date_time DESC;

-- ============================================
-- 8. CHECK APPOINTMENT STATUS DISTRIBUTION
-- ============================================
SELECT '=== APPOINTMENT STATUS BREAKDOWN ===' AS info;

SELECT 
    status,
    COUNT(*) AS count
FROM appointment_entity
GROUP BY status
ORDER BY count DESC;

-- ============================================
-- 9. CHECK FUTURE APPOINTMENTS (for testing)
-- ============================================
SELECT '=== FUTURE APPOINTMENTS ===' AS info;

SELECT 
    ae.id,
    pu.username AS patient_username,
    du.username AS doctor_username,
    ae.appointment_date_time,
    ae.status,
    ae.reason,
    ae.share_medical_records,
    ae.is_video_call
FROM appointment_entity ae
LEFT JOIN patient p ON ae.patient_id = p.id
LEFT JOIN users pu ON p.id = pu.id
LEFT JOIN healthcare_provider hp ON ae.provider_id = hp.id
LEFT JOIN users du ON hp.id = du.id
WHERE ae.appointment_date_time > CURRENT_TIMESTAMP
ORDER BY ae.appointment_date_time;

-- ============================================
-- 10. CHECK PSYCHIATRY DOCTOR (for video call testing)
-- ============================================
SELECT '=== PSYCHIATRY DOCTOR (Video Calls) ===' AS info;

SELECT 
    u.id,
    u.username,
    u.first_name || ' ' || u.last_name AS doctor_name,
    hp.clinic_name,
    ps.specialization
FROM healthcare_provider hp
INNER JOIN users u ON hp.id = u.id
INNER JOIN provider_specialization ps ON hp.id = ps.healthcare_provider_id
WHERE ps.specialization = 'PSYCHIATRY'
AND u.account_status = 'ACTIVE';

-- ============================================
-- 11. CHECK NOTIFICATION PREFERENCES
-- ============================================
SELECT '=== NOTIFICATION PREFERENCES ===' AS info;

SELECT 
    u.username,
    unp.email_notifications,
    unp.push_notifications,
    unp.appointment_reminders
FROM user_notification_preferences unp
INNER JOIN users u ON unp.user_id = u.id;

-- ============================================
-- 12. CHECK PRIVACY SETTINGS
-- ============================================
SELECT '=== PRIVACY SETTINGS ===' AS info;

SELECT 
    u.username,
    ups.profile_visibility,
    ups.show_email,
    ups.show_phone
FROM user_privacy_settings ups
INNER JOIN users u ON ups.user_id = u.id;

-- ============================================
-- 13. QUICK TEST CREDENTIALS CHECK
-- ============================================
SELECT '=== TEST CREDENTIALS SUMMARY ===' AS info;

SELECT 
    username,
    role,
    email,
    account_status,
    CASE 
        WHEN role = 'ADMIN' THEN 'admin@mediconnect.com'
        WHEN role = 'PATIENT' THEN 'patient email'
        WHEN role = 'HEALTHPROVIDER' THEN 'doctor email'
    END AS expected_email_pattern
FROM users
WHERE username IN ('admin', 'patient1', 'patient2', 'doctor1', 'doctor2')
ORDER BY role, username;

-- ============================================
-- 14. CHECK FOR DATA INTEGRITY ISSUES
-- ============================================
SELECT '=== DATA INTEGRITY CHECKS ===' AS info;

-- Check for orphaned patients
SELECT 'Orphaned Patients' AS check_type, COUNT(*) AS issues
FROM patient p
LEFT JOIN users u ON p.id = u.id
WHERE u.id IS NULL

UNION ALL

-- Check for orphaned doctors
SELECT 'Orphaned Doctors', COUNT(*)
FROM healthcare_provider hp
LEFT JOIN users u ON hp.id = u.id
WHERE u.id IS NULL

UNION ALL

-- Check for appointments without valid patient
SELECT 'Appointments without Patient', COUNT(*)
FROM appointment_entity ae
LEFT JOIN patient p ON ae.patient_id = p.id
WHERE p.id IS NULL

UNION ALL

-- Check for appointments without valid doctor
SELECT 'Appointments without Doctor', COUNT(*)
FROM appointment_entity ae
LEFT JOIN healthcare_provider hp ON ae.provider_id = hp.id
WHERE hp.id IS NULL;

-- ============================================
-- 15. EXPECTED RESULTS SUMMARY
-- ============================================
SELECT '=== EXPECTED RESULTS ===' AS info;

SELECT 
    'Total Users' AS metric,
    '11 (1 admin + 5 patients + 5 doctors)' AS expected_value,
    COUNT(*)::TEXT AS actual_value,
    CASE WHEN COUNT(*) = 11 THEN '✓ PASS' ELSE '✗ FAIL' END AS status
FROM users

UNION ALL

SELECT 
    'Total Patients',
    '5',
    COUNT(*)::TEXT,
    CASE WHEN COUNT(*) = 5 THEN '✓ PASS' ELSE '✗ FAIL' END
FROM patient

UNION ALL

SELECT 
    'Total Doctors',
    '5',
    COUNT(*)::TEXT,
    CASE WHEN COUNT(*) = 5 THEN '✓ PASS' ELSE '✗ FAIL' END
FROM healthcare_provider

UNION ALL

SELECT 
    'Total Appointments',
    '6',
    COUNT(*)::TEXT,
    CASE WHEN COUNT(*) = 6 THEN '✓ PASS' ELSE '✗ FAIL' END
FROM appointment_entity

UNION ALL

SELECT 
    'Active Doctors',
    '5',
    COUNT(*)::TEXT,
    CASE WHEN COUNT(*) = 5 THEN '✓ PASS' ELSE '✗ FAIL' END
FROM healthcare_provider hp
INNER JOIN users u ON hp.id = u.id
WHERE u.account_status = 'ACTIVE'

UNION ALL

SELECT 
    'Psychiatry Doctors',
    '1',
    COUNT(*)::TEXT,
    CASE WHEN COUNT(*) = 1 THEN '✓ PASS' ELSE '✗ FAIL' END
FROM healthcare_provider hp
INNER JOIN provider_specialization ps ON hp.id = ps.healthcare_provider_id
WHERE ps.specialization = 'PSYCHIATRY';

-- ============================================
-- END OF VERIFICATION
-- ============================================




