# Jira Platform - External PostgreSQL Startup
# Uses external PostgreSQL (in0-eplmdb-v01) instead of local/Docker PostgreSQL
#
# Usage:
#   .\start-platform-external.ps1          # Start all services
#   .\start-platform-external.ps1 -Stop    # Stop all services
#   .\start-platform-external.ps1 -Status  # Check service status
#   .\start-platform-external.ps1 -Restart # Restart all services

param(
    [switch]$Stop,
    [switch]$Status,
    [switch]$Restart
)

$ErrorActionPreference = "SilentlyContinue"
$ProjectRoot = $PSScriptRoot
$RuntimeDir = Join-Path $ProjectRoot "platform-runtime"
$LogDir = Join-Path $RuntimeDir "logs"
$PidDir = Join-Path $RuntimeDir "pids"
$ConfigPath = Join-Path $ProjectRoot "config\services-external.json"
$StatusFile = Join-Path $RuntimeDir ".runtime-status"

# ============================================================
# UTILITY FUNCTIONS (same as start-platform.ps1)
# ============================================================

function Initialize-Runtime {
    $dirs = @($LogDir, $PidDir, (Join-Path $RuntimeDir "attachments"))
    foreach ($dir in $dirs) {
        if (-not (Test-Path $dir)) {
            New-Item -ItemType Directory -Path $dir -Force | Out-Null
        }
    }
    if (-not (Test-Path $ConfigPath)) {
        Write-Host "[ERROR] Configuration file not found: $ConfigPath" -ForegroundColor Red
        return $false
    }
    return $true
}

function Test-ExternalDbConnection {
    Write-Host "Testing connection to external PostgreSQL (in0-eplmdb-v01:5432)..." -ForegroundColor Cyan
    try {
        $socket = New-Object System.Net.Sockets.TcpClient
        $socket.Connect("in0-eplmdb-v01", 5432)
        $socket.Close()
        Write-Host "[OK] External PostgreSQL is reachable" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "[ERROR] Cannot reach external PostgreSQL at in0-eplmdb-v01:5432" -ForegroundColor Red
        Write-Host "  Check network connectivity and firewall rules" -ForegroundColor Yellow
        return $false
    }
}

function Get-Services {
    $config = Get-Content $ConfigPath -Raw | ConvertFrom-Json
    return $config.services
}

function Save-ServicePid {
    param([string]$ServiceName, [int]$ProcessId)
    $pidFile = Join-Path $PidDir "$ServiceName.pid"
    Set-Content -Path $pidFile -Value $ProcessId -NoNewline
}

function Clear-ServicePid {
    param([string]$ServiceName)
    $pidFile = Join-Path $PidDir "$ServiceName.pid"
    if (Test-Path $pidFile) { Remove-Item $pidFile -Force }
}

function Get-ServicePid {
    param([string]$ServiceName)
    $pidFile = Join-Path $PidDir "$ServiceName.pid"
    if (Test-Path $pidFile) { return [int](Get-Content $pidFile -Raw).Trim() }
    return $null
}

function Test-ProcessRunning {
    param([int]$ProcessId)
    if ($ProcessId -eq 0 -or $ProcessId -eq $null) { return $false }
    try { $process = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue; return $null -ne $process } catch { return $false }
}

# ============================================================
# FAST SERVICE STARTUP
# ============================================================

function Find-JavaPath {
    $javaHome = $env:JAVA_HOME
    if ($javaHome -and (Test-Path "$javaHome\bin\java.exe")) { return "$javaHome\bin\java.exe" }
    $commonPaths = @(
        "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.2\jbr\bin\java.exe",
        "C:\Program Files\Java\jdk-21\bin\java.exe",
        "C:\Program Files\Java\jdk-17\bin\java.exe",
        "C:\Program Files\Java\jdk-11\bin\java.exe"
    )
    foreach ($path in $commonPaths) {
        if (Test-Path $path) { return $path }
    }
    return "java.exe"
}

function Get-ServiceModuleDir {
    param([object]$Service, [string]$ProjectRoot)
    if ($Service.name -eq "gateway") {
        return Join-Path $ProjectRoot "jira-gateway"
    }
    $short = $Service.name -replace "-service$", ""
    return Join-Path $ProjectRoot "jira-$short-service"
}

function Find-JarPath {
    param([object]$Service, [string]$ProjectRoot)
    if ($Service.jarName) {
        $explicit = Join-Path (Get-ServiceModuleDir -Service $Service -ProjectRoot $ProjectRoot) "target\$($Service.jarName)"
        if (Test-Path $explicit) { return $explicit }
    }
    $jarDir = Join-Path (Get-ServiceModuleDir -Service $Service -ProjectRoot $ProjectRoot) "target"
    if (-not (Test-Path $jarDir)) { return $null }
    $jars = Get-ChildItem -Path $jarDir -Filter "*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch "original|classes" } |
        Sort-Object Length -Descending
    if ($jars) { return $jars[0].FullName }
    return $null
}

function Set-ServiceProcessEnvironment {
    param([object]$Service)
    $saved = @{}
    if (-not $Service.environment) { return $saved }
    foreach ($key in $Service.environment.PSObject.Properties.Name) {
        $saved[$key] = [Environment]::GetEnvironmentVariable($key, "Process")
        [Environment]::SetEnvironmentVariable($key, [string]$Service.environment.$key, "Process")
    }
    return $saved
}

function Restore-ServiceProcessEnvironment {
    param([hashtable]$Saved)
    foreach ($key in $Saved.Keys) {
        if ($null -eq $Saved[$key]) {
            [Environment]::SetEnvironmentVariable($key, $null, "Process")
        } else {
            [Environment]::SetEnvironmentVariable($key, $Saved[$key], "Process")
        }
    }
}

function Start-ServiceFast {
    param([object]$Service, [string]$ProjectRoot)

    $existingPid = Get-ServicePid -ServiceName $Service.name
    if ($existingPid -and (Test-ProcessRunning -ProcessId $existingPid)) {
        Write-Host "[SKIP] $($Service.displayName) already running" -ForegroundColor Gray
        return $true
    }

    $logFile = Join-Path $LogDir "$($Service.name).log"
    $errFile = Join-Path $LogDir "$($Service.name).err.log"

    try {
        if ($Service.type -eq "backend" -or $Service.type -eq "gateway") {
            $javaPath = Find-JavaPath
            $jarPath = Find-JarPath -Service $Service -ProjectRoot $ProjectRoot

            if (-not $jarPath) {
                $moduleDir = Get-ServiceModuleDir -Service $Service -ProjectRoot $ProjectRoot
                $moduleName = Split-Path -Leaf $moduleDir
                Write-Host "[ERROR] JAR not found for $($Service.name) (build: mvn -pl $moduleName package -DskipTests)" -ForegroundColor Red
                return $false
            }

            $heapSize = if ($Service.memory) { $Service.memory } else { "256m" }
            $argList = @("-Xms$heapSize", "-Xmx$heapSize", "-jar", $jarPath, "--server.port=$($Service.port)")

            $envBackup = Set-ServiceProcessEnvironment -Service $Service
            try {
                $proc = Start-Process -FilePath $javaPath `
                    -ArgumentList $argList `
                    -WorkingDirectory $ProjectRoot `
                    -WindowStyle Hidden `
                    -RedirectStandardOutput $logFile `
                    -RedirectStandardError $errFile `
                    -PassThru
            } finally {
                Restore-ServiceProcessEnvironment -Saved $envBackup
            }

            if (-not $proc -or $proc.HasExited) {
                Write-Host "[ERROR] $($Service.name) exited immediately - see $logFile" -ForegroundColor Red
                return $false
            }

            Save-ServicePid -ServiceName $Service.name -ProcessId $proc.Id
            Write-Host "[START] $($Service.displayName) (PID: $($proc.Id))" -ForegroundColor Green
            return $true

        } elseif ($Service.type -eq "frontend") {
            $frontendDir = Join-Path $ProjectRoot "jira-frontend"
            if (-not (Test-Path $frontendDir)) {
                Write-Host "[ERROR] Frontend directory not found" -ForegroundColor Red
                return $false
            }

            $envBackup = Set-ServiceProcessEnvironment -Service $Service
            [Environment]::SetEnvironmentVariable("FORCE_COLOR", "1", "Process")
            try {
                $proc = Start-Process -FilePath "npm.cmd" `
                    -ArgumentList @("run", "dev") `
                    -WorkingDirectory $frontendDir `
                    -WindowStyle Hidden `
                    -RedirectStandardOutput $logFile `
                    -RedirectStandardError $errFile `
                    -PassThru
            } finally {
                Restore-ServiceProcessEnvironment -Saved $envBackup
            }

            if (-not $proc -or $proc.HasExited) {
                Write-Host "[ERROR] frontend exited immediately - see $logFile" -ForegroundColor Red
                return $false
            }

            Save-ServicePid -ServiceName $Service.name -ProcessId $proc.Id
            Write-Host "[START] $($Service.displayName) (PID: $($proc.Id))" -ForegroundColor Green
            return $true
        }
        return $false
    } catch {
        Write-Host "[ERROR] Failed to start $($Service.name): $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

function Test-ServiceReady {
    param([int]$Port)
    try {
        $socket = New-Object System.Net.Sockets.TcpClient
        $socket.Connect("localhost", $Port)
        $socket.Close()
        return $true
    } catch { return $false }
}

function Wait-ForServicesReady {
    param([array]$Ports, [int]$TimeoutSeconds = 60)
    Write-Host ""
    Write-Host "Checking services readiness..." -ForegroundColor Cyan
    $startTime = Get-Date
    $allReady = $false
    while (-not $allReady -and ((Get-Date) - $startTime).TotalSeconds -lt $TimeoutSeconds) {
        $allReady = $true
        foreach ($port in $Ports) {
            if (-not (Test-ServiceReady -Port $port)) { $allReady = $false }
        }
        if (-not $allReady) { Start-Sleep -Seconds 2 }
    }
    return $allReady
}

function Open-PlatformBrowsers {
    param([int]$GatewayPort = 8080, [int]$FrontendPort = 3000, [int]$TimeoutSeconds = 90)
    Write-Host "Waiting for gateway (:$GatewayPort) and frontend (:$FrontendPort)..." -ForegroundColor Cyan
    $ready = Wait-ForServicesReady -Ports @($GatewayPort, $FrontendPort) -TimeoutSeconds $TimeoutSeconds
    if (-not $ready) {
        Write-Host "[WARN] Ports not fully ready yet - opening browser anyway" -ForegroundColor Yellow
    }
    $urls = @("http://localhost:$FrontendPort", "http://localhost:$GatewayPort")
    foreach ($url in $urls) {
        try { Start-Process $url; Write-Host "  Opened $url" -ForegroundColor Gray }
        catch { Write-Host "  Could not open $url" -ForegroundColor Yellow }
    }
}

# ============================================================
# MAIN STARTUP
# ============================================================

function Start-PlatformExternal {
    Clear-Host
    Write-Host ""
    Write-Host "  =============================================================" -ForegroundColor Yellow
    Write-Host "  :                                                              :" -ForegroundColor Yellow
    Write-Host "  :      JIRA PLATFORM - EXTERNAL PostgreSQL MODE               :" -ForegroundColor Yellow
    Write-Host "  :      Database: in0-eplmdb-v01 / systems                     :" -ForegroundColor Yellow
    Write-Host "  :                                                              :" -ForegroundColor Yellow
    Write-Host "  =============================================================" -ForegroundColor Yellow
    Write-Host ""

    if (-not (Initialize-Runtime)) { return }
    if (-not (Test-ExternalDbConnection)) { return }

    $services = Get-Services
    $backends = $services | Where-Object { $_.type -eq "backend" }
    $gateway = $services | Where-Object { $_.name -eq "gateway" }
    $frontend = $services | Where-Object { $_.type -eq "frontend" }

    Write-Host ""
    Write-Host "Starting services (no local PostgreSQL needed)..." -ForegroundColor Yellow
    Write-Host ""

    Write-Host "--- WAVE 1: Starting backend services ---" -ForegroundColor Magenta
    $started = 0
    foreach ($service in $backends) {
        if (Start-ServiceFast -Service $service -ProjectRoot $ProjectRoot) { $started++ }
    }
    if ($started -lt $backends.Count) {
        Write-Host "[WARN] $($backends.Count - $started) backend(s) failed - run: mvn package -DskipTests" -ForegroundColor Yellow
    }

    Write-Host ""
    Write-Host "--- WAVE 2: Starting gateway ---" -ForegroundColor Magenta
    Start-Sleep -Seconds 3
    if ($gateway) { [void](Start-ServiceFast -Service $gateway -ProjectRoot $ProjectRoot) }

    Write-Host ""
    Write-Host "--- WAVE 3: Starting frontend ---" -ForegroundColor Magenta
    Start-Sleep -Seconds 3
    if ($frontend) { [void](Start-ServiceFast -Service $frontend -ProjectRoot $ProjectRoot) }

    $status = @{ started = (Get-Date).ToString("HH:mm:ss"); status = "running"; mode = "external-db" }
    Set-Content -Path $StatusFile -Value ($status | ConvertTo-Json) -NoNewline

    $gatewayPort = if ($gateway) { [int]$gateway.port } else { 8080 }
    $frontendPort = if ($frontend) { [int]$frontend.port } else { 3000 }

    Write-Host ""
    Write-Host "==============================================================" -ForegroundColor Green
    Write-Host "  All services started! (External DB mode)" -ForegroundColor Green
    Write-Host "  Database:  in0-eplmdb-v01:5432/systems" -ForegroundColor Green
    Write-Host "  Frontend:  http://localhost:$frontendPort" -ForegroundColor Green
    Write-Host "  Gateway:   http://localhost:$gatewayPort" -ForegroundColor Green
    Write-Host "  Logs:      platform-runtime\logs\" -ForegroundColor Gray
    Write-Host "==============================================================" -ForegroundColor Green
    Write-Host ""

    Open-PlatformBrowsers -GatewayPort $gatewayPort -FrontendPort $frontendPort
}

function Stop-PlatformFast {
    Write-Host ""
    Write-Host "Stopping all services..." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "[1/3] Stopping Java services..." -ForegroundColor Cyan
    Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue
    Write-Host "[2/3] Stopping Node.js..." -ForegroundColor Cyan
    Stop-Process -Name "node" -Force -ErrorAction SilentlyContinue
    Stop-Process -Name "npm" -Force -ErrorAction SilentlyContinue
    Write-Host "[3/3] Cleaning up..." -ForegroundColor Cyan
    if (Test-Path $PidDir) {
        Get-ChildItem -Path $PidDir -Filter "*.pid" | Remove-Item -Force
    }
    if (Test-Path $StatusFile) { Remove-Item $StatusFile -Force }
    Write-Host ""
    Write-Host "All services stopped." -ForegroundColor Green
}

function Show-StatusFast {
    $services = Get-Services
    Write-Host ""
    Write-Host "  =============================================================" -ForegroundColor Yellow
    Write-Host "  :          JIRA PLATFORM STATUS (External DB)               :" -ForegroundColor Yellow
    Write-Host "  =============================================================" -ForegroundColor Yellow
    Write-Host ""
    foreach ($service in $services) {
        $serviceProcessId = Get-ServicePid -ServiceName $service.name
        $running = Test-ProcessRunning -ProcessId $serviceProcessId
        $status = if ($running) { "RUNNING" } else { "STOPPED" }
        $color = if ($running) { "Green" } else { "Red" }
        Write-Host "  $($service.displayName.PadRight(25))" -NoNewline
        Write-Host $status.PadRight(10) -NoNewline -ForegroundColor $color
        Write-Host " Port: $($service.port)" -ForegroundColor Gray
    }
    Write-Host ""
}

# ============================================================
# ENTRY POINT
# ============================================================

if ($Stop) {
    Stop-PlatformFast
} elseif ($Status) {
    Show-StatusFast
} elseif ($Restart) {
    Stop-PlatformFast
    Start-Sleep -Seconds 2
    Start-PlatformExternal
} else {
    Start-PlatformExternal
}
