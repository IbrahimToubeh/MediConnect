# Fix Appointment Status Constraint Error

## Problem
The database has a check constraint `appointment_entity_status_check` that doesn't match the enum values in your Java code, causing this error:
```
ERROR: new row for relation "appointment_entity" violates check constraint "appointment_entity_status_check"
```

## Solution

Run this SQL script to fix the constraint:

### Option 1: Run SQL Script via pgAdmin
1. Open **pgAdmin**
2. Connect to your PostgreSQL server
3. Navigate to database: `Meddiconnect`
4. Right-click on database → **Query Tool**
5. Open file: `fix_appointment_status_constraint.sql`
6. Execute the query (F5 or Run button)

### Option 2: Run SQL Script via Command Line
```powershell
cd MediConnect
psql -h localhost -p 5432 -U postgres -d Meddiconnect -f fix_appointment_status_constraint.sql
```
Enter password when prompted: `Abdnsour1`

### Option 3: Manual SQL Execution
Connect to your database and run:
```sql
ALTER TABLE appointment_entity 
DROP CONSTRAINT IF EXISTS appointment_entity_status_check;

ALTER TABLE appointment_entity 
ADD CONSTRAINT appointment_entity_status_check 
CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW'));
```

## What This Fix Does

- **Drops** the old constraint that was rejecting valid enum values
- **Creates** a new constraint that allows these values:
  - `SCHEDULED` ✅
  - `CONFIRMED` ✅
  - `IN_PROGRESS` ✅
  - `COMPLETED` ✅
  - `CANCELLED` ✅
  - `NO_SHOW` ✅

## After Running

1. Restart your Spring Boot application
2. Try booking an appointment again
3. The error should be resolved!

