# Jira Platform - External PostgreSQL Startup (No Rebuild Required)
# Runs the existing JARs against external PostgreSQL using environment variable overrides
#
# Usage:
#   .\start-external-db.ps1          # Start all services
#   .\start-external-db.ps1 -Stop    # Stop all services
#   .\start-external-db.ps1 -Status  # Check service status
#   .\start-external-db.ps1 -Core    # Start only core services (auth, user, project, issue, workflow, gateway, frontend)

param(
    [switch]$Stop,
    [switch]$Status,
    [switch]$Core
)

$ErrorActionPreference = "SilentlyContinue"
$ProjectRoot = $PSScriptRoot
$LogDir = Join-Path $ProjectRoot "platform-runtime\logs"
$PidDir = Join-Path $ProjectRoot "platform-runtime\pids"
$MigDir = Join-Path $ProjectRoot "migrations-external"

# External DB Configuration
$DB_HOST = "in0-eplmdb-v01"
$DB_PORT = "5432"
$DB_NAME = "systems"
$DB_USERNAME = "systems_admin"
$DB_PASSWORD = "Hcu4ieD8R13qaf7JVSsu"
$JWT_SECRET = "jira-platform-super-secret-key-that-is-at-least-256-bits-long"

# Java 21 path - search known locations (Java 21+ first, JAVA_HOME last as fallback)
$JavaPath = $null
$JavaSearchPaths = @(
    "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.2\jbr\bin\java.exe",
    "C:\Program Files\JetBrains\IntelliJ IDEA *\jbr\bin\java.exe",
    "C:\Program Files\Java\jdk-21*\bin\java.exe",
    "C:\Program Files\Eclipse Adoptium\jdk-21*\bin\java.exe",
    "C:\Program Files\Eclipse Foundation\jdk-21*\bin\java.exe",
    "$env:JAVA_HOME\bin\java.exe"
)
foreach ($p in $JavaSearchPaths) {
    $resolved = Get-Item $p -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($resolved) { $JavaPath = $resolved.FullName; break }
}
if (-not $JavaPath) {
    $JavaPath = "java.exe"
    Write-Host "[WARN] Java 21 not found, falling back to system Java" -ForegroundColor Yellow
} else {
    Write-Host "Java: $JavaPath" -ForegroundColor Cyan
}

# Service definitions: name, port, schema, jarPath, memory, extraEnv
$AllServices = @(
    @{ name="auth-service";         port=8081; schema="jira_auth";         jar="jira-auth-service\target\jira-auth-service-1.0.0.jar";         mem="256m"; extra=@{JWT_SECRET=$JWT_SECRET; JWT_EXPIRATION_MS="86400000"; JWT_REFRESH_EXPIRATION_MS="604800000"} },
    @{ name="user-service";         port=8082; schema="jira_user";         jar="jira-user-service\target\jira-user-service-1.0.0.jar";         mem="256m"; extra=@{} },
    @{ name="project-service";      port=8083; schema="jira_project";      jar="jira-project-service\target\jira-project-service-1.0.0.jar";   mem="256m"; extra=@{} },
    @{ name="issue-service";        port=8084; schema="jira_issue";        jar="jira-issue-service\target\jira-issue-service-1.0.0.jar";       mem="512m"; extra=@{WORKFLOW_SERVICE_URL="http://localhost:8085"} },
    @{ name="workflow-service";     port=8085; schema="jira_workflow";      jar="jira-workflow-service\target\jira-workflow-service-1.0.0.jar"; mem="256m"; extra=@{} },
    @{ name="comment-service";      port=8086; schema="jira_comment";      jar="jira-comment-service\target\jira-comment-service-1.0.0.jar";   mem="256m"; extra=@{} },
    @{ name="notification-service"; port=8087; schema="jira_notification"; jar="jira-notification-service\target\jira-notification-service-1.0.0.jar"; mem="256m"; extra=@{} },
    @{ name="search-service";       port=8088; schema="jira_search";       jar="jira-search-service\target\jira-search-service-1.0.0.jar";     mem="256m"; extra=@{} },
    @{ name="audit-service";        port=8089; schema="jira_audit";        jar="jira-audit-service\target\jira-audit-service-1.0.0.jar";       mem="256m"; extra=@{} },
    @{ name="attachment-service";   port=8090; schema="jira_attachment";   jar="jira-attachment-service\target\jira-attachment-service-1.0.0.jar"; mem="512m"; extra=@{ATTACHMENT_STORAGE_PATH="platform-runtime\attachments"} },
    @{ name="sprint-service";       port=8091; schema="jira_sprint";       jar="jira-sprint-service\target\jira-sprint-service-1.0.0.jar";     mem="256m"; extra=@{} },
    @{ name="plan-service";         port=8092; schema="jira_plan";         jar="jira-plan-service\target\jira-plan-service-1.0.0.jar";         mem="256m"; extra=@{} },
    @{ name="admin-service";        port=8093; schema="jira_admin";        jar="jira-admin-service\target\jira-admin-service-1.0.0.jar";       mem="256m"; extra=@{} },
    @{ name="migration-service";    port=8094; schema="jira_migration";    jar="jira-migration-service\target\jira-migration-service-1.0.0.jar"; mem="512m"; extra=@{} },
    @{ name="version-service";      port=8096; schema="jira_version";      jar="jira-version-service\target\jira-version-service-1.0.0.jar";   mem="256m"; extra=@{} },
    @{ name="component-service";    port=8097; schema="jira_component";    jar="jira-component-service\target\jira-component-service-1.0.0.jar"; mem="256m"; extra=@{} }
)

$CoreServiceNames = @("auth-service","user-service","project-service","issue-service","workflow-service")

# ============================================================
# FUNCTIONS
# ============================================================

function Initialize-Dirs {
    @($LogDir, $PidDir, (Join-Path $ProjectRoot "platform-runtime\attachments")) | ForEach-Object {
        if (-not (Test-Path $_)) { New-Item -ItemType Directory -Path $_ -Force | Out-Null }
    }
}

function Test-DbConnection {
    Write-Host "Testing external PostgreSQL connection..." -ForegroundColor Cyan
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient($DB_HOST, [int]$DB_PORT)
        $tcp.Close()
        Write-Host "[OK] $DB_HOST`:$DB_PORT is reachable" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "[FAIL] Cannot reach $DB_HOST`:$DB_PORT" -ForegroundColor Red
        return $false
    }
}

function Get-ServicePid([string]$Name) {
    $f = Join-Path $PidDir "$Name.pid"
    if (Test-Path $f) { return [int](Get-Content $f -Raw).Trim() }
    return $null
}

function Test-Running([int]$Pid) {
    if (-not $Pid) { return $false }
    try { return $null -ne (Get-Process -Id $Pid -ErrorAction SilentlyContinue) } catch { return $false }
}

function Start-Service($svc) {
    $existingPid = Get-ServicePid $svc.name
    if ($existingPid -and (Test-Running $existingPid)) {
        Write-Host "[SKIP] $($svc.name) already running (PID $existingPid)" -ForegroundColor Gray
        return $true
    }

    $jarPath = Join-Path $ProjectRoot $svc.jar
    if (-not (Test-Path $jarPath)) {
        Write-Host "[ERROR] JAR not found: $($svc.jar)" -ForegroundColor Red
        return $false
    }

    $logFile = Join-Path $LogDir "$($svc.name).log"
    $errFile = Join-Path $LogDir "$($svc.name).err.log"

    # Determine migration directory name (jira-xyz-service)
    $svcDirName = "jira-" + ($svc.name -replace "-service$", "") + "-service"
    $migPath = Join-Path $MigDir $svcDirName

    # Build environment - use SPRING_DATASOURCE_URL to override profile-level hardcoded URLs
    $dbUrl = "jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
    $envVars = @{
        SPRING_PROFILES_ACTIVE = "local"
        SPRING_DATASOURCE_URL = $dbUrl
        SPRING_DATASOURCE_USERNAME = $DB_USERNAME
        SPRING_DATASOURCE_PASSWORD = $DB_PASSWORD
        SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA = $svc.schema
        SPRING_FLYWAY_ENABLED = "true"
        SPRING_FLYWAY_BASELINE_ON_MIGRATE = "true"
        SPRING_FLYWAY_VALIDATE_ON_MIGRATE = "false"
        SPRING_FLYWAY_CREATE_SCHEMAS = "true"
        SPRING_FLYWAY_DEFAULT_SCHEMA = $svc.schema
        SPRING_FLYWAY_SCHEMAS = $svc.schema
    }

    # Use filesystem migrations if available (fixes PG 11 compatibility)
    if (Test-Path $migPath) {
        $envVars["SPRING_FLYWAY_LOCATIONS"] = "filesystem:$migPath"
    }

    # Add extra env vars
    foreach ($key in $svc.extra.Keys) {
        $envVars[$key] = $svc.extra[$key]
    }

    # Save and set env
    $saved = @{}
    foreach ($key in $envVars.Keys) {
        $saved[$key] = [Environment]::GetEnvironmentVariable($key, "Process")
        [Environment]::SetEnvironmentVariable($key, $envVars[$key], "Process")
    }

    try {
        $proc = Start-Process -FilePath $JavaPath `
            -ArgumentList @("-Xms$($svc.mem)", "-Xmx$($svc.mem)", "-jar", $jarPath, "--server.port=$($svc.port)") `
            -WorkingDirectory $ProjectRoot `
            -WindowStyle Hidden `
            -RedirectStandardOutput $logFile `
            -RedirectStandardError $errFile `
            -PassThru

        if (-not $proc -or $proc.HasExited) {
            Write-Host "[ERROR] $($svc.name) exited immediately" -ForegroundColor Red
            return $false
        }

        Set-Content -Path (Join-Path $PidDir "$($svc.name).pid") -Value $proc.Id -NoNewline
        Write-Host "[START] $($svc.name) :$($svc.port) (PID $($proc.Id))" -ForegroundColor Green
        return $true
    } finally {
        foreach ($key in $saved.Keys) {
            if ($null -eq $saved[$key]) {
                [Environment]::SetEnvironmentVariable($key, $null, "Process")
            } else {
                [Environment]::SetEnvironmentVariable($key, $saved[$key], "Process")
            }
        }
    }
}

function Start-Gateway {
    $jarPath = Join-Path $ProjectRoot "jira-gateway\target\jira-gateway-1.0.0.jar"
    if (-not (Test-Path $jarPath)) {
        Write-Host "[ERROR] Gateway JAR not found" -ForegroundColor Red
        return $false
    }

    $existingPid = Get-ServicePid "gateway"
    if ($existingPid -and (Test-Running $existingPid)) {
        Write-Host "[SKIP] gateway already running (PID $existingPid)" -ForegroundColor Gray
        return $true
    }

    $logFile = Join-Path $LogDir "gateway.log"
    $errFile = Join-Path $LogDir "gateway.err.log"

    $saved = @{}
    foreach ($key in @("SPRING_PROFILES_ACTIVE","JWT_SECRET")) {
        $saved[$key] = [Environment]::GetEnvironmentVariable($key, "Process")
    }
    [Environment]::SetEnvironmentVariable("SPRING_PROFILES_ACTIVE", "local", "Process")
    [Environment]::SetEnvironmentVariable("JWT_SECRET", $JWT_SECRET, "Process")

    try {
        $proc = Start-Process -FilePath $JavaPath `
            -ArgumentList @("-Xms512m", "-Xmx512m", "-jar", $jarPath, "--server.port=8080") `
            -WorkingDirectory $ProjectRoot `
            -WindowStyle Hidden `
            -RedirectStandardOutput $logFile `
            -RedirectStandardError $errFile `
            -PassThru

        if (-not $proc -or $proc.HasExited) {
            Write-Host "[ERROR] gateway exited immediately" -ForegroundColor Red
            return $false
        }

        Set-Content -Path (Join-Path $PidDir "gateway.pid") -Value $proc.Id -NoNewline
        Write-Host "[START] gateway :8080 (PID $($proc.Id))" -ForegroundColor Green
        return $true
    } finally {
        foreach ($key in $saved.Keys) {
            if ($null -eq $saved[$key]) {
                [Environment]::SetEnvironmentVariable($key, $null, "Process")
            } else {
                [Environment]::SetEnvironmentVariable($key, $saved[$key], "Process")
            }
        }
    }
}

function Start-Frontend {
    $frontendDir = Join-Path $ProjectRoot "jira-frontend"
    if (-not (Test-Path $frontendDir)) {
        Write-Host "[ERROR] Frontend directory not found" -ForegroundColor Red
        return $false
    }

    $existingPid = Get-ServicePid "frontend"
    if ($existingPid -and (Test-Running $existingPid)) {
        Write-Host "[SKIP] frontend already running (PID $existingPid)" -ForegroundColor Gray
        return $true
    }

    $logFile = Join-Path $LogDir "frontend.log"
    $errFile = Join-Path $LogDir "frontend.err.log"

    [Environment]::SetEnvironmentVariable("VITE_API_GATEWAY_URL", "http://localhost:8080", "Process")

    $proc = Start-Process -FilePath "npm.cmd" `
        -ArgumentList @("run", "dev") `
        -WorkingDirectory $frontendDir `
        -WindowStyle Hidden `
        -RedirectStandardOutput $logFile `
        -RedirectStandardError $errFile `
        -PassThru

    if (-not $proc -or $proc.HasExited) {
        Write-Host "[ERROR] frontend exited immediately" -ForegroundColor Red
        return $false
    }

    Set-Content -Path (Join-Path $PidDir "frontend.pid") -Value $proc.Id -NoNewline
    Write-Host "[START] frontend :3000 (PID $($proc.Id))" -ForegroundColor Green
    return $true
}

# ============================================================
# MAIN
# ============================================================

if ($Stop) {
    Write-Host "`nStopping all services..." -ForegroundColor Yellow
    Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue
    Stop-Process -Name "node" -Force -ErrorAction SilentlyContinue
    Stop-Process -Name "npm" -Force -ErrorAction SilentlyContinue
    if (Test-Path $PidDir) { Get-ChildItem $PidDir -Filter "*.pid" | Remove-Item -Force }
    Write-Host "All services stopped.`n" -ForegroundColor Green
    return
}

if ($Status) {
    Write-Host ""
    Write-Host "  JIRA PLATFORM STATUS (External DB: $DB_HOST)" -ForegroundColor Yellow
    Write-Host "  ================================================" -ForegroundColor Yellow
    foreach ($svc in $AllServices) {
        $pid = Get-ServicePid $svc.name
        $up = Test-Running $pid
        $st = if ($up) { "RUNNING" } else { "STOPPED" }
        $cl = if ($up) { "Green" } else { "Red" }
        Write-Host ("  {0,-25} " -f $svc.name) -NoNewline
        Write-Host ("{0,-10}" -f $st) -NoNewline -ForegroundColor $cl
        Write-Host " :$($svc.port)" -ForegroundColor Gray
    }
    foreach ($name in @("gateway","frontend")) {
        $pid = Get-ServicePid $name
        $up = Test-Running $pid
        $st = if ($up) { "RUNNING" } else { "STOPPED" }
        $cl = if ($up) { "Green" } else { "Red" }
        $pt = if ($name -eq "gateway") { "8080" } else { "3000" }
        Write-Host ("  {0,-25} " -f $name) -NoNewline
        Write-Host ("{0,-10}" -f $st) -NoNewline -ForegroundColor $cl
        Write-Host " :$pt" -ForegroundColor Gray
    }
    Write-Host ""
    return
}

# STARTUP
Clear-Host
Write-Host ""
Write-Host "  =============================================================" -ForegroundColor Yellow
Write-Host "  :      JIRA PLATFORM - EXTERNAL PostgreSQL MODE              :" -ForegroundColor Yellow
Write-Host "  :      Database: $DB_HOST / $DB_NAME                     :" -ForegroundColor Yellow
Write-Host "  =============================================================" -ForegroundColor Yellow
Write-Host ""

Initialize-Dirs
if (-not (Test-DbConnection)) { return }

$services = if ($Core) {
    $AllServices | Where-Object { $CoreServiceNames -contains $_.name }
} else {
    $AllServices
}

Write-Host ""
$mode = if ($Core) { "core" } else { "all" }
Write-Host "Starting $mode services..." -ForegroundColor Cyan
Write-Host ""

foreach ($svc in $services) {
    Start-Service $svc | Out-Null
}

Write-Host ""
Write-Host "Starting gateway..." -ForegroundColor Cyan
Start-Sleep -Seconds 5
Start-Gateway | Out-Null

Write-Host ""
Write-Host "Starting frontend..." -ForegroundColor Cyan
Start-Sleep -Seconds 3
Start-Frontend | Out-Null

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Green
Write-Host "  Services started! (External DB: $DB_HOST/$DB_NAME)" -ForegroundColor Green
Write-Host "  Frontend:  http://localhost:3000" -ForegroundColor Green
Write-Host "  Gateway:   http://localhost:8080" -ForegroundColor Green
Write-Host "  Swagger:   http://localhost:8080/swagger-ui.html" -ForegroundColor Green
Write-Host "  Logs:      platform-runtime\logs\" -ForegroundColor Gray
Write-Host "==============================================================" -ForegroundColor Green
Write-Host ""

Write-Host "Waiting for services to be ready..." -ForegroundColor Cyan
Start-Sleep -Seconds 30

Write-Host ""
Write-Host "Service Health Check:" -ForegroundColor Cyan
foreach ($svc in $services) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$($svc.port)/actuator/health" -TimeoutSec 5 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            Write-Host "  $($svc.name) :$($svc.port) - UP" -ForegroundColor Green
        } else {
            Write-Host "  $($svc.name) :$($svc.port) - DOWN" -ForegroundColor Red
        }
    } catch {
        Write-Host "  $($svc.name) :$($svc.port) - STARTING..." -ForegroundColor Yellow
    }
}

Write-Host ""
Start-Process "http://localhost:3000"
