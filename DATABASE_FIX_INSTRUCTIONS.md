# Database Schema Fix Instructions

## Problem
You're getting this error:
```
JDBC exception executing SQL [select lr1_0.id,lr1_0.description,lr1_0.image,lr1_0.patient_id from laboratory_result lr1_0 left join patient p1_0 on p1_0.id=lr1_0.patient_id where p1_0.id=?] [ERROR: relation "laboratory_result" does not exist Position: 69]
```

This means the `laboratory_result` table (and possibly other tables) don't exist in your PostgreSQL database.

## Solution Options

### Option 1: Automatic Fix (Recommended)
1. **Stop your Spring Boot application** if it's running
2. **Run the PowerShell script**:
   ```powershell
   cd MediConnect
   .\run_sql_fix.ps1
   ```
3. **Restart your Spring Boot application**

### Option 2: Manual Fix via pgAdmin
1. **Open pgAdmin** (or any PostgreSQL client)
2. **Connect to your database** (`Meddiconnect`)
3. **Open Query Tool**
4. **Copy and paste the contents** of `fix_database_schema.sql`
5. **Execute the query**
6. **Restart your Spring Boot application**

### Option 3: Manual Fix via Command Line
1. **Open Command Prompt or PowerShell**
2. **Navigate to the MediConnect folder**
3. **Run this command**:
   ```bash
   psql -h localhost -p 5432 -U postgres -d Meddiconnect -f fix_database_schema.sql
   ```
4. **Enter your password** when prompted (`Abdnsour1`)
5. **Restart your Spring Boot application**

### Option 4: Quick Fix (Just the missing table)
If you only want to create the missing `laboratory_result` table:

```sql
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

CREATE INDEX IF NOT EXISTS idx_laboratory_result_patient_id 
    ON laboratory_result(patient_id);
```

## What This Fix Does
- Creates the missing `laboratory_result` table
- Creates other missing tables (medication, medical_record, etc.)
- Sets up proper foreign key relationships
- Creates indexes for better performance
- Adds helpful comments

## After the Fix
1. **Restart your Spring Boot application**
2. **Test your patient profile** - the error should be gone
3. **Try uploading a lab result** to test the functionality
4. **Check the Laboratory Results tab** in your patient profile

## Verification
To verify the tables were created, you can run this query in your database:
```sql
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('laboratory_result', 'medication', 'medical_record');
```

You should see all three tables listed.
