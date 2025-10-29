-- Create MediConnect database if it doesn't exist
-- Run this in psql or pgAdmin as a superuser

-- Connect to default postgres database first, then run:
CREATE DATABASE "MediConnect" 
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE "MediConnect" TO postgres;

-- Verify creation
SELECT datname, datowner, encoding FROM pg_database WHERE datname = 'MediConnect';

