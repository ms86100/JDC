# Sync Flyway schema history when a migration file was edited after it was already applied (dev only).
Set-Location $PSScriptRoot
Write-Host "Running Flyway repair for avionics_systems_workflow schema..."
mvn -q flyway:repair
if ($LASTEXITCODE -eq 0) {
    Write-Host "Repair complete. You can run: mvn spring-boot:run"
} else {
    Write-Host "Repair failed. Check PostgreSQL is running and credentials in pom.xml match application.yml."
    exit $LASTEXITCODE
}
