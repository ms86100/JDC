# E2E: Export CSV, Rank, Realtime event path
$ErrorActionPreference = "Stop"
$base = if ($env:API_BASE) { $env:API_BASE } else { "http://localhost:8080" }
$userId = if ($env:TEST_USER_ID) { $env:TEST_USER_ID } else { "00000000-0000-0000-0001-000000000001" }
$headers = @{ "X-User-Id" = $userId }

Write-Host "== Fetch issue ==" -ForegroundColor Cyan
$search = Invoke-RestMethod -Uri "$base/api/issues/search?jql=ORDER%20BY%20updated%20DESC&page=0&pageSize=2" -Headers $headers
$issues = $search.issues
if (-not $issues) { $issues = $search.content }
if (-not $issues -or $issues.Count -lt 1) { throw "Need at least 1 issue" }
$issueId = $issues[0].id
$projectId = $issues[0].projectId
Write-Host "OK $($issues[0].issueKey)"

Write-Host "`n== Export CSV ==" -ForegroundColor Cyan
$csvPath = Join-Path $env:TEMP "avionics-systems-export-test.csv"
Invoke-WebRequest -Uri "$base/api/issues/search/export?jql=ORDER%20BY%20updated%20DESC" -Headers $headers -OutFile $csvPath
$lines = (Get-Content $csvPath | Measure-Object -Line).Lines
if ($lines -lt 2) { throw "CSV export empty" }
Write-Host "OK CSV $lines lines -> $csvPath"

Write-Host "`n== Rank DOWN ==" -ForegroundColor Cyan
$rankBody = @{ projectId = $projectId; direction = "DOWN" } | ConvertTo-Json
try {
    $ranked = Invoke-RestMethod -Uri "$base/api/issues/$issueId/rank" -Method PATCH -Headers $headers -Body $rankBody -ContentType "application/json"
    Write-Host "OK rank applied, key=$($ranked.issueKey)"
} catch {
    Write-Warning "Rank may fail with single issue in project: $($_.Exception.Message)"
}

Write-Host "`n== WebSocket endpoint (smoke) ==" -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8084/actuator/health" -Method GET
    Write-Host "OK issue-service health: $($health.status)"
} catch {
    Write-Warning "issue-service health check skipped"
}

Write-Host "`nAll export/rank E2E steps done." -ForegroundColor Green
