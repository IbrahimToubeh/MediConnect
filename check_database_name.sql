-- Check what databases exist (case-sensitive check)
SELECT datname FROM pg_database WHERE datname ILIKE '%medi%' OR datname ILIKE '%connect%';

-- Also check exact name
SELECT datname FROM pg_database WHERE datname = 'MediConnect';
SELECT datname FROM pg_database WHERE datname = 'mediconnect';
SELECT datname FROM pg_database WHERE datname = 'MEDICONNECT';

