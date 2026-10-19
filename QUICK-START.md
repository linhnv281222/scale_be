# ScaleHubIOT - Quick Commands

## Build & Package

```powershell
# Build portable package (recommended)
.\build-app.ps1

# Build with Launch4j
.\build-app.ps1 -Method launch4j

# Skip JAR rebuild (if already built)
.\build-app.ps1 -SkipBuild
```

## Run

```powershell
# Development - with H2 database
.\run-h2.ps1

# Production package
cd package-output
.\ScaleHubIOT.bat
```

## Check Status

```powershell
.\check-status.ps1
```

## Output

- **package-output/** - Complete portable package with bundled JRE
- **ScaleHubIOT.exe** - Single executable (if using Launch4j)

## Quick Test

```powershell
.\build-app.ps1 -SkipBuild
cd package-output
.\ScaleHubIOT.bat
```

Then open: http://localhost:8080/api/v1/swagger-ui.html

---

See [build-exe.md](build-exe.md) for full documentation.
