Get-Content "..\..\.env.local" | ForEach-Object {
    if ($_ -and !$_.StartsWith("#")) {
        $parts = $_ -split "=", 2
        if ($parts.Length -eq 2) {
            $name = $parts[0].Trim()
            $value = $parts[1].Trim()
            
            [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
            
            if ($name -like "*TOKEN*" -or $name -like "*PASSWORD*") {
                Write-Host "  ✅ $name = ********" -ForegroundColor Green
            } else {
                Write-Host "  ✅ $name = $value" -ForegroundColor Green
            }
        }
    }
}

$host_db = if ($env:DB_HOST) { $env:DB_HOST } else { "192.168.100.93" }
$user    = if ($env:DB_USER) { $env:DB_USER } else { "root" }
$pass    = if ($env:DB_PASS) { $env:DB_PASS } else { "password" }
$db      = if ($env:DB_NAME) { $env:DB_NAME } else { "facturas_monrachem" }

$ErrorActionPreference = "Stop"
$seedDir     = Join-Path $PSScriptRoot "..\seeds"
$routinesDir = Join-Path $PSScriptRoot "..\routines"

Get-ChildItem "$seedDir\*.sql" | Sort-Object Name | ForEach-Object {
    Write-Host "Running $($_.Name)..."
    $env:MYSQL_PWD = $pass
    Get-Content -Raw $_.FullName | mysql -h $host_db -u $user $db
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed on $($_.Name)"
        exit 1
    }
    Write-Host "  OK"
}
Write-Host "Seeding complete."

if (Test-Path "$routinesDir\*.sql") {
    Get-ChildItem "$routinesDir\*.sql" | Sort-Object Name | ForEach-Object {
        Write-Host "Applying routine $($_.Name)..."
        $env:MYSQL_PWD = $pass
        Get-Content -Raw $_.FullName | mysql -h $host_db -u $user $db
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed on $($_.Name)"
            exit 1
        }
        Write-Host "  OK"
    }
}
