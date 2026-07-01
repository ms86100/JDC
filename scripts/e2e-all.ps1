# Run core E2E smoke scripts (requires gateway 8080 + services)
param([string]$Gateway = "http://localhost:8080")

$scripts = @(
    "e2e-transitions-and-actions.ps1",
    "e2e-rbac.ps1"
)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$fail = 0
foreach ($s in $scripts) {
    $path = Join-Path $root $s
    if (-not (Test-Path $path)) {
        Write-Host "SKIP missing $s" -ForegroundColor Yellow
        continue
    }
    Write-Host "`n=== $s ===" -ForegroundColor Cyan
    & $path -Gateway $Gateway
    if ($LASTEXITCODE -ne 0) { $fail++ }
}
if ($fail -gt 0) { exit 1 }
Write-Host "`nAll E2E scripts passed" -ForegroundColor Green
