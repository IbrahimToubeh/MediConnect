-- Complete database schema fix for MediConnect
-- Run this script in your PostgreSQL database to create all missing tables

-- Create laboratory_result table
CREATE TABLE IF NOT EXISTS laboratory_result (
    id BIGSERIAL PRIMARY KEY,
    description VARCHAR(255),
    image BYTEA,
    patient_id BIGINT,
    CONSTRAINT fk_laboratory_result_patient 
        FOREIGN KEY (patient_id) 
        REFERENCES patient(id) 
        ON DELETE CASCADE
);

-- Create medication table (for regular medications)
CREATE TABLE IF NOT EXISTS medication (
    id BIGSERIAL PRIMARY KEY,
    medication_name VARCHAR(255) NOT NULL,
    medication_dosage VARCHAR(255) NOT NULL,
    medication_frequency VARCHAR(255) NOT NULL,
    medication_start_date DATE,
    medication_end_date DATE,
    in_use BOOLEAN,
    patient_id BIGINT,
    CONSTRAINT fk_medication_patient 
        FOREIGN KEY (patient_id) 
        REFERENCES patient(id) 
        ON DELETE CASCADE
);

-- Create mental_health_medication table
CREATE TABLE IF NOT EXISTS mental_health_medication (
    id BIGSERIAL PRIMARY KEY,
    medication_name VARCHAR(255) NOT NULL,
    medication_dosage VARCHAR(255) NOT NULL,
    medication_frequency VARCHAR(255) NOT NULL,
    medication_start_date DATE,
    medication_end_date DATE,
    in_use BOOLEAN,
    patient_id BIGINT,
    CONSTRAINT fk_mental_health_medication_patient 
        FOREIGN KEY (patient_id) 
        REFERENCES patient(id) 
        ON DELETE CASCADE
);

-- Create education_history table
CREATE TABLE IF NOT EXISTS education_history (
    id BIGSERIAL PRIMARY KEY,
    institution_name VARCHAR(255),
    start_date DATE,
    end_date DATE,
    still_enrolled BOOLEAN,
    provider_id BIGINT,
    CONSTRAINT fk_education_history_provider 
        FOREIGN KEY (provider_id) 
        REFERENCES healthcare_provider(id) 
        ON DELETE CASCADE
);

-- Create work_experience table
CREATE TABLE IF NOT EXISTS work_experience (
    id BIGSERIAL PRIMARY KEY,
    organization_name VARCHAR(255),
    role_title VARCHAR(255),
    start_date DATE,
    end_date DATE,
    still_working BOOLEAN,
    provider_id BIGINT,
    CONSTRAINT fk_work_experience_provider 
        FOREIGN KEY (provider_id) 
        REFERENCES healthcare_provider(id) 
        ON DELETE CASCADE
);

-- Create medical_record table
CREATE TABLE IF NOT EXISTS medical_record (
    id SERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    appointment_id INTEGER,
    visit_date TIMESTAMP,
    diagnosis TEXT,
    treatment TEXT,
    prescription TEXT,
    symptoms TEXT,
    notes TEXT,
    temperature DOUBLE PRECISION,
    blood_pressure VARCHAR(255),
    heart_rate INTEGER,
    weight DOUBLE PRECISION,
    height DOUBLE PRECISION,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_medical_record_patient 
        FOREIGN KEY (patient_id) 
        REFERENCES patient(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_medical_record_provider 
        FOREIGN KEY (provider_id) 
        REFERENCES healthcare_provider(id) 
        ON DELETE CASCADE
);

-- Create appointment_entity table
CREATE TABLE IF NOT EXISTS appointment_entity (
    id SERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    appointment_date_time TIMESTAMP,
    status VARCHAR(255) DEFAULT 'SCHEDULED',
    type VARCHAR(255),
    reason TEXT,
    notes TEXT,
    duration_minutes INTEGER DEFAULT 30,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appointment_patient 
        FOREIGN KEY (patient_id) 
        REFERENCES patient(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_appointment_provider 
        FOREIGN KEY (provider_id) 
        REFERENCES healthcare_provider(id) 
        ON DELETE CASCADE
);

-- Create social media tables
CREATE TABLE IF NOT EXISTS medical_post (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    content TEXT,
    media_url VARCHAR(255),
    privacy VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_medical_post_provider 
        FOREIGN KEY (provider_id) 
        REFERENCES healthcare_provider(id) 
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS medical_post_like (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    like_giver_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_medical_post_like_post 
        FOREIGN KEY (post_id) 
        REFERENCES medical_post(id) 
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS medical_post_comment (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    commenter_id BIGINT,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_medical_post_comment_post 
        FOREIGN KEY (post_id) 
        REFERENCES medical_post(id) 
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS medical_post_rating (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    truth_rank VARCHAR(255),
    context_rank VARCHAR(255),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_medical_post_rating_post 
        FOREIGN KEY (post_id) 
        REFERENCES medical_post(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_medical_post_rating_provider 
        FOREIGN KEY (provider_id) 
        REFERENCES healthcare_provider(id) 
        ON DELETE CASCADE
);

-- Create provider_availability table (collection table)
CREATE TABLE IF NOT EXISTS provider_availability (
    healthcare_provider_id BIGINT,
    available_days VARCHAR(255),
    CONSTRAINT fk_provider_availability_provider 
        FOREIGN KEY (healthcare_provider_id) 
        REFERENCES healthcare_provider(id) 
        ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_laboratory_result_patient_id ON laboratory_result(patient_id);
CREATE INDEX IF NOT EXISTS idx_medication_patient_id ON medication(patient_id);
CREATE INDEX IF NOT EXISTS idx_mental_health_medication_patient_id ON mental_health_medication(patient_id);
CREATE INDEX IF NOT EXISTS idx_education_history_provider_id ON education_history(provider_id);
CREATE INDEX IF NOT EXISTS idx_work_experience_provider_id ON work_experience(provider_id);
CREATE INDEX IF NOT EXISTS idx_medical_record_patient_id ON medical_record(patient_id);
CREATE INDEX IF NOT EXISTS idx_medical_record_provider_id ON medical_record(provider_id);
CREATE INDEX IF NOT EXISTS idx_appointment_patient_id ON appointment_entity(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointment_provider_id ON appointment_entity(provider_id);

-- Add comments for documentation
COMMENT ON TABLE laboratory_result IS 'Stores laboratory test results with images for patients';
COMMENT ON TABLE medication IS 'Stores regular medications for patients';
COMMENT ON TABLE mental_health_medication IS 'Stores mental health medications for patients';
COMMENT ON TABLE education_history IS 'Stores education history for healthcare providers';
COMMENT ON TABLE work_experience IS 'Stores work experience for healthcare providers';
COMMENT ON TABLE medical_record IS 'Stores medical visit records';
COMMENT ON TABLE appointment_entity IS 'Stores patient appointments with healthcare providers';
COMMENT ON TABLE medical_post IS 'Stores medical posts by healthcare providers';
COMMENT ON TABLE medical_post_like IS 'Stores likes on medical posts';
COMMENT ON TABLE medical_post_comment IS 'Stores comments on medical posts';
COMMENT ON TABLE medical_post_rating IS 'Stores professional ratings of medical posts';
COMMENT ON TABLE provider_availability IS 'Stores availability days for healthcare providers';
