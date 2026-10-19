# Check ScaleHubIOT Status
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ScaleHubIOT Status Check" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Check process
Write-Host "[1] Process Status:" -ForegroundColor Yellow
$process = Get-Process | Where-Object { $_.ProcessName -like "*java*" -and $_.WorkingSet -gt 100MB }
if ($process) {
    Write-Host "✅ Running (PID: $($process.Id), Memory: $([math]::Round($process.WorkingSet/1MB,0))MB)" -ForegroundColor Green
} else {
    Write-Host "❌ Not running" -ForegroundColor Red
    exit
}

# 2. Check port
Write-Host ""
Write-Host "[2] Port 8080:" -ForegroundColor Yellow
$port = netstat -ano | findstr ":8080.*LISTENING"
if ($port) {
    Write-Host "✅ Listening" -ForegroundColor Green
} else {
    Write-Host "⏳ Not listening yet (app may be starting...)" -ForegroundColor Yellow
}

# 3. Check API
Write-Host ""
Write-Host "[3] API Health:" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/actuator/health" -UseBasicParsing -TimeoutSec 3
    Write-Host "✅ API Healthy" -ForegroundColor Green
    Write-Host "   Response: $($response.Content)" -ForegroundColor Gray
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  App is Ready! 🚀" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Access:" -ForegroundColor Yellow
    Write-Host "  • API: http://localhost:8080/api/v1" -ForegroundColor Cyan
    Write-Host "  • Swagger: http://localhost:8080/api/v1/swagger-ui.html" -ForegroundColor Cyan
    Write-Host "  • H2 Console: http://localhost:8080/h2-console" -ForegroundColor Cyan
} catch {
    Write-Host "⚠️ API not responding" -ForegroundColor Yellow
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Possible reasons:" -ForegroundColor Yellow
    Write-Host "  • App is still starting (wait 10-30s)" -ForegroundColor Gray
    Write-Host "  • Database connection issue" -ForegroundColor Gray
    Write-Host "  • Configuration error" -ForegroundColor Gray
    Write-Host "  • Check application logs for details" -ForegroundColor Gray
}

Write-Host ""
