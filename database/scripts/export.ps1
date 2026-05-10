# Load .env.local from project root if it exists

Get-Content ".\.env.local" | ForEach-Object {
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
$user    = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }
$pass    = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "password" }
$db      = if ($env:DB_NAME) { $env:DB_NAME } else { "facturas_monrachem" }
$env:MYSQL_PWD = $pass

$exportDir = Join-Path $PSScriptRoot "..\export"
if (-not (Test-Path $exportDir)) { New-Item -ItemType Directory -Path $exportDir | Out-Null }

$dumpArgs = @("--no-create-info", "--complete-insert", "--skip-extended-insert", "--skip-triggers")

$baseTables = @(
    "cat_provincia_cr", "cat_canton_cr", "cat_distrito_cr",
    "parametro_sistema", "permiso", "rol", "rol_permiso",
    "plantilla_notificacion", "plantilla_whatsapp", "presentacion"
)

Write-Host "Exporting base tables..."
foreach ($table in $baseTables) {
    $out = Join-Path $exportDir "$table.sql"
    & mysqldump -h $host_db -u $user @dumpArgs $db $table | Set-Content $out -Encoding UTF8
    Write-Host "  $table -> $out"
}

$expConfig = Read-Host "Export configuracion tables? (configuracion_email, configuracion_empresa, configuracion_facturacion, configuracion_hacienda, configuracion_notificaciones) [y/N]"
if ($expConfig -match '^[Yy]') {
    foreach ($t in @("configuracion_email", "configuracion_empresa", "configuracion_facturacion", "configuracion_hacienda", "configuracion_notificaciones")) {
        $out = Join-Path $exportDir "$t.sql"
        & mysqldump -h $host_db -u $user @dumpArgs $db $t | Set-Content $out -Encoding UTF8
        Write-Host "  $t -> $out"
    }
}

$expEmpresa = Read-Host "Export empresa table? [y/N]"
if ($expEmpresa -match '^[Yy]') {
    $out = Join-Path $exportDir "empresa.sql"
    & mysqldump -h $host_db -u $user @dumpArgs $db empresa | Set-Content $out -Encoding UTF8
    Write-Host "  empresa -> $out"
}

$expProducto = Read-Host "Export producto table? [y/N]"
if ($expProducto -match '^[Yy]') {
    $out = Join-Path $exportDir "producto.sql"
    & mysqldump -h $host_db -u $user @dumpArgs $db producto | Set-Content $out -Encoding UTF8
    Write-Host "  producto -> $out"
}

Write-Host "Export complete. Files in: $exportDir"
