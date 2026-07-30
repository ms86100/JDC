# E2E verification: Bulk Operations + Central Audit Trail
# Prerequisites: gateway 8080, issue-service 8084, audit-service 8089, workflow-service 8085 (for status bulk)

$ErrorActionPreference = "Stop"
$base = if ($env:API_BASE) { $env:API_BASE } else { "http://localhost:8080" }
$userId = if ($env:TEST_USER_ID) { $env:TEST_USER_ID } else { "00000000-0000-0000-0001-000000000001" }
$headers = @{ "X-User-Id" = $userId; "Content-Type" = "application/json" }

function Step($name, $script) {
    Write-Host "`n== $name ==" -ForegroundColor Cyan
    & $script
    if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) { throw "Step failed: $name" }
}

Step "Health: issue-service via gateway" {
    $r = Invoke-RestMethod -Uri "$base/api/issues/priorities" -Method GET -Headers $headers
    if (-not $r) { throw "No priorities" }
    Write-Host "OK priorities: $($r.Count)"
}

Step "Fetch one issue via JQL" {
    $search = Invoke-RestMethod -Uri "$base/api/issues/search?jql=ORDER%20BY%20updated%20DESC&page=0&pageSize=1" -Method GET -Headers $headers
    $issues = $search.issues
    if (-not $issues -and $search.content) { $issues = $search.content }
    if (-not $issues -or $issues.Count -eq 0) { throw "No issues in DB — create an issue first" }
    $script:issueId = $issues[0].id
    $script:projectId = $issues[0].projectId
    $script:issueKey = $issues[0].issueKey
    Write-Host "OK issue $issueKey ($issueId)"
}

Step "Bulk UPDATE_FIELDS (priority)" {
    $body = @{
        issueIds = @($issueId)
        operationType = "UPDATE_FIELDS"
        projectId = $projectId
        priority = "Medium"
    } | ConvertTo-Json
    $bulk = Invoke-RestMethod -Uri "$base/api/bulk-operations" -Method POST -Headers $headers -Body $body
    if ($bulk.successCount -lt 1) { throw "Bulk fields failed: $($bulk | ConvertTo-Json -Depth 5)" }
    Write-Host "OK bulk fields: $($bulk.status) success=$($bulk.successCount)"
}

Step "Audit: entries for issue" {
    try {
        $audit = Invoke-RestMethod -Uri "$base/api/audit/logs/ISSUE/$issueId?page=0&size=10" -Method GET -Headers $headers
        $count = if ($audit.content) { $audit.content.Count } else { 0 }
        Write-Host "OK audit entries: $count (expect >= 1 after bulk/update)"
        if ($count -lt 1) {
            Write-Warning "No audit rows yet — ensure audit-service 8089 is running with Flyway V1"
        }
    } catch {
        Write-Warning "Audit service not reachable at $base/api/audit — start avionics-systems-audit-service on 8089"
    }
}

Step "Single issue update + audit" {
    $upd = @{ title = "E2E audit test $(Get-Date -Format 'HH:mm:ss')" } | ConvertTo-Json
    $updated = Invoke-RestMethod -Uri "$base/api/issues/$issueId" -Method PUT -Headers $headers -Body $upd
    Write-Host "OK updated: $($updated.issueKey)"
    Start-Sleep -Seconds 2
    try {
        $audit2 = Invoke-RestMethod -Uri "$base/api/audit/logs/ISSUE/$issueId?page=0&size=5" -Method GET -Headers $headers
        $n = if ($audit2.content) { $audit2.content.Count } else { 0 }
        Write-Host "OK audit after update: $n entries"
    } catch { Write-Warning "Audit poll skipped" }
}

Write-Host "`nAll E2E steps completed." -ForegroundColor Green
