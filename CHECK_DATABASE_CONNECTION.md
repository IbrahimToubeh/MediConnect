# Check Database Connection

## Current Database Configuration:
- **Host:** localhost
- **Port:** 5432
- **Database Name:** MediConnect
- **Username:** postgres
- **Password:** admin

## Steps to Verify Database Connection:

### Step 1: Check if PostgreSQL is Running
1. Open **Services** (Press `Win+R`, type `services.msc`, Enter)
2. Look for **PostgreSQL** service
3. Check if it's **Running**
4. If not running, right-click → **Start**

Or check via Command Prompt:
```bash
sc query postgresql-x64-XX  # Replace XX with your PostgreSQL version
```

### Step 2: Verify Database Exists
1. Open **pgAdmin** or **psql** command line
2. Connect to PostgreSQL server
3. Check if database `MediConnect` exists:
```sql
SELECT datname FROM pg_database WHERE datname = 'MediConnect';
```

If it doesn't exist, create it:
```sql
CREATE DATABASE "MediConnect";
```

### Step 3: Test Connection from Command Line
```bash
psql -h localhost -p 5432 -U postgres -d MediConnect
```
Enter password: `admin`

If connection works, you'll see a prompt like: `MediConnect=#`

### Step 4: Verify Tables Exist
Once connected, check tables:
```sql
\dt
```

### Step 5: Run Your Spring Boot Application
1. In IntelliJ, run `MediConnectApplication.java`
2. Check the console logs for:
   - ✅ "HikariPool-1 - Starting..."
   - ✅ "HikariPool-1 - Start completed."
   - ❌ Connection errors (if any)

### Step 6: If Connection Fails - Common Issues:

#### Issue: "Connection refused"
- PostgreSQL is not running
- Fix: Start PostgreSQL service

#### Issue: "FATAL: database 'MediConnect' does not exist"
- Database needs to be created
- Fix: Create database (see Step 2)

#### Issue: "FATAL: password authentication failed"
- Wrong username/password
- Fix: Update `application.properties` with correct credentials

#### Issue: "Connection to localhost:5432 rushed"
- Firewall blocking or PostgreSQL not listening on port 5432
- Fix: Check PostgreSQL `postgresql.conf` - ensure `listen_addresses = '*'` or `'localhost'`
- Check `pg_hba.conf` - ensure local connections are allowed

## Update Configuration (if needed):
Edit: `MediConnect/src/main/resources/application.properties`

## Testing Connection from Application:
After starting the app, check logs for:
- Database connection established
- Tables created/updated
- No connection errors

