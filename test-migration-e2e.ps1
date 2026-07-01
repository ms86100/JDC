$ErrorActionPreference = 'Stop'
$base = 'http://localhost:3000'
$login = Invoke-RestMethod -Uri "$base/api/auth/login" -Method POST -Body (@{username='ms86100';password='admin123'}|ConvertTo-Json) -ContentType 'application/json'
$userId = '8b4583a5-3ac0-4e6d-a202-a9fdc5f7af9e'
$token = $login.accessToken

function Post-Multipart($url, $filePath, [hashtable]$extraFields = @{}) {
  $boundary = [guid]::NewGuid().ToString()
  $fileBytes = [IO.File]::ReadAllBytes($filePath)
  $fileName = [IO.Path]::GetFileName($filePath)
  $enc = [Text.Encoding]::UTF8
  $sb = New-Object System.Text.StringBuilder
  foreach ($k in $extraFields.Keys) {
    [void]$sb.Append("--$boundary`r`nContent-Disposition: form-data; name=`"$k`"`r`n`r`n$($extraFields[$k])`r`n")
  }
  $header = $sb.ToString()
  $footer = "`r`n--$boundary--`r`n"
  $partHeader = "$header--$boundary`r`nContent-Disposition: form-data; name=`"file`"; filename=`"$fileName`"`r`nContent-Type: application/octet-stream`r`n`r`n"
  $partHeaderBytes = $enc.GetBytes($partHeader)
  $footerBytes = $enc.GetBytes($footer)
  $body = New-Object byte[] ($partHeaderBytes.Length + $fileBytes.Length + $footerBytes.Length)
  [Buffer]::BlockCopy($partHeaderBytes, 0, $body, 0, $partHeaderBytes.Length)
  [Buffer]::BlockCopy($fileBytes, 0, $body, $partHeaderBytes.Length, $fileBytes.Length)
  [Buffer]::BlockCopy($footerBytes, 0, $body, $partHeaderBytes.Length + $fileBytes.Length, $footerBytes.Length)
  $req = [Net.HttpWebRequest]::Create($url)
  $req.Method = 'POST'
  $req.ContentType = "multipart/form-data; boundary=$boundary"
  $req.Headers.Add('Authorization', "Bearer $token")
  $req.Headers.Add('X-User-Id', $userId)
  $req.Headers.Add('X-Migration-Role', 'MIGRATION_ADMIN')
  $req.ContentLength = $body.Length
  $stream = $req.GetRequestStream()
  $stream.Write($body, 0, $body.Length)
  $stream.Close()
  $resp = $req.GetResponse()
  $reader = New-Object IO.StreamReader($resp.GetResponseStream())
  $json = $reader.ReadToEnd() | ConvertFrom-Json
  $resp.Close()
  return $json
}

function Wait-JobStatus($jobId, $label) {
  $deadline = (Get-Date).AddMinutes(3)
  $h = @{ Authorization = "Bearer $token"; 'X-User-Id' = $userId; 'X-Migration-Role' = 'MIGRATION_ADMIN' }
  while ((Get-Date) -lt $deadline) {
    $job = Invoke-RestMethod -Uri "$base/api/migration/jobs/$jobId" -Headers $h
    Write-Host "  $label status=$($job.jobStatus) progress=$($job.progressPercentage)%"
    if ($job.jobStatus -in @('COMPLETED','FAILED','CANCELLED')) { return $job }
    Start-Sleep -Seconds 3
  }
  throw "Timeout job $jobId"
}

$h = @{ Authorization = "Bearer $token"; 'X-User-Id' = $userId }
$tProj = Invoke-RestMethod -Uri "$base/api/migration/templates?entityType=PROJECT" -Headers $h
$tIssue = Invoke-RestMethod -Uri "$base/api/migration/templates?entityType=ISSUE" -Headers $h
Write-Host "Templates PROJECT=$($tProj.Count) ISSUE=$($tIssue.Count)"

$projKey = 'MIG' + (Get-Date -Format 'HHmmss')
Write-Host "Using project key $projKey"
$projFile = Join-Path $env:TEMP 'mig-project.csv'
$content = "project_key,name,description`n$projKey,Migration Project One,E2E project import`n"
[System.IO.File]::WriteAllText($projFile, $content, [Text.UTF8Encoding]::new($false))
$projJob = Post-Multipart "$base/api/migration/import/csv" $projFile
Write-Host "Project CSV job=$($projJob.id)"
$pr = Wait-JobStatus $projJob.id 'Project CSV'
if ($pr.jobStatus -ne 'COMPLETED') { throw "Project CSV failed: $($pr.errorMessage)" }

$projects = Invoke-RestMethod -Uri "$base/api/projects" -Headers @{ Authorization = "Bearer $token" }
$target = $projects | Where-Object { $_.projectKey -eq $projKey } | Select-Object -First 1
if (-not $target) { throw "$projKey not found" }
Write-Host "Target project $($target.id)"

$issueFile = Join-Path $env:TEMP 'mig-issues.csv'
$issueContent = "project_key,issue_type,summary,status`n$projKey,Task,Migration test issue one,To Do`n$projKey,Bug,Migration test issue two,To Do`n"
[System.IO.File]::WriteAllText($issueFile, $issueContent, [Text.UTF8Encoding]::new($false))
$issueJob = Post-Multipart "$base/api/migration/import/csv" $issueFile @{ targetProjectId = $target.id }
Write-Host "Issue CSV job=$($issueJob.id)"
$ir = Wait-JobStatus $issueJob.id 'Issue CSV'
if ($ir.jobStatus -ne 'COMPLETED') { throw "Issue CSV failed: $($ir.errorMessage)" }

$xmlPath = Join-Path $PSScriptRoot 'jira-migration-service\src\test\resources\samples\jira_dc_issue_export.xml'
$val = Post-Multipart "$base/api/migration/import/jira-dc/validate" $xmlPath
Write-Host "XML validate valid=$($val.valid)"
$xmlJob = Post-Multipart "$base/api/migration/import/jira-dc" $xmlPath @{ targetProjectId = $target.id }
Write-Host "XML job=$($xmlJob.id)"
$xr = Wait-JobStatus $xmlJob.id 'XML DC'
if ($xr.jobStatus -ne 'COMPLETED') { throw "XML failed: $($xr.errorMessage)" }
Write-Host 'E2E migration tests PASSED'
