$ErrorActionPreference = 'Stop'

$mysql = 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe'
$dbUser = 'root'
$dbPass = 'tritrongtutien'

function Run-Sql {
    param([string]$Sql)
    & $mysql -u $dbUser "--password=$dbPass" --default-character-set=utf8mb4 -e $Sql
}

function Run-File {
    param([string]$Db, [string]$File)
    Write-Host "  -> Dang chay: $File tren $Db"
    $raw = Get-Content $File -Raw -Encoding UTF8
    $raw = $raw.Replace('phongkham_chuctu', $Db)
    $tmp = [System.IO.Path]::GetTempFileName()
    [System.IO.File]::WriteAllText($tmp, $raw, [System.Text.Encoding]::UTF8)
    $tmpFixed = $tmp.Replace('\', '/')
    & $mysql -u $dbUser "--password=$dbPass" --default-character-set=utf8mb4 -e "USE $Db; source $tmpFixed;"
    Remove-Item $tmp -Force -ErrorAction SilentlyContinue
}

$targets = @('phongkham_chuctu', 'phongkham_chuctu_test')

foreach ($db in $targets) {
    Write-Host "=== RESET DATABASE $db ==="
    Run-Sql "DROP DATABASE IF EXISTS $db; CREATE DATABASE $db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    
    Run-File $db 'database\001_schema_v2.sql'
    Run-File $db 'database\002_add_patient_fields.sql'
    Run-File $db 'database\003_contract_alignment.sql'
    Run-File $db 'database\004_seed_slots_60min.sql'
    Run-File $db 'database\005_v1_support.sql'
    Run-File $db 'docker\mysql\init\03-seed-users.sql'
    Run-File $db 'database\seed_articles.sql'
    Run-File $db 'database\006_fix_mojibake_data.sql'
    
    Write-Host "Kiem tra so luong bang tren ${db}:"
    Run-Sql "SELECT COUNT(*) AS total_tables FROM information_schema.TABLES WHERE TABLE_SCHEMA = '$db';"
    Run-Sql "SELECT id, username, full_name, role FROM ${db}.users;"
    Run-Sql "SELECT COUNT(*) AS active_60min_slots FROM ${db}.time_slots WHERE is_active = 1;"
}

Write-Host "=== HOAN TAT RESET DATABASE ==="
