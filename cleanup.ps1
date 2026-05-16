# Kill Java processes using gateway JAR
Get-Process java -ErrorAction SilentlyContinue | Where-Object {$_.Path -like "*jira-gateway*"} | Stop-Process -Force -ErrorAction SilentlyContinue

# Wait and cleanup
Start-Sleep -Seconds 2

# Remove target directory
Remove-Item -Path "C:\Users\thech\OneDrive\Desktop\cloudetest\jira-platform\jira-gateway\target" -Force -Recurse -ErrorAction SilentlyContinue

Write-Host "Cleanup complete"