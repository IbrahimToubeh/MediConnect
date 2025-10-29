# How to Run Your Backend Outside IntelliJ

## Common Issue
IntelliJ runs your app with all dependencies on the classpath automatically, but when running from command line or deploying, you need to build a proper JAR file.

## Solution Steps

### Step 1: Build the Executable JAR
Run this command in PowerShell (from the `MediConnect` folder):

```powershell
cd MediConnect
.\mvnw.cmd clean package -DskipTests
```

This creates an executable JAR file at:
```
MediConnect/target/MediConnect-0.0.1-SNAPSHOT.jar
```

### Step 2: Run the JAR File
```powershell
java -jar target/MediConnect-0.0.1-SNAPSHOT.jar
```

### Step 3: Verify Prerequisites Before Running
Make sure these are running:

1. **PostgreSQL Database:**
   - Database: `Meddiconnect`
   - Host: `localhost:5432`
   - Username: `postgres`
   - Password: `Abdnsour1`

2. **Redis** (if using caching):
   - Host: `localhost:6379`

## Common Errors & Fixes

### Error: "Java version mismatch"
**Problem:** Command line uses different Java version than IntelliJ.

**Fix:**
```powershell
# Check your Java version
java -version

# Should show Java 24. If not, set JAVA_HOME:
$env:JAVA_HOME = "C:\Program Files\Java\jdk-24"  # Update path to your JDK
```

### Error: "Cannot find or load main class"
**Problem:** JAR wasn't built correctly.

**Fix:**
1. In IntelliJ: Right-click `pom.xml` → **Maven** → **Reload Project**
2. Then run: `.\mvnw.cmd clean package -DskipTests`

### Error: "Connection refused" to database
**Problem:** PostgreSQL not running or wrong connection details.

**Fix:**
1. Check if PostgreSQL service is running
2. Verify database name in `application.properties` matches your actual database

### Error: "Port 8080 already in use"
**Problem:** Another instance is running.

**Fix:**
```powershell
# Find what's using port 8080
netstat -ano | findstr :8080

# Kill the process (replace PID with actual process ID)
taskkill /PID <PID> /F
```

## Quick Run Script

Create `run-backend.ps1`:
```powershell
# Build
cd MediConnect
.\mvnw.cmd clean package -DskipTests

# Run
java -jar target/MediConnect-0.0.1-SNAPSHOT.jar
```

Then run: `.\run-backend.ps1`

## IntelliJ vs Command Line Differences

| Feature | IntelliJ | Command Line |
|---------|----------|--------------|
| **Dependencies** | Auto-added to classpath | Must be in JAR |
| **Java Version** | Uses IDE's JDK | Uses system JAVA_HOME |
| **Hot Reload** | DevTools works | Need to rebuild JAR |
| **Configuration** | IDE settings | Uses `application.properties` |

## Next Steps After Building

After successful build, you should see:
```
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

The JAR file will be at: `target/MediConnect-0.0.1-SNAPSHOT.jar`

You can now run it from anywhere:
```powershell
java -jar path\to\MediConnect-0.0.1-SNAPSHOT.jar
```

