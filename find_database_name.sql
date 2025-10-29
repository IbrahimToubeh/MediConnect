-- Run this query to find your actual database name
-- Connect to PostgreSQL (to 'postgres' database) and run:

SELECT datname FROM pg_database 
WHERE datname ILIKE '%medi%' OR datname ILIKE '%connect%'
ORDER BY datname;

-- This will show you all databases with "medi" or "connect" in the name
-- Check what the EXACT name is (case-sensitive!)

