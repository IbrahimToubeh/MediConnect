# Script to help find your PostgreSQL password
Write-Host "Testing PostgreSQL connection with common passwords..." -ForegroundColor Cyan
Write-Host ""

$commonPasswords = @("admin", "postgres", "password", "root", "123456", "", "postgres123")

foreach ($pwd in $commonPasswords) {
    Write-Host "Trying password: '$pwd'..." -ForegroundColor Yellow
    $env:PGPASSWORD = $pwd
    $result = psql -h localhost -p 5432 -U postgres -d postgres -c "SELECT version();" 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "SUCCESS! Your password is: '$pwd'" -ForegroundColor Green
        Write-Host ""
        Write-Host "Update application.properties with:" -ForegroundColor Cyan
        Write-Host "spring.datasource.password=$pwd" -ForegroundColor White
        break
    }
}

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "None of the common passwords worked." -ForegroundColor Red
    Write-Host "You need to reset your PostgreSQL password." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "To reset password using pgAdmin:" -ForegroundColor Cyan
    Write-Host "1. Open pgAdmin"
    Write-Host "2. Connect to server"
    Write-Host "3. Right-click 'Login/Group Roles' -> 'postgres'"
    Write-Host "4. Go to 'Definition' tab"
    Write-Host "5. Set password to 'admin'"
    Write-Host "6. Click Save"
}

