# Quick Run Script - JAR with H2 Database
Write-Host "Starting ScaleHubIOT with H2 database..." -ForegroundColor Cyan

# Stop any running instance
Get-Process | Where-Object { $_.ProcessName -like "*java*" } | Stop-Process -Force -ErrorAction SilentlyContinue

# Create data directory
if (-not (Test-Path "data")) {
    New-Item -ItemType Directory -Path "data" | Out-Null
}

# Run with H2 database
java -jar target\ScaleHubIOT-1.0-SNAPSHOT.jar `
    --spring.datasource.url=jdbc:h2:file:./data/scalehub `
    --spring.datasource.username=sa `
    --spring.datasource.password= `
    --spring.datasource.driver-class-name=org.h2.Driver `
    --spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect `
    --spring.h2.console.enabled=true `
    --spring.jpa.show-sql=false

