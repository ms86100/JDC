# E2E: production RBAC — mutating calls without permission or without user fail closed
param(
    [string]$Gateway = "http://localhost:8080",
    [string]$UserId = "00000000-0000-0000-0000-000000000001",
    [string]$ProjectId = "",
    [string]$DeniedUserId = "00000000-0000-0000-0000-000000009999"
)

function Invoke-IssueApi($Method, $Url, $Body, $Headers) {
    try {
        if ($Body) {
            return Invoke-WebRequest -Uri $Url -Method $Method -Headers $Headers -Body ($Body | ConvertTo-Json) -ContentType "application/json" -UseBasicParsing
        }
        return Invoke-WebRequest -Uri $Url -Method $Method -Headers $Headers -UseBasicParsing
    } catch {
        return $_.Exception.Response
    }
}

$headers = @{ "X-User-Id" = $UserId; "Content-Type" = "application/json" }
$deniedHeaders = @{ "X-User-Id" = $DeniedUserId; "Content-Type" = "application/json" }

if (-not $ProjectId) {
    $projects = Invoke-RestMethod -Uri "$Gateway/api/projects/all" -Headers $headers
    $ProjectId = $projects[0].id
}

$search = Invoke-RestMethod -Uri "$Gateway/api/issues/search?projectId=$ProjectId&page=0&pageSize=1" -Headers $headers
$issueId = $search.issues[0].id

Write-Host "=== RBAC E2E (issue $issueId) ===" -ForegroundColor Cyan

# No X-User-Id on update → 403
$noUser = Invoke-IssueApi PUT "$Gateway/api/issues/$issueId" @{ title = "RBAC test" } @{ "Content-Type" = "application/json" }
if ($noUser.StatusCode -ne 403) {
    throw "Expected 403 without X-User-Id, got $($noUser.StatusCode)"
}
Write-Host "PASS: update without user → 403"

# Denied user update → 403 (when project-service enforces permissions)
$denied = Invoke-IssueApi PUT "$Gateway/api/issues/$issueId" @{ title = "Should fail" } $deniedHeaders
if ($denied.StatusCode -notin 403, 503) {
    throw "Expected 403/503 for denied user, got $($denied.StatusCode)"
}
Write-Host "PASS: denied user update → $($denied.StatusCode)"

Write-Host "RBAC checks complete (ensure fail-open=false in prod profile)" -ForegroundColor Green
