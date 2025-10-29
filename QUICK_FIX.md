# QUICK FIX for "Failed to fetch data" Error

## The Problem
You're getting "Failed to fetch data" when trying to view your patient profile. This is because the database tables are missing.

## The Solution (Choose One)

### Option 1: Run the Database Fix Script (Easiest)
1. Open PowerShell as Administrator
2. Navigate to your MediConnect folder:
   ```powershell
   cd "C:\Users\abdns\Desktop\meddiconnect\MediConnect"
   ```
3. Run the fix script:
   ```powershell
   .\run_sql_fix.ps1
   ```

### Option 2: Manual Fix via pgAdmin
1. Open pgAdmin
2. Connect to your PostgreSQL database
3. Right-click on your database "Meddiconnect"
4. Select "Query Tool"
5. Copy and paste this SQL:

```sql
-- Create the missing laboratory_result table
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

-- Create medication table
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

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_laboratory_result_patient_id ON laboratory_result(patient_id);
CREATE INDEX IF NOT EXISTS idx_medication_patient_id ON medication(patient_id);
CREATE INDEX IF NOT EXISTS idx_mental_health_medication_patient_id ON mental_health_medication(patient_id);
```

6. Click "Execute" (F5)
7. You should see "Query returned successfully"

### Option 3: Command Line Fix
1. Open Command Prompt
2. Run this command:
```bash
psql -h localhost -p 5432 -U postgres -d Meddiconnect -c "CREATE TABLE IF NOT EXISTS laboratory_result (id BIGSERIAL PRIMARY KEY, description VARCHAR(255), image BYTEA, patient_id BIGINT, CONSTRAINT fk_laboratory_result_patient FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE CASCADE);"
```

## After Running the Fix
1. **Restart your Spring Boot application** (stop and start it again)
2. **Try logging in** and going to your patient profile
3. **The error should be gone** and you should see your data

## If It Still Doesn't Work
1. Check the browser console (F12) for detailed error messages
2. Make sure your Spring Boot application is running on port 8080
3. Verify your database connection is working

## Verification
To check if the tables were created, run this in pgAdmin:
```sql
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('laboratory_result', 'medication', 'mental_health_medication');
```

You should see all three tables listed.
