$loginPayload = '{"identifier":"letan","password":"MatKhauAdmin@2026"}'
$loginRes = Invoke-RestMethod -Uri 'http://localhost:4000/api/v1/auth/staff/login' -Method Post -Body $loginPayload -ContentType 'application/json'
Write-Host "Login response JSON:" ($loginRes | ConvertTo-Json -Depth 5)

$token = $loginRes.data.token
Write-Host "Real token:" $token.Substring(0, 20)
try {
    $accRes = Invoke-RestMethod -Uri 'http://localhost:4000/api/v1/staff/account' -Method Get -Headers @{ Authorization = "Bearer $token" }
    Write-Host "Account response:" ($accRes | ConvertTo-Json -Depth 5)
} catch {
    Write-Host "Account error:" $_.Exception.ToString()
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Response Body:" $reader.ReadToEnd()
    }
}
