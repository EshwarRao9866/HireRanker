$loginBody = @{ email = 'admin@hireranker.com'; password = 'Admin@123' } | ConvertTo-Json
$login = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body $loginBody
$headers = @{ Authorization = 'Bearer ' + $login.token }

Write-Host "=== APPLICATIONS IN MYSQL ==="
$apps = Invoke-RestMethod -Uri 'http://localhost:8080/api/applications' -Method Get -Headers $headers
$apps | Select-Object id, candidateId, candidateName, candidateEmail, jobTitle, status | Format-Table -AutoSize

Write-Host "=== DASHBOARD STATS ==="
$dash = Invoke-RestMethod -Uri 'http://localhost:8080/api/admin/dashboard' -Method Get -Headers $headers
Write-Host "Total Applications:" $dash.totalApplications
Write-Host "Screened Resumes:" $dash.screenedResumes
Write-Host "Shortlisted Candidates:" $dash.shortlistedCandidates
Write-Host "Total Interviews:" $dash.totalInterviews
Write-Host "Status Breakdown:"
$dash.applicationStatus | Format-Table -AutoSize
