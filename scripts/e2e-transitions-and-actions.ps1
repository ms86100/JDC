# E2E: workflow transitions + issue More-menu actions (link, clone, move)
param(
    [string]$Gateway = "http://localhost:8080",
    [string]$UserId = "00000000-0000-0000-0000-000000000001",
    [string]$ProjectId = "",
    [string]$IssueId = ""
)

$headers = @{ "X-User-Id" = $UserId; "Content-Type" = "application/json" }

function Get-Json($url) {
    Invoke-RestMethod -Uri $url -Headers $headers -Method Get
}

function Post-Json($url, $body) {
    Invoke-RestMethod -Uri $url -Headers $headers -Method Post -Body ($body | ConvertTo-Json -Depth 6)
}

Write-Host "=== Transitions & actions E2E ===" -ForegroundColor Cyan

if (-not $ProjectId) {
    $projects = Get-Json "$Gateway/api/projects/all"
    if (-not $projects -or $projects.Count -eq 0) { throw "No projects. Start project-service and create a project." }
    $ProjectId = $projects[0].id
}
Write-Host "Project: $ProjectId"

if (-not $IssueId) {
    $search = Get-Json "$Gateway/api/issues/search?projectId=$ProjectId&page=0&pageSize=1"
    $issues = $search.issues
    if (-not $issues -or $issues.Count -eq 0) {
        $created = Post-Json "$Gateway/api/issues" @{
            projectId = $ProjectId
            title = "E2E transition test"
            issueTypeId = $null
        }
        $IssueId = $created.id
    } else {
        $IssueId = $issues[0].id
    }
}
Write-Host "Issue: $IssueId"

$trans = Get-Json "$Gateway/api/issues/$IssueId/transitions?projectId=$ProjectId"
$count = @($trans.transitions).Count
Write-Host "Available transitions: $count"
if ($count -eq 0) { throw "FAIL: No transitions returned" }

$target = $trans.transitions[0]
$patch = Invoke-RestMethod -Uri "$Gateway/api/issues/$IssueId/status?projectId=$ProjectId" -Headers $headers -Method Patch -Body (@{
    transitionId = $target.id
    statusId = $target.toStatusId
} | ConvertTo-Json) -ContentType "application/json"
Write-Host "Transition OK -> status $($patch.status)"

$cloned = Post-Json "$Gateway/api/issues/$IssueId/clone" @{}
Write-Host "Clone OK -> $($cloned.issueKey)"

$linkTarget = $cloned.issueKey
Post-Json "$Gateway/api/issues/$IssueId/links" @{ targetIssueKey = $linkTarget; linkType = "relates_to" } | Out-Null
Write-Host "Link OK -> $linkTarget"

if ($projects.Count -gt 1) {
    $moveTo = $projects[1].id
    $moved = Post-Json "$Gateway/api/issues/$($cloned.id)/move" @{ projectId = $moveTo }
    Write-Host "Move OK -> project $($moved.projectId)"
}

Write-Host "PASS" -ForegroundColor Green
