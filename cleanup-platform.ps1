# Jira Platform — safe cleanup of generated / ephemeral files only.
# Does NOT remove source, config, docs, .cursor, consolidated-migration, or .git
param(
    [switch]$IncludeMavenTarget,
    [switch]$IncludeNodeModules,
    [switch]$WhatIf
)

$ErrorActionPreference = "SilentlyContinue"
$Root = $PSScriptRoot

function Remove-SafeItem {
    param([string]$Path, [string]$Label)
    if (-not (Test-Path $Path)) { return }
    if ($WhatIf) {
        Write-Host "[WhatIf] Would remove: $Label -> $Path" -ForegroundColor Yellow
        return
    }
    Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "Removed: $Label" -ForegroundColor Green
}

function Clear-LogFiles {
    param([string]$Dir)
    if (-not (Test-Path $Dir)) { return }
    Get-ChildItem -LiteralPath $Dir -File -Filter "*.log" -ErrorAction SilentlyContinue | ForEach-Object {
        if ($WhatIf) { Write-Host "[WhatIf] $($_.FullName)" -ForegroundColor Yellow }
        else { Remove-Item -LiteralPath $_.FullName -Force }
    }
}

Write-Host "Jira Platform cleanup ($Root)" -ForegroundColor Cyan

# Runtime logs (launcher) — stop launcher.py first if files are locked
$logsDir = Join-Path $Root "logs"
if (Test-Path $logsDir) {
    Get-ChildItem -LiteralPath $logsDir -Recurse -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Extension -eq ".log" } | ForEach-Object {
            if ($WhatIf) { Write-Host "[WhatIf] $($_.FullName)" -ForegroundColor Yellow }
            else {
                try { Remove-Item -LiteralPath $_.FullName -Force }
                catch { Write-Host "Locked (stop platform first): $($_.Name)" -ForegroundColor Yellow }
            }
        }
    Get-ChildItem -LiteralPath $logsDir -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notin @(".git") } | ForEach-Object {
            Remove-SafeItem $_.FullName "logs/$($_.Name)"
        }
}
if (-not $WhatIf) { New-Item -ItemType File -Path (Join-Path $Root "logs\.gitkeep") -Force | Out-Null }

# Python cache
Remove-SafeItem (Join-Path $Root "__pycache__") "__pycache__"

# JVM crash dumps
Get-ChildItem -Path $Root -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match '^(hs_err_pid|replay_pid).*\.log$' -and $_.FullName -notmatch '\\\.git\\' } |
    ForEach-Object {
        if ($WhatIf) { Write-Host "[WhatIf] $($_.FullName)" -ForegroundColor Yellow }
        else { Remove-Item -LiteralPath $_.FullName -Force }
    }

# Local temp / debug at repo root
@("plan-error.txt", "plan-output.txt") | ForEach-Object {
    Remove-SafeItem (Join-Path $Root $_) $_
}

# One-off shell tests (regenerable; listed in .gitignore)
Get-ChildItem -LiteralPath $Root -File -Filter "test-*.sh" | ForEach-Object {
    Remove-SafeItem $_.FullName $_.Name
}

# Frontend ephemeral
Remove-SafeItem (Join-Path $Root "jira-frontend\backup") "jira-frontend/backup"
Remove-SafeItem (Join-Path $Root "jira-frontend\test-results") "jira-frontend/test-results"
Remove-SafeItem (Join-Path $Root "jira-frontend\dist") "jira-frontend/dist"

# Alternate runtime logs/pids
Clear-LogFiles (Join-Path $Root "platform-runtime\logs")
Get-ChildItem -LiteralPath (Join-Path $Root "platform-runtime\pids") -File -ErrorAction SilentlyContinue |
    ForEach-Object {
        if ($WhatIf) { Write-Host "[WhatIf] pid $($_.Name)" -ForegroundColor Yellow }
        else { Remove-Item -LiteralPath $_.FullName -Force }
    }

# Service-local stray logs
@(
    "jira-plan-service\plan-service.log"
) | ForEach-Object { Remove-SafeItem (Join-Path $Root $_) $_ }

# Move root audit snapshots into docs/archive (preserve, declutter root)
$archive = Join-Path $Root "docs\archive"
if (-not $WhatIf) { New-Item -ItemType Directory -Path $archive -Force | Out-Null }
@(
    "API_VALIDATION_REPORT.md",
    "ENTERPRISE_API_AUDIT_REPORT.md",
    "GAP_ANALYSIS_UPDATED.md",
    "GAP_FILL_STATUS_REPORT.md",
    "PLAN_MANAGEMENT_REQUIREMENTS.md"
) | ForEach-Object {
    $src = Join-Path $Root $_
    if (Test-Path $src) {
        $dst = Join-Path $archive $_
        if ($WhatIf) { Write-Host "[WhatIf] Move $_ -> docs/archive/" -ForegroundColor Yellow }
        else { Move-Item -LiteralPath $src -Destination $dst -Force }
    }
}

# Dev tool: keep but relocate
$validator = Join-Path $Root "api-test-validator.js"
$scriptsDir = Join-Path $Root "scripts"
if ((Test-Path $validator) -and -not (Test-Path (Join-Path $scriptsDir "api-test-validator.js"))) {
    if ($WhatIf) { Write-Host "[WhatIf] Move api-test-validator.js -> scripts/" -ForegroundColor Yellow }
    else {
        New-Item -ItemType Directory -Path $scriptsDir -Force | Out-Null
        Move-Item -LiteralPath $validator -Destination (Join-Path $scriptsDir "api-test-validator.js") -Force
    }
}

if ($IncludeMavenTarget) {
    Get-ChildItem -LiteralPath $Root -Directory -Filter "jira-*" | ForEach-Object {
        Remove-SafeItem (Join-Path $_.FullName "target") "$($_.Name)/target"
    }
}

if ($IncludeNodeModules) {
    Remove-SafeItem (Join-Path $Root "jira-frontend\node_modules") "jira-frontend/node_modules"
}

Write-Host "Done. Rebuild: python launcher.py  |  Frontend: cd jira-frontend && npm ci" -ForegroundColor Cyan
