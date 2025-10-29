# Fix Database Password Authentication Error

## Error:
```
FATAL: password authentication failed for user "postgres"
```

## Solution Options:

### Option 1: Update application.properties with Correct Password

Edit: `MediConnect/src/main/resources/application.properties`

Update the password line:
```properties
spring.datasource.password=YOUR_ACTUAL_PASSWORD
```

### Option 2: Reset PostgreSQL Password

If you don't know your password, you can reset it:

#### Using pgAdmin:
1. Open pgAdmin
2. Connect to PostgreSQL server
3. Right-click on "Login/Group Roles" → "postgres"
4. Go to "Definition" tab
5. Change password to: `admin` (or your preferred password)
6. Click "Save"

#### Using Command Line:
1. Open Command Prompt as Administrator
2. Navigate to PostgreSQL bin directory (usually `C:\Program Files\PostgreSQL\[version]\bin`)
3. Run:
```bash
psql -U postgres
```
4. Then in psql:
```sql
ALTER USER postgres WITH PASSWORD 'admin';
\q
```

#### Using Services (Windows):
1. Stop PostgreSQL service
2. Edit `pg_hba.conf` (usually in `C:\Program Files\PostgreSQL\[version]\data\`)
3. Change authentication method to `trust` temporarily:
   ```
   host    all             all             127.0.0.1/32            trust
   ```
4. Start PostgreSQL service
5. Connect and reset password:
```bash
psql -U postgres
ALTER USER postgres WITH PASSWORD 'admin';
```
6. Change `pg_hba.conf` back to `md5` or `scram-sha-256`
7. Restart PostgreSQL service

### Option 3: Test Current Password

Test what password works:
```bash
psql -h localhost -p 5432 -U postgres -d postgres
```
Try different passwords until one works, then update `application.properties`.

## Quick Fix:
1. Find your PostgreSQL password
2. Update `application.properties` with correct password
3. Restart Spring Boot application

