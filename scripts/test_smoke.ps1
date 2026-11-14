$ErrorActionPreference = 'Stop'
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

function Call-Api {
    param (
        [string]$Method,
        [string]$Uri,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )
    $params = @{
        Method      = $Method
        Uri         = $Uri
        Headers     = $Headers
        WebSession  = $session
        ContentType = 'application/json; charset=utf-8'
    }
    if ($Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 10)
    }
    try {
        $res = Invoke-RestMethod @params
        return @{ Success = $true; Data = $res }
    } catch {
        $errText = $_.ToString()
        $status = 0
        if ($_.Exception.Response) {
            $status = $_.Exception.Response.StatusCode.value__
            $stream = $_.Exception.Response.GetResponseStream()
            if ($stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $errText = $reader.ReadToEnd()
            }
        }
        return @{ Success = $false; StatusCode = $status; Error = $errText; Exception = $_.Exception.Message }
    }
}

Write-Host "================ 1. HEALTH CHECK ================"
$health = Call-Api -Method 'GET' -Uri 'http://localhost:4000/api/v1/health'
Write-Host "Status: 200, OK: $($health.Data.data.ok), Message: $($health.Data.message)"

Write-Host "`n================ 2. META CHECK ================"
$meta = Call-Api -Method 'GET' -Uri 'http://localhost:4000/api/v1/meta'
Write-Host "Service types: $($meta.Data.data.serviceTypes.Count)"
Write-Host "Time slots: $($meta.Data.data.slots.Count)"
Write-Host "Post categories: $($meta.Data.data.postCategories.Count)"
Write-Host "First post category format: value=$($meta.Data.data.postCategories[0].value), label=$($meta.Data.data.postCategories[0].label)"

Write-Host "`n================ 3. STAFF LOGIN (letan) ================"
$staffLogin = Call-Api -Method 'POST' -Uri 'http://localhost:4000/api/v1/auth/staff/login' -Body @{
    identifier = 'letan'
    password = 'MatKhauAdmin@2026'
}
if (-not $staffLogin.Success) {
    Write-Host "Staff Login Failed: $($staffLogin.Error)"
    exit 1
}
$staffToken = $staffLogin.Data.data.token
Write-Host "Staff Login Success! User: $($staffLogin.Data.data.user.fullName), Role: $($staffLogin.Data.data.user.role)"

Write-Host "`n================ 4. STAFF ACCOUNT (GET /api/v1/staff/account) ================"
$staffAccount = Call-Api -Method 'GET' -Uri 'http://localhost:4000/api/v1/staff/account' -Headers @{
    Authorization = "Bearer $staffToken"
}
Write-Host "Staff Account Success: $($staffAccount.Success)"
if ($staffAccount.Success) {
    Write-Host "Staff Account info: $($staffAccount.Data.data | ConvertTo-Json -Compress)"
} else {
    Write-Host "Staff Account Error: $($staffAccount.Error)"
}

Write-Host "`n================ 5. STAFF QUEUE (GET /api/v1/staff/queue) ================"
$staffQueue = Call-Api -Method 'GET' -Uri 'http://localhost:4000/api/v1/staff/queue' -Headers @{
    Authorization = "Bearer $staffToken"
}
Write-Host "Staff Queue Success: $($staffQueue.Success), items count: $($staffQueue.Data.data.items.Count)"

Write-Host "`n================ 6. REFRESH TOKEN VIA COOKIE (GET /api/v1/auth/refresh) ================"
$refreshGet = Call-Api -Method 'GET' -Uri "http://localhost:4000/api/v1/auth/refresh"
Write-Host "Refresh GET (via cookie ct_refresh) Success: $($refreshGet.Success)"
if ($refreshGet.Success) {
    Write-Host "New refreshed token: $($refreshGet.Data.data.token.Substring(0, 20))..."
}

Write-Host "`n================ 7. PATIENT OTP FLOW ================"
$testPhone = "0988" + (Get-Random -Minimum 100000 -Maximum 999999).ToString()
Write-Host "Requesting OTP for phone: $testPhone"
$otpReq = Call-Api -Method 'POST' -Uri 'http://localhost:4000/api/v1/auth/login' -Body @{
    phone = $testPhone
}
Write-Host "OTP Request Success: $($otpReq.Success)"
$devCode = $otpReq.Data.data.devCode
Write-Host "Generated devCode: $devCode"

Write-Host "`n================ 8. PATIENT OTP VERIFY (/login/verify) ================"
$otpVerify = Call-Api -Method 'POST' -Uri 'http://localhost:4000/api/v1/auth/login/verify' -Body @{
    phone = $testPhone
    code  = $devCode
}
Write-Host "OTP Verify Success: $($otpVerify.Success)"
if ($otpVerify.Success) {
    $patientToken = $otpVerify.Data.data.token
    Write-Host "Patient Token: $($patientToken.Substring(0, 20))..."

    Write-Host "`n================ 9. PATIENT /auth/me ================"
    $me = Call-Api -Method 'GET' -Uri 'http://localhost:4000/api/v1/auth/me' -Headers @{
        Authorization = "Bearer $patientToken"
    }
    Write-Host "Auth /me Success: $($me.Success), User: $($me.Data.data.fullName), Phone: $($me.Data.data.phone)"
}

Write-Host "`n================ 10. LEGACY ROUTE 404 VERIFICATION ================"
foreach ($path in @('/api/meta', '/api/posts', '/posts', '/uploads', '/api/uploads')) {
    $legacy = Call-Api -Method 'GET' -Uri "http://localhost:4000$path"
    Write-Host "$path -> Status: $($legacy.StatusCode) (Expected 404)"
}

