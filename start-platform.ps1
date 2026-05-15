# Jira Platform Fast Enterprise Runtime
# Optimized for parallel startup - no waiting between independent services

param(
    [switch]$Stop,
    [switch]$Status,
    [switch]$Restart
)

$ErrorActionPreference = "SilentlyContinue"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$RuntimeDir = Join-Path $ProjectRoot "platform-runtime"
$LogDir = Join-Path $RuntimeDir "logs"
$PidDir = Join-Path $RuntimeDir "pids"
$ConfigPath = Join-Path $ProjectRoot "config\services.json"
$StatusFile = Join-Path $RuntimeDir ".runtime-status"

# ============================================================
# UTILITY FUNCTIONS
# ============================================================

function Initialize-Runtime {
    $dirs = @($LogDir, $PidDir, (Join-Path $RuntimeDir "postgres\data"), (Join-Path $RuntimeDir "attachments"))
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

function Get-Services {
    $config = Get-Content $ConfigPath -Raw | ConvertFrom-Json
    return $config.services
}

function Save-ServicePid {
    param([string]$ServiceName, [int]$Pid)
    $pidFile = Join-Path $PidDir "$ServiceName.pid"
    Set-Content -Path $pidFile -Value $Pid -NoNewline
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
    param([int]$Pid)
    if ($Pid -eq 0 -or $Pid -eq $null) { return $false }
    try { $process = Get-Process -Id $Pid -ErrorAction SilentlyContinue; return $null -ne $process } catch { return $false }
}

function Write-Log {
    param([string]$Level = "INFO", [string]$Service = "Runtime", [string]$Message)
    $timestamp = Get-Date -Format "HH:mm:ss"
    $logFile = Join-Path $LogDir "platform-runtime.log"
    $logEntry = "[$timestamp] [$Level] [$Service] $Message`n"
    Add-Content -Path $logFile -Value $logEntry -Encoding UTF8
}

# ============================================================
# FAST SERVICE STARTUP
# ============================================================

function Find-JavaPath {
    $javaHome = $env:JAVA_HOME
    if ($javaHome -and (Test-Path "$javaHome\bin\java.exe")) { return "$javaHome\bin\java.exe" }

    $commonPaths = @(
        "C:\Program Files\Java\jdk-21\bin\java.exe",
        "C:\Program Files\Java\jdk-17\bin\java.exe",
        "C:\Program Files\Java\jdk-11\bin\java.exe"
    )
    foreach ($path in $commonPaths) {
        if (Test-Path $path) { return $path }
    }
    return "java.exe"
}

function Find-JarPath {
    param([object]$Service, [string]$ProjectRoot)
    $serviceNameShort = $Service.name -replace "-service", ""
    $jarDir = Join-Path $ProjectRoot "jira-$serviceNameShort-service\target"
    if (-not (Test-Path $jarDir)) { return $null }

    $jars = Get-ChildItem -Path $jarDir -Filter "*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch "original" }
    if ($jars) { return $jars[0].FullName }
    return $null
}

function Start-ServiceFast {
    param([object]$Service, [string]$ProjectRoot)

    $existingPid = Get-ServicePid -ServiceName $Service.name
    if ($existingPid -and (Test-ProcessRunning -Pid $existingPid)) {
        Write-Host "[SKIP] $($Service.displayName) already running" -ForegroundColor Gray
        return $true
    }

    $logFile = Join-Path $LogDir "$($Service.name).log"

    try {
        if ($Service.type -eq "backend" -or $Service.type -eq "gateway") {
            $javaPath = Find-JavaPath
            $jarPath = Find-JarPath -Service $Service -ProjectRoot $ProjectRoot

            if (-not $jarPath) {
                Write-Host "[ERROR] JAR not found for $($Service.name)" -ForegroundColor Red
                return $false
            }

            $heapSize = if ($Service.memory) { $Service.memory } else { "256m" }

            $args = @("-Xms$heapSize", "-Xmx$heapSize", "-jar", $jarPath, "--server.port=$($Service.port)")

            $envVars = @{}
            if ($Service.environment) {
                foreach ($key in $Service.environment.PSObject.Properties.Name) {
                    $envVars[$key] = [string]$Service.environment.$key
                }
            }

            $psi = New-Object System.Diagnostics.ProcessStartInfo
            $psi.FileName = $javaPath
            $psi.Arguments = $args -join " "
            $psi.WorkingDirectory = $ProjectRoot
            $psi.UseShellExecute = $false
            $psi.RedirectStandardOutput = $true
            $psi.RedirectStandardError = $true
            $psi.CreateNoWindow = $true

            foreach ($key in $envVars.Keys) { $psi.EnvironmentVariables[$key] = $envVars[$key] }

            $process = [System.Diagnostics.Process]::Start($psi)
            $process.OutputDataReceived += { if ($EventArgs.Data) { Add-Content -Path $logFile -Value "[$((Get-Date).ToString('HH:mm:ss'))] $($EventArgs.Data)" -Encoding UTF8 } }
            $process.ErrorDataReceived += { if ($EventArgs.Data) { Add-Content -Path $logFile -Value "[$((Get-Date).ToString('HH:mm:ss'))] ERROR: $($EventArgs.Data)" -Encoding UTF8 } }
            $process.BeginOutputReadLine()
            $process.BeginErrorReadLine()

            Save-ServicePid -ServiceName $Service.name -Pid $process.Id
            Write-Host "[START] $($Service.displayName) (PID: $($process.Id))" -ForegroundColor Green
            return $true

        } elseif ($Service.type -eq "frontend") {
            $frontendDir = Join-Path $ProjectRoot "jira-frontend"
            if (-not (Test-Path $frontendDir)) {
                Write-Host "[ERROR] Frontend directory not found" -ForegroundColor Red
                return $false
            }

            $psi = New-Object System.Diagnostics.ProcessStartInfo
            $psi.FileName = "npm.cmd"
            $psi.Arguments = "run dev"
            $psi.WorkingDirectory = $frontendDir
            $psi.UseShellExecute = $false
            $psi.RedirectStandardOutput = $true
            $psi.RedirectStandardError = $true
            $psi.CreateNoWindow = $true
            $psi.EnvironmentVariables["FORCE_COLOR"] = "1"

            if ($Service.environment) {
                foreach ($key in $Service.environment.PSObject.Properties.Name) {
                    $psi.EnvironmentVariables[$key] = [string]$Service.environment.$key
                }
            }

            $process = [System.Diagnostics.Process]::Start($psi)
            $process.OutputDataReceived += { if ($EventArgs.Data) { Add-Content -Path $logFile -Value "[$((Get-Date).ToString('HH:mm:ss'))] $($EventArgs.Data)" -Encoding UTF8 } }
            $process.ErrorDataReceived += { if ($EventArgs.Data) { Add-Content -Path $logFile -Value "[$((Get-Date).ToString('HH:mm:ss'))] ERROR: $($EventArgs.Data)" -Encoding UTF8 } }
            $process.BeginOutputReadLine()
            $process.BeginErrorReadLine()

            Save-ServicePid -ServiceName $Service.name -Pid $process.Id
            Write-Host "[START] $($Service.displayName) (PID: $($process.Id))" -ForegroundColor Green
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
            if (-not (Test-ServiceReady -Port $port)) {
                $allReady = $false
            }
        }
        if (-not $allReady) { Start-Sleep -Seconds 2 }
    }

    return $allReady
}

# ============================================================
# MAIN STARTUP - PARALLEL FAST START
# ============================================================

function Start-PlatformFast {
    Clear-Host
    Write-Host ""
    Write-Host "  =============================================================" -ForegroundColor Cyan
    Write-Host "  :                                                              :" -ForegroundColor Cyan
    Write-Host "  :           JIRA PLATFORM - FAST STARTUP                      :" -ForegroundColor Cyan
    Write-Host "  :                                                              :" -ForegroundColor Cyan
    Write-Host "  =============================================================" -ForegroundColor Cyan
    Write-Host ""

    if (-not (Initialize-Runtime)) { return }

    $services = Get-Services

    # FAST STRATEGY: Start services in waves by priority
    # Wave 1: Backend services (all at once, they handle their own DB connections)
    # Wave 2: Gateway (after backends started)
    # Wave 3: Frontend (after gateway)

    $backends = $services | Where-Object { $_.type -eq "backend" }
    $gateway = $services | Where-Object { $_.name -eq "gateway" }
    $frontend = $services | Where-Object { $_.type -eq "frontend" }

    Write-Host "Starting services in parallel..." -ForegroundColor Yellow
    Write-Host ""

    # WAVE 1: Start all backends simultaneously
    Write-Host "--- WAVE 1: Starting backend services ---" -ForegroundColor Magenta
    $jobs = @()
    foreach ($service in $backends) {
        $jobs += Start-Job -ScriptBlock {
            param($svc, $projRoot)
            & $PSScriptRoot\start-platform.ps1 -StartService $svc.name
        } -ArgumentList $service, $ProjectRoot
    }

    # Start backends one by one quickly (parallel is complex in batch)
    foreach ($service in $backends) {
        Start-ServiceFast -Service $service -ProjectRoot $ProjectRoot
    }

    Write-Host ""
    Write-Host "--- WAVE 2: Starting gateway ---" -ForegroundColor Magenta
    Start-Sleep -Seconds 3  # Brief wait for auth service
    if ($gateway) {
        Start-ServiceFast -Service $gateway -ProjectRoot $ProjectRoot
    }

    Write-Host ""
    Write-Host "--- WAVE 3: Starting frontend ---" -ForegroundColor Magenta
    Start-Sleep -Seconds 3  # Brief wait for gateway
    if ($frontend) {
        Start-ServiceFast -Service $frontend -ProjectRoot $ProjectRoot
    }

    # Save status
    $status = @{ started = (Get-Date).ToString("HH:mm:ss"); status = "running" }
    Set-Content -Path $StatusFile -Value ($status | ConvertTo-Json) -NoNewline

    Write-Host ""
    Write-Host "==============================================================" -ForegroundColor Green
    Write-Host "  All services started!" -ForegroundColor Green
    Write-Host "  Access: http://localhost:8080" -ForegroundColor Green
    Write-Host "  Logs: platform-runtime\logs\" -ForegroundColor Gray
    Write-Host "==============================================================" -ForegroundColor Green
    Write-Host ""

    # Open browser
    Start-Sleep -Seconds 3
    try { Start-Process "http://localhost:8080" } catch {}
}

function Stop-PlatformFast {
    Write-Host ""
    Write-Host "Stopping all services..." -ForegroundColor Yellow
    Write-Host ""

    # Kill all Java processes
    Write-Host "[1/3] Stopping Java services..." -ForegroundColor Cyan
    Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue

    # Kill Node processes
    Write-Host "[2/3] Stopping Node.js..." -ForegroundColor Cyan
    Stop-Process -Name "node" -Force -ErrorAction SilentlyContinue
    Stop-Process -Name "npm" -Force -ErrorAction SilentlyContinue

    # Clean up PID files
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
    Write-Host "  =============================================================" -ForegroundColor Cyan
    Write-Host "  :                    JIRA PLATFORM STATUS                    :" -ForegroundColor Cyan
    Write-Host "  =============================================================" -ForegroundColor Cyan
    Write-Host ""

    foreach ($service in $services) {
        $pid = Get-ServicePid -ServiceName $service.name
        $running = Test-ProcessRunning -Pid $pid
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
    Start-PlatformFast
} else {
    Start-PlatformFast
}