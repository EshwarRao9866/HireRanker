$loginBody = @{
    email = "gorai@hire.com"
    password = "candidate123"
} | ConvertTo-Json

$loginRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $loginRes.token
Write-Host "Candidate logged in successfully. Token obtained: $($token.Substring(0, 10))..."

$headers = @{
    Authorization = "Bearer $token"
}

$dash = Invoke-RestMethod -Uri "http://localhost:8080/api/candidates/me/dashboard" -Method Get -Headers $headers
$dash | ConvertTo-Json -Depth 5
