# Jira Platform Fast Launcher
# Sequential startup with health verification

$ErrorActionPreference = "SilentlyContinue"
$ProjectRoot = "C:\Users\thech\OneDrive\Desktop\cloudetest\jira-platform"
$LogDir = "$ProjectRoot\platform-runtime\logs"
$PidDir = "$ProjectRoot\platform-runtime\pids"
$DB_PASSWORD = "UNIpay@123"

# Create directories
@($LogDir, $PidDir) | ForEach-Object { if (-not (Test-Path $_)) { New-Item -ItemType Directory -Path $_ -Force | Out-Null } }

# Find Java
$javaExe = if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    "$env:JAVA_HOME\bin\java.exe"
} else { "java.exe" }

Write-Host "Java: $javaExe" -ForegroundColor Cyan

# Clean up
Write-Host "Stopping existing services..." -ForegroundColor Yellow
Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue
Stop-Process -Name "node" -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 3

# Service definitions with proper DB password
$services = @(
    @{ name = "auth-service"; jar = "$ProjectRoot\jira-auth-service\target\jira-auth-service-1.0.0.jar"; port = 8081; deps = @() },
    @{ name = "user-service"; jar = "$ProjectRoot\jira-user-service\target\jira-user-service-1.0.0.jar"; port = 8082; deps = @("auth-service") },
    @{ name = "project-service"; jar = "$ProjectRoot\jira-project-service\target\jira-project-service-1.0.0.jar"; port = 8083; deps = @("auth-service") },
    @{ name = "workflow-service"; jar = "$ProjectRoot\jira-workflow-service\target\jira-workflow-service-1.0.0.jar"; port = 8085; deps = @("auth-service") },
    @{ name = "issue-service"; jar = "$ProjectRoot\jira-issue-service\target\jira-issue-service-1.0.0.jar"; port = 8084; deps = @("auth-service", "project-service", "workflow-service") },
    @{ name = "comment-service"; jar = "$ProjectRoot\jira-comment-service\target\jira-comment-service-1.0.0.jar"; port = 8086; deps = @("auth-service") },
    @{ name = "notification-service"; jar = "$ProjectRoot\jira-notification-service\target\jira-notification-service-1.0.0.jar"; port = 8087; deps = @("auth-service") },
    @{ name = "search-service"; jar = "$ProjectRoot\jira-search-service\target\jira-search-service-1.0.0.jar"; port = 8088; deps = @("auth-service") },
    @{ name = "audit-service"; jar = "$ProjectRoot\jira-audit-service\target\jira-audit-service-1.0.0.jar"; port = 8089; deps = @("auth-service") },
    @{ name = "attachment-service"; jar = "$ProjectRoot\jira-attachment-service\target\jira-attachment-service-1.0.0.jar"; port = 8090; deps = @("auth-service") },
    @{ name = "sprint-service"; jar = "$ProjectRoot\jira-sprint-service\target\jira-sprint-service-1.0.0.jar"; port = 8091; deps = @("auth-service") },
    @{ name = "plan-service"; jar = "$ProjectRoot\jira-plan-service\target\jira-plan-service-1.0.0.jar"; port = 8092; deps = @("auth-service") },
    @{ name = "admin-service"; jar = "$ProjectRoot\jira-admin-service\target\jira-admin-service-1.0.0.jar"; port = 8093; deps = @("auth-service") }
)

function Start-ServiceWithRetry {
    param($svc, $javaExe, $projectRoot, $logDir, $pidDir, $dbPassword)

    $logFile = "$logDir\$($svc.name).log"
    if (Test-Path $logFile) { Remove-Item $logFile -Force }

    if (-not (Test-Path $svc.jar)) {
        Write-Host "  $($svc.name): JAR NOT FOUND" -ForegroundColor Red
        return $false
    }

    Write-Host "  $($svc.name)..." -NoNewline

    # All services use jira_platform database with different schemas

    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $javaExe
    $psi.Arguments = "-Xms256m -Xmx256m -jar `"$($svc.jar)`" --server.port=$($svc.port)"
    $psi.WorkingDirectory = $projectRoot
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.CreateNoWindow = $true

    # Environment - CRITICAL: Use correct DB password
    $psi.EnvironmentVariables["SPRING_PROFILES_ACTIVE"] = "local"
    $psi.EnvironmentVariables["DB_HOST"] = "localhost"
    $psi.EnvironmentVariables["DB_PORT"] = "5432"
    $psi.EnvironmentVariables["DB_NAME"] = "jira_platform"
    $psi.EnvironmentVariables["DB_USERNAME"] = "jiraadmin"
    $psi.EnvironmentVariables["DB_PASSWORD"] = $dbPassword

    $proc = [System.Diagnostics.Process]::Start($psi)

    $proc.OutputDataReceived += {
        if ($EventArgs.Data) {
            Add-Content -Path $logFile -Value "[$((Get-Date).ToString('HH:mm:ss'))] $($EventArgs.Data)" -Encoding UTF8
        }
    }
    $proc.ErrorDataReceived += {
        if ($EventArgs.Data) {
            Add-Content -Path $logFile -Value "[$((Get-Date).ToString('HH:mm:ss'))] ERROR: $($EventArgs.Data)" -Encoding UTF8
        }
    }
    $proc.BeginOutputReadLine()
    $proc.BeginErrorReadLine()

    # Wait for health check with retries
    $maxRetries = 10
    $healthy = $false

    for ($i = 0; $i -lt $maxRetries; $i++) {
        Start-Sleep -Seconds 3
        try {
            $r = Invoke-WebRequest -Uri "http://localhost:$($svc.port)/actuator/health" -TimeoutSec 3 -UseBasicParsing
            if ($r.StatusCode -eq 200) {
                $healthy = $true
                break
            }
        } catch {}
        Write-Host "." -NoNewline
    }

    if ($healthy) {
        Set-Content -Path "$pidDir\$($svc.name).pid" -Value $proc.Id -NoNewline
        Write-Host " OK (PID: $($proc.Id))" -ForegroundColor Green
        return $true
    } else {
        Write-Host " FAILED" -ForegroundColor Red
        # Try to show error from log
        if (Test-Path $logFile) {
            $errors = Get-Content $logFile | Select-String "ERROR|Exception|error|failed" | Select-Object -First 3
            if ($errors) {
                Write-Host "    Possible errors:" -ForegroundColor Yellow
                foreach ($e in $errors) {
                    Write-Host "    $($e.Line)" -ForegroundColor Yellow
                }
            }
        }
        return $false
    }
}

# Start services sequentially
Write-Host ""
Write-Host "Starting backend services sequentially..." -ForegroundColor Green

$startedCount = 0
foreach ($svc in $services) {
    $success = Start-ServiceWithRetry -svc $svc -javaExe $javaExe -projectRoot $ProjectRoot -logDir $LogDir -pidDir $PidDir -dbPassword $DB_PASSWORD
    if ($success) { $startedCount++ }
}

# Start gateway
Write-Host ""
Write-Host "Starting gateway..." -ForegroundColor Green

$gatewayLog = "$LogDir\gateway.log"
if (Test-Path $gatewayLog) { Remove-Item $gatewayLog -Force }

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $javaExe
$psi.Arguments = "-Xms512m -Xmx512m -jar `"$ProjectRoot\jira-gateway\target\jira-gateway-1.0.0.jar`" --server.port=8080"
$psi.WorkingDirectory = $ProjectRoot
$psi.UseShellExecute = $false
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.CreateNoWindow = $true
$psi.EnvironmentVariables["SPRING_PROFILES_ACTIVE"] = "local"
$psi.EnvironmentVariables["AUTH_SERVICE_URL"] = "http://localhost:8081"

$gateway = [System.Diagnostics.Process]::Start($psi)
$gateway.OutputDataReceived += { if ($EventArgs.Data) { Add-Content -Path $gatewayLog -Value "[$((Get-Date).ToString('HH:mm:ss'))] $($EventArgs.Data)" -Encoding UTF8 } }
$gateway.ErrorDataReceived += { if ($EventArgs.Data) { Add-Content -Path $gatewayLog -Value "[$((Get-Date).ToString('HH:mm:ss'))] ERROR: $($EventArgs.Data)" -Encoding UTF8 } }
$gateway.BeginOutputReadLine()
$gateway.BeginErrorReadLine()

Write-Host "  gateway..." -NoNewline
for ($i = 0; $i -lt 10; $i++) {
    Start-Sleep -Seconds 3
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -TimeoutSec 3 -UseBasicParsing
        if ($r.StatusCode -eq 200) {
            Write-Host " OK" -ForegroundColor Green
            break
        }
    } catch {}
    Write-Host "." -NoNewline
}

# Start frontend
Write-Host ""
Write-Host "Starting frontend..." -ForegroundColor Green

$frontendLog = "$LogDir\frontend.log"
$cmdScript = "cd /d `"$ProjectRoot\jira-frontend`" && npm run dev"
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = "cmd.exe"
$psi.Arguments = "/c start `"Jira Frontend`" cmd /c `"$cmdScript`""
$psi.UseShellExecute = $false

$frontend = [System.Diagnostics.Process]::Start($psi)
Write-Host "  frontend starting in new window..." -ForegroundColor Gray

# Wait for all services
Write-Host ""
Write-Host "Final health check..." -ForegroundColor Cyan
Start-Sleep -Seconds 5

# Open browser
Write-Host "Opening browser..." -ForegroundColor Green
Start-Process "http://localhost:8080"

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Green
Write-Host "  Started: $startedCount backend services + gateway + frontend" -ForegroundColor Green
Write-Host "  Gateway: http://localhost:8080" -ForegroundColor White
Write-Host "  Frontend: http://localhost:3000" -ForegroundColor White
Write-Host "  Logs: platform-runtime\logs\" -ForegroundColor Gray
Write-Host "==============================================================" -ForegroundColor Green