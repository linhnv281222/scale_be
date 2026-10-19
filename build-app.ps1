# ScaleHubIOT Build Script - All-in-One
# Builds portable package with bundled JRE (recommended) or Launch4j EXE

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("portable", "launch4j")]
    [string]$Method = "portable",
    
    [Parameter(Mandatory=$false)]
    [switch]$SkipBuild = $false
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ScaleHubIOT Builder" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build JAR
if (-not $SkipBuild) {
    Write-Host "[1/3] Building JAR..." -ForegroundColor Yellow
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Build failed" -ForegroundColor Red
        exit 1
    }
    Write-Host "✅ JAR built" -ForegroundColor Green
} else {
    Write-Host "[1/3] Using existing JAR..." -ForegroundColor Yellow
    if (-not (Test-Path "target\ScaleHubIOT-1.0-SNAPSHOT.jar")) {
        Write-Host "❌ JAR not found. Run without -SkipBuild" -ForegroundColor Red
        exit 1
    }
}

if ($Method -eq "portable") {
    # ===== PORTABLE PACKAGE WITH BUNDLED JRE =====
    Write-Host ""
    Write-Host "[2/3] Creating portable package..." -ForegroundColor Yellow
    
    $packageDir = "package-output"
    if (Test-Path $packageDir) {
        Remove-Item $packageDir -Recurse -Force
    }
    
    # Create structure
    New-Item -ItemType Directory -Path "$packageDir\bin" -Force | Out-Null
    New-Item -ItemType Directory -Path "$packageDir\config" -Force | Out-Null
    New-Item -ItemType Directory -Path "$packageDir\data" -Force | Out-Null
    New-Item -ItemType Directory -Path "$packageDir\logs" -Force | Out-Null
    
    # Copy JAR
    Copy-Item "target\ScaleHubIOT-1.0-SNAPSHOT.jar" "$packageDir\bin\ScaleHubIOT.jar"
    
    # Create config
    @"
# ScaleHubIOT Configuration
spring.application.name=ScaleHub IoT
server.port=8080
server.servlet.context-path=/api/v1

# ========================================
# DATABASE CONFIGURATION - CLICK AND RUN!
# ========================================
# DEFAULT: H2 Database (embedded) - NO SETUP NEEDED!
# ✅ Just click ScaleHubIOT.bat and it runs immediately
# ✅ Data stored in ./data/scalehub
# ✅ Suitable for testing, demo, small-to-medium deployments

spring.datasource.url=jdbc:h2:file:./data/scalehub;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=TRUE
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# ADVANCED: PostgreSQL (external) - OPTIONAL for heavy production use
# ⚠️ Requires PostgreSQL installation & configuration
# ✅ Better for 24/7 heavy write operations
# ✅ Production-grade scalability
# 📄 See SETUP-POSTGRESQL.txt for instructions
# To use PostgreSQL: Uncomment lines below and comment H2 lines above
#spring.datasource.url=jdbc:postgresql://localhost:5432/scalehub_db
#spring.datasource.username=postgres
#spring.datasource.password=your_password_here
#spring.datasource.driver-class-name=org.postgresql.Driver
#spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Connection Pool (optimized for heavy writes)
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.defer-datasource-initialization=true

# JWT Configuration
jwt.secret=ScaleHubIOT2026SecretKeyForProductionPleaseChangeThisToYourOwnSecretKey
jwt.expiration=86400000
jwt.refresh-expiration=604800000

# Logging
logging.level.org.springframework=INFO
logging.level.org.facenet=INFO
logging.file.name=logs/scalehub.log
"@ | Out-File -FilePath "$packageDir\config\application.properties" -Encoding UTF8
    
    Write-Host "✅ Package structure created" -ForegroundColor Green
    
    # Bundle JRE
    Write-Host ""
    Write-Host "[3/3] Bundling JRE..." -ForegroundColor Yellow
    
    $javaHome = $env:JAVA_HOME
    if (-not $javaHome) {
        $javaCmd = Get-Command java -ErrorAction SilentlyContinue
        if ($javaCmd) {
            $javaHome = $javaCmd.Source | Split-Path | Split-Path
        }
    }
    
    if ($javaHome -and (Test-Path "$javaHome\bin\java.exe")) {
        Write-Host "   Found Java at: $javaHome" -ForegroundColor Gray
        Write-Host "   Creating minimal JRE..." -ForegroundColor Gray
        
        $modules = "java.base,java.desktop,java.instrument,java.management,java.naming,java.prefs,java.scripting,java.security.jgss,java.security.sasl,java.sql,jdk.unsupported,jdk.crypto.ec"
        
        & "$javaHome\bin\jlink" `
            --add-modules $modules `
            --output "$packageDir\jre" `
            --strip-debug `
            --no-man-pages `
            --no-header-files 2>&1 | Out-Null
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ JRE bundled" -ForegroundColor Green
        } else {
            Write-Host "⚠️ jlink failed, copying full JRE..." -ForegroundColor Yellow
            Copy-Item "$javaHome" "$packageDir\jre" -Recurse -Force
            Write-Host "✅ JRE copied" -ForegroundColor Green
        }
    } else {
        Write-Host "⚠️ Java not found. Package will require Java 21+ on target machine" -ForegroundColor Yellow
    }
    
    # Create launchers
    @"
@echo off
title ScaleHubIOT Server
cd /d "%~dp0"

echo ========================================
echo   ScaleHubIOT Server
echo ========================================
echo.

if exist jre\bin\java.exe (
    echo [*] Using bundled JRE
    jre\bin\java -Xms512m -Xmx2048m -jar bin\ScaleHubIOT.jar --spring.config.location=file:config/application.properties
) else (
    echo [*] Using system Java
    java -Xms512m -Xmx2048m -jar bin\ScaleHubIOT.jar --spring.config.location=file:config/application.properties
)

pause
"@ | Out-File -FilePath "$packageDir\ScaleHubIOT.bat" -Encoding ASCII
    
    @"
# ScaleHubIOT Launcher
Set-Location `$PSScriptRoot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ScaleHubIOT Server" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if (Test-Path "jre\bin\java.exe") {
    Write-Host "[*] Using bundled JRE" -ForegroundColor Green
    & ".\jre\bin\java.exe" -Xms512m -Xmx2048m -jar bin\ScaleHubIOT.jar --spring.config.location=file:config/application.properties
} else {
    Write-Host "[*] Using system Java" -ForegroundColor Green
    java -Xms512m -Xmx2048m -jar bin\ScaleHubIOT.jar --spring.config.location=file:config/application.properties
}
"@ | Out-File -FilePath "$packageDir\ScaleHubIOT.ps1" -Encoding UTF8
    
    @"
@echo off
cd /d "%~dp0"
if exist jre\bin\javaw.exe (
    start "" jre\bin\javaw.exe -Xms512m -Xmx2048m -jar bin\ScaleHubIOT.jar --spring.config.location=file:config/application.properties
) else (
    start "" javaw -Xms512m -Xmx2048m -jar bin\ScaleHubIOT.jar --spring.config.location=file:config/application.properties
)
echo ScaleHubIOT started in background
"@ | Out-File -FilePath "$packageDir\start-background.bat" -Encoding ASCII
    
    @"
@echo off
echo Stopping ScaleHubIOT...
taskkill /F /IM java.exe 2>nul
taskkill /F /IM javaw.exe 2>nul
echo Done.
"@ | Out-File -FilePath "$packageDir\stop.bat" -Encoding ASCII
    
    # README
    @"
# ScaleHubIOT - Portable Package

## Quick Start

1. Run: ScaleHubIOT.bat (or ScaleHubIOT.ps1)
2. Wait 10-20 seconds
3. Access: http://localhost:8080/api/v1/swagger-ui.html

## Files

- ScaleHubIOT.bat - Main launcher
- ScaleHubIOT.ps1 - PowerShell launcher
- start-background.bat - Run in background
- stop.bat - Stop server
- config/application.properties - Configuration
- data/ - Database files
- logs/ - Log files
- jre/ - Java Runtime (bundled)

## URLs

- API: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/api/v1/swagger-ui.html
- H2 Console: http://localhost:8080/api/v1/h2-console
  JDBC URL: jdbc:h2:file:./data/scalehub
  User: sa
  Password: (empty)

## Requirements

Windows 10/11, 2GB RAM
No Java installation needed (JRE bundled)
"@ | Out-File -FilePath "$packageDir\README.txt" -Encoding UTF8
    
    $totalSize = (Get-ChildItem $packageDir -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  ✅ Portable Package Ready!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Location: $packageDir\" -ForegroundColor White
    Write-Host "Size: $([math]::Round($totalSize, 2)) MB" -ForegroundColor White
    Write-Host ""
    Write-Host "Run:" -ForegroundColor Yellow
    Write-Host "  cd $packageDir" -ForegroundColor Cyan
    Write-Host "  .\ScaleHubIOT.bat" -ForegroundColor Cyan
    Write-Host ""
    
} else {
    # ===== LAUNCH4J EXE =====
    Write-Host ""
    Write-Host "[2/3] Creating EXE with Launch4j..." -ForegroundColor Yellow
    
    $launch4jPaths = @(
        "C:\Program Files (x86)\Launch4j\launch4jc.exe",
        "C:\Program Files\Launch4j\launch4jc.exe"
    )
    
    $launch4j = $launch4jPaths | Where-Object { Test-Path $_ } | Select-Object -First 1
    
    if (-not $launch4j) {
        Write-Host "❌ Launch4j not found" -ForegroundColor Red
        Write-Host "   Download: https://launch4j.sourceforge.net/" -ForegroundColor Yellow
        Write-Host "   Or use: .\build-app.ps1 -Method portable" -ForegroundColor Yellow
        exit 1
    }
    
    Write-Host ""
    Write-Host "[3/3] Building..." -ForegroundColor Yellow
    & $launch4j "launch4j-config.xml"
    
    if ($LASTEXITCODE -eq 0 -and (Test-Path "ScaleHubIOT.exe")) {
        $size = [math]::Round((Get-Item "ScaleHubIOT.exe").Length / 1MB, 2)
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "  ✅ EXE Ready!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "File: ScaleHubIOT.exe ($size MB)" -ForegroundColor White
        Write-Host ""
        Write-Host "Run:" -ForegroundColor Yellow
        Write-Host "  .\ScaleHubIOT.exe" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Note: Requires Java 21+ on target machine" -ForegroundColor Gray
    } else {
        Write-Host "❌ Failed to create EXE" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
