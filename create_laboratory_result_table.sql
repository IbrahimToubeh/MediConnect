-- SQL script to create the missing laboratory_result table
-- Run this in your PostgreSQL database if the automatic schema update doesn't work

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

-- Create index for better performance
CREATE INDEX IF NOT EXISTS idx_laboratory_result_patient_id 
    ON laboratory_result(patient_id);

-- Add comments
COMMENT ON TABLE laboratory_result IS 'Stores laboratory test results with images for patients';
COMMENT ON COLUMN laboratory_result.id IS 'Primary key';
COMMENT ON COLUMN laboratory_result.description IS 'Description of the laboratory result';
COMMENT ON COLUMN laboratory_result.image IS 'Binary data of the laboratory result image';
COMMENT ON COLUMN laboratory_result.patient_id IS 'Foreign key reference to patient table';
