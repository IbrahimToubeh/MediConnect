-- Test script to check if required tables exist
SELECT 
    table_name,
    CASE 
        WHEN table_name IS NOT NULL THEN 'EXISTS'
        ELSE 'MISSING'
    END as status
FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN (
    'users',
    'patient', 
    'healthcare_provider',
    'laboratory_result',
    'medication',
    'mental_health_medication',
    'medical_record',
    'appointment_entity'
)
ORDER BY table_name;
