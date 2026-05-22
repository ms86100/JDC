# Jira Platform Fast Enterprise Runtime
# Optimized for parallel startup - no waiting between independent services

param(
    [switch]$Stop,
    [switch]$Status,
    [switch]$Restart
)

$ErrorActionPreference = "SilentlyContinue"
# Script lives in jira-platform/ — repo root is the script directory (not its parent).
$ProjectRoot = $PSScriptRoot
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
                # Start-Process: PowerShell cannot use Process.OutputDataReceived += (that is C# syntax).
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
            if (-not (Test-ServiceReady -Port $port)) {
                $allReady = $false
            }
        }
        if (-not $allReady) { Start-Sleep -Seconds 2 }
    }

    return $allReady
}

function Open-PlatformBrowsers {
    param(
        [int]$GatewayPort = 8080,
        [int]$FrontendPort = 3000,
        [int]$TimeoutSeconds = 90
    )

    Write-Host "Waiting for gateway (:$GatewayPort) and frontend (:$FrontendPort)..." -ForegroundColor Cyan
    $ready = Wait-ForServicesReady -Ports @($GatewayPort, $FrontendPort) -TimeoutSeconds $TimeoutSeconds
    if (-not $ready) {
        Write-Host "[WARN] Ports not fully ready yet - opening browser anyway" -ForegroundColor Yellow
    }

    $urls = @(
        "http://localhost:$FrontendPort",
        "http://localhost:$GatewayPort"
    )
    foreach ($url in $urls) {
        try {
            Start-Process $url
            Write-Host "  Opened $url" -ForegroundColor Gray
        } catch {
            Write-Host "  Could not open $url" -ForegroundColor Yellow
        }
    }
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

    # WAVE 1: Start all backend services
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
    Start-Sleep -Seconds 3  # Brief wait for auth service
    if ($gateway) {
        [void](Start-ServiceFast -Service $gateway -ProjectRoot $ProjectRoot)
    }

    Write-Host ""
    Write-Host "--- WAVE 3: Starting frontend ---" -ForegroundColor Magenta
    Start-Sleep -Seconds 3  # Brief wait for gateway
    if ($frontend) {
        [void](Start-ServiceFast -Service $frontend -ProjectRoot $ProjectRoot)
    }

    # Save status
    $status = @{ started = (Get-Date).ToString("HH:mm:ss"); status = "running" }
    Set-Content -Path $StatusFile -Value ($status | ConvertTo-Json) -NoNewline

    $gatewayPort = if ($gateway) { [int]$gateway.port } else { 8080 }
    $frontendPort = if ($frontend) { [int]$frontend.port } else { 3000 }

    Write-Host ""
    Write-Host "==============================================================" -ForegroundColor Green
    Write-Host "  All services started!" -ForegroundColor Green
    Write-Host "  Frontend: http://localhost:$frontendPort" -ForegroundColor Green
    Write-Host "  Gateway:  http://localhost:$gatewayPort" -ForegroundColor Green
    Write-Host "  Logs: platform-runtime\logs\" -ForegroundColor Gray
    Write-Host "==============================================================" -ForegroundColor Green
    Write-Host ""

    Open-PlatformBrowsers -GatewayPort $gatewayPort -FrontendPort $frontendPort
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
    Start-PlatformFast
} else {
    Start-PlatformFast
}