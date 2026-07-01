# Systems and Avionics -- Demo Launcher
# Starts all services against external PostgreSQL, verifies health, opens browser.
#
# Usage:
#   demo.bat                    (double-click or run from cmd)
#   .\demo-launch.ps1           (from PowerShell)
#   .\demo-launch.ps1 -Core     (core services only - saves memory)
#   .\demo-launch.ps1 -Stop     (stop everything)
#   .\demo-launch.ps1 -Status   (check what's running)

param(
    [switch]$Stop,
    [switch]$Status,
    [switch]$Core
)

$ErrorActionPreference = "SilentlyContinue"
$ProjectRoot = $PSScriptRoot
$LogDir = Join-Path $ProjectRoot "platform-runtime\logs"
$PidDir = Join-Path $ProjectRoot "platform-runtime\pids"

# -- External DB --
$DB_HOST     = "in0-eplmdb-v01"
$DB_PORT     = "5432"
$DB_NAME     = "systems"
$DB_USERNAME = "systems_admin"
$DB_PASSWORD = "Hcu4ieD8R13qaf7JVSsu"
$JWT_SECRET  = "jira-platform-super-secret-key-that-is-at-least-256-bits-long"

# -- Find Java 21 --
$JavaPath = $null
@(
    "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.2\jbr\bin\java.exe",
    "C:\Program Files\JetBrains\IntelliJ IDEA *\jbr\bin\java.exe",
    "C:\Program Files\Java\jdk-21*\bin\java.exe",
    "C:\Program Files\Eclipse Adoptium\jdk-21*\bin\java.exe",
    "C:\Program Files\Eclipse Foundation\jdk-21*\bin\java.exe",
    "$env:JAVA_HOME\bin\java.exe"
) | ForEach-Object {
    if (-not $JavaPath) {
        $r = Get-Item $_ -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($r) { $JavaPath = $r.FullName }
    }
}
if (-not $JavaPath) { $JavaPath = "java.exe" }

# -- Service definitions --
$AllServices = @(
    @{ name="auth-service";         port=8081; schema="jira_auth";         jar="jira-auth-service\target\jira-auth-service-1.0.0.jar";                     mem="256m"; extra=@{JWT_SECRET=$JWT_SECRET; JWT_EXPIRATION_MS="86400000"; JWT_REFRESH_EXPIRATION_MS="604800000"} },
    @{ name="user-service";         port=8082; schema="jira_user";         jar="jira-user-service\target\jira-user-service-1.0.0.jar";                     mem="256m"; extra=@{} },
    @{ name="project-service";      port=8083; schema="jira_project";      jar="jira-project-service\target\jira-project-service-1.0.0.jar";               mem="256m"; extra=@{} },
    @{ name="issue-service";        port=8084; schema="jira_issue";        jar="jira-issue-service\target\jira-issue-service-1.0.0.jar";                   mem="384m"; extra=@{WORKFLOW_SERVICE_URL="http://localhost:8085"} },
    @{ name="workflow-service";     port=8085; schema="jira_workflow";      jar="jira-workflow-service\target\jira-workflow-service-1.0.0.jar";             mem="256m"; extra=@{} },
    @{ name="comment-service";      port=8086; schema="jira_comment";      jar="jira-comment-service\target\jira-comment-service-1.0.0.jar";               mem="192m"; extra=@{} },
    @{ name="sprint-service";       port=8091; schema="jira_sprint";       jar="jira-sprint-service\target\jira-sprint-service-1.0.0.jar";                 mem="192m"; extra=@{} },
    @{ name="search-service";       port=8088; schema="jira_search";       jar="jira-search-service\target\jira-search-service-1.0.0.jar";                 mem="192m"; extra=@{} },
    @{ name="audit-service";        port=8089; schema="jira_audit";        jar="jira-audit-service\target\jira-audit-service-1.0.0.jar";                   mem="192m"; extra=@{} },
    @{ name="notification-service"; port=8087; schema="jira_notification"; jar="jira-notification-service\target\jira-notification-service-1.0.0.jar";     mem="192m"; extra=@{} },
    @{ name="attachment-service";   port=8090; schema="jira_attachment";   jar="jira-attachment-service\target\jira-attachment-service-1.0.0.jar";          mem="256m"; extra=@{ATTACHMENT_STORAGE_PATH="platform-runtime\attachments"} },
    @{ name="plan-service";         port=8092; schema="jira_plan";         jar="jira-plan-service\target\jira-plan-service-1.0.0.jar";                     mem="192m"; extra=@{} },
    @{ name="admin-service";        port=8093; schema="jira_admin";        jar="jira-admin-service\target\jira-admin-service-1.0.0.jar";                   mem="192m"; extra=@{} },
    @{ name="migration-service";    port=8094; schema="jira_migration";    jar="jira-migration-service\target\jira-migration-service-1.0.0.jar";           mem="256m"; extra=@{} },
    @{ name="version-service";      port=8096; schema="jira_version";      jar="jira-version-service\target\jira-version-service-1.0.0.jar";               mem="192m"; extra=@{} },
    @{ name="component-service";    port=8097; schema="jira_component";    jar="jira-component-service\target\jira-component-service-1.0.0.jar";           mem="192m"; extra=@{} },
    @{ name="dashboard-service";    port=8098; schema="jira_dashboard";    jar="jira-dashboard-service\target\jira-dashboard-service-1.0.0.jar";           mem="192m"; extra=@{} },
    @{ name="test-service";         port=8099; schema="jira_test";         jar="jira-test-service\target\jira-test-service-1.0.0.jar";                     mem="192m"; extra=@{} }
)

$CoreNames = @("auth-service","user-service","project-service","issue-service","workflow-service")

# -- Helpers --
function Initialize-Dirs {
    @($LogDir, $PidDir, (Join-Path $ProjectRoot "platform-runtime\attachments")) | ForEach-Object {
        if (-not (Test-Path $_)) { New-Item -ItemType Directory -Path $_ -Force | Out-Null }
    }
}

function Get-Pid([string]$n)  { $f = Join-Path $PidDir "$n.pid"; if (Test-Path $f) { return [int](Get-Content $f -Raw).Trim() }; return $null }
function Test-Up([int]$p)     { if (-not $p) { return $false }; try { return $null -ne (Get-Process -Id $p -ErrorAction Stop) } catch { return $false } }

function Start-Svc($svc) {
    $ep = Get-Pid $svc.name
    if ($ep -and (Test-Up $ep)) { Write-Host "  [SKIP] $($svc.name) already running (PID $ep)" -ForegroundColor Gray; return $true }

    $jarPath = Join-Path $ProjectRoot $svc.jar
    if (-not (Test-Path $jarPath)) { Write-Host "  [MISS] $($svc.name) - JAR not found" -ForegroundColor DarkYellow; return $false }

    $logFile = Join-Path $LogDir "$($svc.name).log"
    $errFile = Join-Path $LogDir "$($svc.name).err.log"
    $migDir  = Join-Path $ProjectRoot "migrations-external" ("jira-" + ($svc.name -replace "-service$","") + "-service")

    $dbUrl = "jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
    $env = @{
        SPRING_PROFILES_ACTIVE = "local"
        SPRING_DATASOURCE_URL = $dbUrl; SPRING_DATASOURCE_USERNAME = $DB_USERNAME; SPRING_DATASOURCE_PASSWORD = $DB_PASSWORD
        SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA = $svc.schema
        SPRING_FLYWAY_ENABLED = "true"; SPRING_FLYWAY_BASELINE_ON_MIGRATE = "true"; SPRING_FLYWAY_VALIDATE_ON_MIGRATE = "false"
        SPRING_FLYWAY_CREATE_SCHEMAS = "true"; SPRING_FLYWAY_DEFAULT_SCHEMA = $svc.schema; SPRING_FLYWAY_SCHEMAS = $svc.schema
    }
    if (Test-Path $migDir) { $env["SPRING_FLYWAY_LOCATIONS"] = "filesystem:$migDir" }
    foreach ($k in $svc.extra.Keys) { $env[$k] = $svc.extra[$k] }

    $saved = @{}
    foreach ($k in $env.Keys) { $saved[$k] = [Environment]::GetEnvironmentVariable($k,"Process"); [Environment]::SetEnvironmentVariable($k,$env[$k],"Process") }

    try {
        $proc = Start-Process -FilePath $JavaPath -ArgumentList @("-Xms128m","-Xmx$($svc.mem)","-jar",$jarPath,"--server.port=$($svc.port)") `
            -WorkingDirectory $ProjectRoot -WindowStyle Hidden -RedirectStandardOutput $logFile -RedirectStandardError $errFile -PassThru
        if (-not $proc -or $proc.HasExited) { Write-Host "  [FAIL] $($svc.name)" -ForegroundColor Red; return $false }
        Set-Content -Path (Join-Path $PidDir "$($svc.name).pid") -Value $proc.Id -NoNewline
        Write-Host "  [OK]   $($svc.name) :$($svc.port) (PID $($proc.Id))" -ForegroundColor Green
        return $true
    } finally {
        foreach ($k in $saved.Keys) { [Environment]::SetEnvironmentVariable($k, $saved[$k], "Process") }
    }
}

function Start-Gateway {
    $jp = Join-Path $ProjectRoot "jira-gateway\target\jira-gateway-1.0.0.jar"
    if (-not (Test-Path $jp)) { Write-Host "  [MISS] gateway JAR not found" -ForegroundColor Red; return $false }
    $ep = Get-Pid "gateway"; if ($ep -and (Test-Up $ep)) { Write-Host "  [SKIP] gateway already running" -ForegroundColor Gray; return $true }
    $saved = @{}
    foreach ($k in @("SPRING_PROFILES_ACTIVE","JWT_SECRET")) { $saved[$k] = [Environment]::GetEnvironmentVariable($k,"Process") }
    [Environment]::SetEnvironmentVariable("SPRING_PROFILES_ACTIVE","local","Process")
    [Environment]::SetEnvironmentVariable("JWT_SECRET",$JWT_SECRET,"Process")
    try {
        $proc = Start-Process -FilePath $JavaPath -ArgumentList @("-Xms128m","-Xmx384m","-jar",$jp,"--server.port=8080") `
            -WorkingDirectory $ProjectRoot -WindowStyle Hidden `
            -RedirectStandardOutput (Join-Path $LogDir "gateway.log") -RedirectStandardError (Join-Path $LogDir "gateway.err.log") -PassThru
        if (-not $proc -or $proc.HasExited) { Write-Host "  [FAIL] gateway" -ForegroundColor Red; return $false }
        Set-Content -Path (Join-Path $PidDir "gateway.pid") -Value $proc.Id -NoNewline
        Write-Host "  [OK]   gateway :8080 (PID $($proc.Id))" -ForegroundColor Green
        return $true
    } finally { foreach ($k in $saved.Keys) { [Environment]::SetEnvironmentVariable($k,$saved[$k],"Process") } }
}

function Start-Frontend {
    $fd = Join-Path $ProjectRoot "jira-frontend"
    if (-not (Test-Path $fd)) { Write-Host "  [MISS] frontend dir not found" -ForegroundColor Red; return $false }
    $ep = Get-Pid "frontend"; if ($ep -and (Test-Up $ep)) { Write-Host "  [SKIP] frontend already running" -ForegroundColor Gray; return $true }
    [Environment]::SetEnvironmentVariable("VITE_API_GATEWAY_URL","http://localhost:8080","Process")
    $proc = Start-Process -FilePath "npm.cmd" -ArgumentList @("run","dev") -WorkingDirectory $fd -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $LogDir "frontend.log") -RedirectStandardError (Join-Path $LogDir "frontend.err.log") -PassThru
    if (-not $proc -or $proc.HasExited) { Write-Host "  [FAIL] frontend" -ForegroundColor Red; return $false }
    Set-Content -Path (Join-Path $PidDir "frontend.pid") -Value $proc.Id -NoNewline
    Write-Host "  [OK]   frontend :3000 (PID $($proc.Id))" -ForegroundColor Green
    return $true
}

function Show-Status {
    Write-Host ""
    Write-Host "  SERVICE                     STATUS      PORT" -ForegroundColor Yellow
    Write-Host "  ---------------------------------------------" -ForegroundColor Yellow
    foreach ($svc in $AllServices) {
        $pid = Get-Pid $svc.name; $up = Test-Up $pid
        $health = ""
        if ($up) { try { $r = Invoke-WebRequest -Uri "http://localhost:$($svc.port)/actuator/health" -TimeoutSec 3 -ErrorAction Stop; $health = if ($r.StatusCode -eq 200) {"HEALTHY"} else {"DEGRADED"} } catch { $health = "STARTING" } }
        $st = if ($up) { $health } else { "STOPPED" }
        $cl = switch ($st) { "HEALTHY" {"Green"} "STARTING" {"Yellow"} "DEGRADED" {"DarkYellow"} default {"Red"} }
        Write-Host ("  {0,-25} " -f $svc.name) -NoNewline; Write-Host ("{0,-12}" -f $st) -NoNewline -ForegroundColor $cl; Write-Host ":$($svc.port)" -ForegroundColor Gray
    }
    foreach ($n in @("gateway","frontend")) {
        $pid = Get-Pid $n; $up = Test-Up $pid; $pt = if ($n -eq "gateway") {"8080"} else {"3000"}
        $st = if ($up) {"RUNNING"} else {"STOPPED"}; $cl = if ($up) {"Green"} else {"Red"}
        Write-Host ("  {0,-25} " -f $n) -NoNewline; Write-Host ("{0,-12}" -f $st) -NoNewline -ForegroundColor $cl; Write-Host ":$pt" -ForegroundColor Gray
    }
    Write-Host ""
}

function Wait-ForHealth {
    param([int[]]$Ports, [int]$TimeoutSec = 90)
    Write-Host ""
    Write-Host "  Waiting for services (up to ${TimeoutSec}s)..." -ForegroundColor Cyan
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    $remaining = [System.Collections.Generic.HashSet[int]]::new()
    $Ports | ForEach-Object { $remaining.Add($_) | Out-Null }

    while ($remaining.Count -gt 0 -and (Get-Date) -lt $deadline) {
        $done = @()
        foreach ($p in $remaining) {
            try {
                $r = Invoke-WebRequest -Uri "http://localhost:${p}/actuator/health" -TimeoutSec 2 -ErrorAction Stop
                if ($r.StatusCode -eq 200) { $done += $p }
            } catch {}
        }
        foreach ($p in $done) { $remaining.Remove($p) | Out-Null; Write-Host "    :$p UP" -ForegroundColor Green }
        if ($remaining.Count -gt 0) { Start-Sleep -Seconds 3 }
    }
    if ($remaining.Count -gt 0) {
        Write-Host "    Still starting: $($remaining -join ', ')" -ForegroundColor Yellow
    }
}

# =============================================================
#  MAIN
# =============================================================

if ($Stop) {
    Write-Host "`n  Stopping all services..." -ForegroundColor Yellow
    Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue
    Stop-Process -Name "node" -Force -ErrorAction SilentlyContinue
    Stop-Process -Name "npm"  -Force -ErrorAction SilentlyContinue
    if (Test-Path $PidDir) { Get-ChildItem $PidDir -Filter "*.pid" | Remove-Item -Force }
    Write-Host "  All services stopped.`n" -ForegroundColor Green
    return
}

if ($Status) { Show-Status; return }

# -- Startup --
Clear-Host
Write-Host ""
Write-Host "  +---------------------------------------------------+" -ForegroundColor Cyan
Write-Host "  |     SYSTEMS AND AVIONICS -- DEMO LAUNCHER        |" -ForegroundColor Cyan
Write-Host "  |     Database: $DB_HOST / $DB_NAME           |" -ForegroundColor Cyan
Write-Host "  |     Java: $(Split-Path $JavaPath -Leaf)                            |" -ForegroundColor Cyan
Write-Host "  +---------------------------------------------------+" -ForegroundColor Cyan
Write-Host ""

Initialize-Dirs

# Test DB
Write-Host "  Testing database connection..." -ForegroundColor Cyan
try {
    $tcp = New-Object System.Net.Sockets.TcpClient($DB_HOST, [int]$DB_PORT)
    $tcp.Close()
    Write-Host "  [OK] $DB_HOST`:$DB_PORT reachable" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] Cannot reach $DB_HOST`:$DB_PORT - check VPN" -ForegroundColor Red
    return
}

# Stop stale processes
Write-Host ""
Write-Host "  Cleaning up stale processes..." -ForegroundColor Yellow
Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 3

# Select services
$services = if ($Core) {
    Write-Host "  Mode: CORE (5 services + gateway + frontend)" -ForegroundColor Cyan
    $AllServices | Where-Object { $CoreNames -contains $_.name }
} else {
    Write-Host "  Mode: FULL (all services + gateway + frontend)" -ForegroundColor Cyan
    $AllServices
}

# Start services
Write-Host ""
Write-Host "  Starting backend services..." -ForegroundColor Cyan
Write-Host ""
foreach ($svc in $services) { Start-Svc $svc | Out-Null }

# Start gateway
Write-Host ""
Write-Host "  Starting gateway..." -ForegroundColor Cyan
Start-Sleep -Seconds 5
Start-Gateway | Out-Null

# Start frontend
Write-Host ""
Write-Host "  Starting frontend..." -ForegroundColor Cyan
Start-Frontend | Out-Null

# Wait for health
$healthPorts = @(8081, 8083, 8084, 8080)
Wait-ForHealth -Ports $healthPorts -TimeoutSec 90

# Final status
Write-Host ""
Write-Host "  +---------------------------------------------------+" -ForegroundColor Green
Write-Host "  |  DEMO READY                                     |" -ForegroundColor Green
Write-Host "  |                                                 |" -ForegroundColor Green
Write-Host "  |  App:     http://localhost:3000                 |" -ForegroundColor Green
Write-Host "  |  Gateway: http://localhost:8080                 |" -ForegroundColor Green
Write-Host "  |  Swagger: http://localhost:8080/swagger-ui.html |" -ForegroundColor Green
Write-Host "  |  Logs:    platform-runtime\logs\                |" -ForegroundColor Green
Write-Host "  +---------------------------------------------------+" -ForegroundColor Green
Write-Host ""

# Open browser
Start-Process "http://localhost:3000"
