param(
    [string]$BaseUrl = "http://localhost:8094",
    [string]$WorkflowXml = "..\docs\soc\workflow\jira-dc-enterprise-change-workflow.xml",
    [string]$SchemeXml = "..\docs\soc\workflow\jira-dc-enterprise-workflow-scheme.xml",
    [switch]$StubDownstream = $true
)

$workflowPath = Join-Path $PSScriptRoot $WorkflowXml
$schemePath = Join-Path $PSScriptRoot $SchemeXml

Write-Host "POST $BaseUrl/api/migration/import/workflow-xml/validate"
$validateUri = "$BaseUrl/api/migration/import/workflow-xml/validate"
$validate = Invoke-RestMethod -Uri $validateUri -Method Post -Form @{
    file = Get-Item $workflowPath
    schemeFile = Get-Item $schemePath
}
$validate | ConvertTo-Json -Depth 6

if (-not $validate.valid) { exit 1 }

Write-Host "POST $BaseUrl/api/migration/import/workflow-xml"
$importUri = "$BaseUrl/api/migration/import/workflow-xml?stubDownstream=$StubDownstream"
$import = Invoke-RestMethod -Uri $importUri -Method Post -Form @{
    file = Get-Item $workflowPath
    schemeFile = Get-Item $schemePath
}
$import | ConvertTo-Json -Depth 4
