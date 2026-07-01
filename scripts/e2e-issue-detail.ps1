# E2E: Issue detail — comment, label, worklog, link, update fields
param(
    [string]$Gateway = "http://localhost:8080",
    [string]$IssueService = "http://localhost:8084",
    [string]$CommentService = "http://localhost:8086",
    [string]$Username = "admin",
    [string]$Password = "admin123",
    [string]$IssueId = "",
    [string]$UserId = "5ba38176-421f-431c-87f9-3836e4147a8c",
    [switch]$SkipAuth
)

$ErrorActionPreference = "Stop"
$fail = 0

function Read-ErrorBody($ex) {
    if ($ex.ErrorDetails.Message) { return $ex.ErrorDetails.Message }
    if ($ex.Exception.Response) {
        $reader = [System.IO.StreamReader]::new($ex.Exception.Response.GetResponseStream())
        return $reader.ReadToEnd()
    }
    return $ex.Exception.Message
}

function Test-Step($name, [scriptblock]$block) {
    try {
        & $block
        Write-Host "PASS $name" -ForegroundColor Green
    } catch {
        Write-Host "FAIL $name : $(Read-ErrorBody $_)" -ForegroundColor Red
        $script:fail++
    }
}

Write-Host "=== Issue Detail E2E ===" -ForegroundColor Cyan

$h = @{ "Content-Type" = "application/json" }
if ($SkipAuth) {
    $h["X-User-Id"] = $UserId
} else {
    $login = Invoke-RestMethod -Uri "$Gateway/api/auth/login" -Method POST `
        -Body (@{ username = $Username; password = $Password } | ConvertTo-Json) -ContentType "application/json"
    $h["Authorization"] = "Bearer $($login.accessToken)"
    $h["X-User-Id"] = $login.userId
}

if (-not $IssueId) {
    $issues = Invoke-RestMethod -Uri "$Gateway/api/issues?page=0&size=1" -Headers $h
    if ($issues.content) { $IssueId = $issues.content[0].id }
    elseif ($issues -is [array]) { $IssueId = $issues[0].id }
}
Write-Host "Issue: $IssueId"

Test-Step "UPDATE fields" {
    $body = @{
        title       = "E2E detail update $(Get-Date -Format 'HHmmss')"
        description = "updated desc"
        environment = "test env"
        storyPoints = 4
        remainingEstimate = 1980
        timeSpent = 26640
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$Gateway/api/issues/$IssueId" -Method PUT -Headers $h -Body $body | Out-Null
}

Test-Step "LABEL add" {
    $body = @{ name = "e2e-label-$(Get-Random)" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$IssueService/api/issues/$IssueId/labels" -Method POST -Headers $h -Body $body | Out-Null
}

Test-Step "WORKLOG add" {
    $body = @{ timeSpentSeconds = 300; workDescription = "e2e work" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$IssueService/api/issues/$IssueId/worklogs" -Method POST -Headers $h -Body $body | Out-Null
}

Test-Step "LINK create" {
    $issue = Invoke-RestMethod -Uri "$IssueService/api/issues/$IssueId" -Headers $h
    $others = Invoke-RestMethod -Uri "$IssueService/api/issues?projectId=$($issue.projectId)&page=0&size=5" -Headers $h
    $target = $null
    if ($others.content) {
        foreach ($o in $others.content) { if ($o.id -ne $IssueId) { $target = $o; break } }
    }
    if (-not $target) {
        $types = Invoke-RestMethod -Uri "$IssueService/api/issues/types" -Headers $h
        $prio = Invoke-RestMethod -Uri "$IssueService/api/issues/priorities" -Headers $h
        $create = @{
            projectId = $issue.projectId
            title = "E2E link target"
            issueTypeId = $types[0].id
            priorityId = $prio[0].id
        } | ConvertTo-Json
        $target = Invoke-RestMethod -Uri "$IssueService/api/issues" -Method POST -Headers $h -Body $create
    }
    $linkBody = @{ targetIssueId = $target.id; linkTypeName = "blocks" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$IssueService/api/issues/$IssueId/links" -Method POST -Headers $h -Body $linkBody | Out-Null
}

Test-Step "COMMENT add (direct)" {
    $body = @{ issueId = $IssueId; content = "e2e comment $(Get-Date -Format 'HHmmss')" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$CommentService/api/comments" -Method POST -Headers $h -Body $body | Out-Null
}

if (-not $SkipAuth) {
    Test-Step "COMMENT add (gateway)" {
        $body = @{ issueId = $IssueId; content = "e2e gw comment" } | ConvertTo-Json
        Invoke-RestMethod -Uri "$Gateway/api/comments" -Method POST -Headers $h -Body $body | Out-Null
    }
}

Test-Step "GET labels" {
    Invoke-RestMethod -Uri "$IssueService/api/issues/$IssueId/labels" -Headers $h | Out-Null
}

Test-Step "GET worklogs" {
    Invoke-RestMethod -Uri "$IssueService/api/issues/$IssueId/worklogs" -Headers $h | Out-Null
}

Test-Step "GET links outward" {
    Invoke-RestMethod -Uri "$IssueService/api/issues/$IssueId/links/outward" -Headers $h | Out-Null
}

Write-Host ""
if ($fail -gt 0) {
    Write-Host "FAILED: $fail step(s)" -ForegroundColor Red
    exit 1
}
Write-Host "ALL PASSED" -ForegroundColor Green
