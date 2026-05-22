# E2E: Issue CRUD — create, read, update, delete with core fields
param(
    [string]$Gateway = "http://localhost:8080",
    [string]$Username = "admin",
    [string]$Password = "admin123",
    [string]$ProjectId = "",
    [string]$UserId = "",
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

Write-Host "=== Issue CRUD E2E ($Gateway) ===" -ForegroundColor Cyan

$h = @{ "Content-Type" = "application/json" }
if ($SkipAuth) {
    if (-not $UserId) { $UserId = "5ba38176-421f-431c-87f9-3836e4147a8c" }
    $h["X-User-Id"] = $UserId
} else {
    $login = Invoke-RestMethod -Uri "$Gateway/api/auth/login" -Method POST `
        -Body (@{ username = $Username; password = $Password } | ConvertTo-Json) -ContentType "application/json"
    $h["Authorization"] = "Bearer $($login.accessToken)"
    $h["X-User-Id"] = $login.userId
}

if (-not $ProjectId) {
    $projects = Invoke-RestMethod -Uri "$Gateway/api/projects?page=0&size=10" -Headers $h
    if ($projects -is [array]) { $ProjectId = $projects[0].id }
    elseif ($projects.content) { $ProjectId = $projects.content[0].id }
    else { $ProjectId = $projects.id }
}
Write-Host "Project: $ProjectId"

$types = Invoke-RestMethod -Uri "$Gateway/api/issues/types" -Headers $h
$prio = Invoke-RestMethod -Uri "$Gateway/api/issues/priorities" -Headers $h
$typeId = $types[0].id
$prioId = $prio[0].id

$title = "E2E-CRUD-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
$createBody = @{
    projectId         = $ProjectId
    title             = $title
    description       = "E2E create body"
    issueTypeId       = $typeId
    priorityId        = $prioId
    storyPoints       = 5
    originalEstimate  = 7200
    remainingEstimate = 3600
    labels            = @("e2e", "crud-test")
} | ConvertTo-Json

try {
    $created = Invoke-RestMethod -Uri "$Gateway/api/issues" -Method POST -Headers $h -Body $createBody
    Write-Host "PASS CREATE: $($created.issueKey) ($($created.id))" -ForegroundColor Green
} catch {
    Write-Host "FAIL CREATE: $(Read-ErrorBody $_)" -ForegroundColor Red
    exit 1
}

$id = $created.id

try {
    $fetched = Invoke-RestMethod -Uri "$Gateway/api/issues/$id" -Headers $h
    if ($fetched.title -ne $title) { throw "GET title mismatch" }
    Write-Host "PASS READ: $($fetched.issueKey)" -ForegroundColor Green
} catch {
    Write-Host "FAIL READ: $(Read-ErrorBody $_)" -ForegroundColor Red
    $fail++
}

$updateBody = @{
    title             = "$title-updated"
    description       = "E2E updated"
    storyPoints       = 8
    priorityId        = $prio[([Math]::Min(1, $prio.Count - 1))].id
    originalEstimate  = 10800
    remainingEstimate = 5400
} | ConvertTo-Json

try {
    $updated = Invoke-RestMethod -Uri "$Gateway/api/issues/$id" -Method PUT -Headers $h -Body $updateBody
    if ($updated.title -ne "$title-updated") { throw "UPDATE title mismatch" }
    if ($updated.storyPoints -ne 8) { throw "UPDATE storyPoints mismatch ($($updated.storyPoints))" }
    Write-Host "PASS UPDATE: $($updated.title) sp=$($updated.storyPoints)" -ForegroundColor Green
} catch {
    Write-Host "FAIL UPDATE: $(Read-ErrorBody $_)" -ForegroundColor Red
    $fail++
}

try {
    Invoke-RestMethod -Uri "$Gateway/api/issues/$id" -Method DELETE -Headers $h | Out-Null
    Write-Host "PASS DELETE" -ForegroundColor Green
} catch {
    Write-Host "FAIL DELETE: $(Read-ErrorBody $_)" -ForegroundColor Red
    $fail++
}

try {
    Invoke-RestMethod -Uri "$Gateway/api/issues/$id" -Headers $h | Out-Null
    Write-Host "FAIL: issue still exists after delete" -ForegroundColor Red
    $fail++
} catch {
    Write-Host "PASS: GET after delete returns error (expected)" -ForegroundColor Green
}

if ($fail -gt 0) { exit 1 }
Write-Host "`nAll issue CRUD checks passed" -ForegroundColor Green
