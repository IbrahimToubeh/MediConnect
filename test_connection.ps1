# PowerShell script to test PostgreSQL connection
# Run this to check if your database is accessible

Write-Host "Testing PostgreSQL Connection..." -ForegroundColor Cyan
Write-Host ""

# Test if PostgreSQL service is running
Write-Host "Checking PostgreSQL service..." -ForegroundColor Yellow
$pgService = Get-Service -Name "*postgresql*" -ErrorAction SilentlyContinue
if ($pgService) {
    Write-Host "Found PostgreSQL service: $($pgService.Name)" -ForegroundColor Green
    if ($pgService.Status -eq 'Running') {
        Write-Host "Status: Running" -ForegroundColor Green
    } else {
        Write-Host "Status: $($pgService.Status) - Service is not running!" -ForegroundColor Red
        Write-Host "Please start the PostgreSQL service" -ForegroundColor Yellow
    }
} else {
    Write-Host "PostgreSQL service not found. Is PostgreSQL installed?" -ForegroundColor Red
}

Write-Host ""
Write-Host "Connection Details:" -ForegroundColor Cyan
Write-Host "  Host: localhost"
Write-Host "  Port: 5432"
Write-Host "  Database: MediConnect"
Write-Host "  Username: postgres"
Write-Host ""

# Test connection using psql (if available)
if (Get-Command psql -ErrorAction SilentlyContinue) {
    Write-Host "Testing connection with psql..." -ForegroundColor Yellow
    Write-Host "You will be prompted for the password (admin)" -ForegroundColor分数 Yellow
    Write-Host ""
    psql -h localhost -p 5432 -U postgres -d MediConnect -c "\conninfo"
} else {
    Write-Host "psql command not found in PATH." -ForegroundColor Yellow
    Write-Host "You can test the connection manually:" -ForegroundColor Yellow
    Write-Host "  psql -h localhost -p 5432 -U postgres -d MediConnect"
    Write-Host ""
}

Write-Host "To create the database if it doesn't exist:" -ForegroundColor Cyan
Write-Host "  psql -h localhost -p 5432 -U postgres -d postgres -f create_database.sql"
Write-Host ""

