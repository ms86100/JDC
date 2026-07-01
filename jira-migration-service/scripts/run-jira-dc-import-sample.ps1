# Run Jira DC sample import (Comment + Attachment with base64 file field)
param(
    [string]$BaseUrl = "http://localhost:8094",
    [string]$SampleXml = "$PSScriptRoot\..\src\test\resources\samples\jira-dc-minimal-comment-attachment.xml",
    [switch]$StubDownstream,
    [string]$UserId = "00000000-0000-0000-0000-000000000001"
)

if (-not (Test-Path $SampleXml)) {
    Write-Error "Sample file not found: $SampleXml"
    exit 1
}

$options = @{ rollbackOnFailure = $false }
if ($StubDownstream) {
    $options.stubDownstream = $true
}

$boundary = [guid]::NewGuid().ToString()
$xmlBytes = [System.IO.File]::ReadAllBytes($SampleXml)
$optionsJson = ($options | ConvertTo-Json -Compress)

$bodyLines = @(
    "--$boundary",
    'Content-Disposition: form-data; name="file"; filename="jira-dc-minimal-comment-attachment.xml"',
    'Content-Type: application/xml',
    '',
    [System.Text.Encoding]::UTF8.GetString($xmlBytes),
    "--$boundary",
    "Content-Disposition: form-data; name=`"options`"",
    '',
    $optionsJson,
    "--$boundary--"
) -join "`r`n"

$headers = @{
    "X-User-Id" = $UserId
    "X-Migration-Role" = "MIGRATION_OPERATOR"
    "Content-Type" = "multipart/form-data; boundary=$boundary"
}

Write-Host "POST $BaseUrl/api/migration/import/jira-dc"
Write-Host "Sample: $SampleXml"
Write-Host "Options: $optionsJson"

try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/api/migration/import/jira-dc" -Method Post -Headers $headers -Body $bodyLines
    $jobId = $response.id
    Write-Host "Job accepted: $jobId"

    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 2
        $job = Invoke-RestMethod -Uri "$BaseUrl/api/migration/jobs/$jobId" -Headers @{ "X-User-Id" = $UserId; "X-Migration-Role" = "MIGRATION_OPERATOR" }
        Write-Host "  status=$($job.jobStatus) processed=$($job.processedEntities) failed=$($job.failedEntities)"
        if ($job.jobStatus -in @("COMPLETED", "FAILED", "ROLLED_BACK", "CANCELLED")) {
            $result = Invoke-RestMethod -Uri "$BaseUrl/api/migration/jobs/$jobId/result" -Headers @{ "X-User-Id" = $UserId; "X-Migration-Role" = "MIGRATION_OPERATOR" }
            Write-Host "Result metadata:" ($result.resultMetadata | ConvertTo-Json -Compress)
            exit $(if ($job.jobStatus -eq "COMPLETED") { 0 } else { 2 })
        }
    }
    Write-Warning "Timed out waiting for job completion"
    exit 3
}
catch {
    Write-Error $_
    Write-Host "`nTip: Start migration-service on 8094, or validate parse only with: mvn test -Dtest=JiraDcXmlParserTest"
    exit 1
}
