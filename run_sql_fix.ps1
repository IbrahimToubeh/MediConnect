# PowerShell script to run the database fix
# This script will execute the SQL fix against your PostgreSQL database

Write-Host "=== MediConnect Database Schema Fix ===" -ForegroundColor Green
Write-Host "This script will create missing tables in your PostgreSQL database." -ForegroundColor Yellow
Write-Host ""

# Database connection parameters (update these if needed)
$dbHost = "localhost"
$dbPort = "5432"
$dbName = "Meddiconnect"
$dbUser = "postgres"
$dbPassword = "Abdnsour1"

# SQL file path
$sqlFile = "fix_database_schema.sql"

Write-Host "Database: $dbName on ${dbHost}:${dbPort}" -ForegroundColor Cyan
Write-Host "User: $dbUser" -ForegroundColor Cyan
Write-Host "SQL File: $sqlFile" -ForegroundColor Cyan
Write-Host ""

# Check if psql is available
try {
    $psqlVersion = psql --version
    Write-Host "PostgreSQL client found: $psqlVersion" -ForegroundColor Green
} catch {
    Write-Host "ERROR: PostgreSQL client (psql) not found!" -ForegroundColor Red
    Write-Host "Please install PostgreSQL or add psql to your PATH." -ForegroundColor Red
    Write-Host "You can also run the SQL manually in pgAdmin or any PostgreSQL client." -ForegroundColor Yellow
    exit 1
}

# Check if SQL file exists
if (-not (Test-Path $sqlFile)) {
    Write-Host "ERROR: SQL file '$sqlFile' not found!" -ForegroundColor Red
    exit 1
}

Write-Host "Running SQL fix..." -ForegroundColor Yellow

# Set password environment variable
$env:PGPASSWORD = $dbPassword

# Run the SQL script
try {
    psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -f $sqlFile
    Write-Host ""
    Write-Host "=== Database schema fix completed successfully! ===" -ForegroundColor Green
    Write-Host "You can now restart your Spring Boot application." -ForegroundColor Yellow
} catch {
    Write-Host "ERROR: Failed to run SQL script!" -ForegroundColor Red
    Write-Host "Please check your database connection and try again." -ForegroundColor Red
} finally {
    # Clear password from environment
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "Press any key to continue..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
