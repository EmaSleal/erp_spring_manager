# Converts mysqldump exports from database/export/ into idempotent seed files in database/seeds/
# Run once after export.ps1 to populate seeds 04-10.

$exportDir = Join-Path $PSScriptRoot "..\export"
$seedsDir  = Join-Path $PSScriptRoot "..\seeds"

$tables = @(
    @{ Num = "04"; Table = "parametro_sistema" },
    @{ Num = "05"; Table = "permiso" },
    @{ Num = "06"; Table = "rol" },
    @{ Num = "07"; Table = "rol_permiso" },
    @{ Num = "08"; Table = "plantilla_notificacion" },
    @{ Num = "09"; Table = "plantilla_whatsapp" },
    @{ Num = "10"; Table = "presentacion" }
)

foreach ($entry in $tables) {
    $src  = Join-Path $exportDir "$($entry.Table).sql"
    $dest = Join-Path $seedsDir  "$($entry.Num)_$($entry.Table).sql"

    if (-not (Test-Path $src)) {
        Write-Warning "  SKIP $($entry.Table).sql — not found in export/"
        continue
    }

    $content = Get-Content $src -Raw -Encoding UTF8

    # Extract only the INSERT lines (everything between LOCK TABLES and UNLOCK TABLES)
    $insertBlock = ""
    if ($content -match '(?s)(LOCK TABLES.*?UNLOCK TABLES;)') {
        $insertBlock = $Matches[1]
        # Remove LOCK/UNLOCK wrappers, keep only INSERT lines
        $insertBlock = $insertBlock -replace 'LOCK TABLES `[^`]+` WRITE;\r?\n', ''
        $insertBlock = $insertBlock -replace '/\*!40000 ALTER TABLE `[^`]+` DISABLE KEYS \*/;\r?\n', ''
        $insertBlock = $insertBlock -replace '/\*!40000 ALTER TABLE `[^`]+` ENABLE KEYS \*/;\r?\n', ''
        $insertBlock = $insertBlock -replace 'UNLOCK TABLES;\r?\n?', ''
    }

    # Convert INSERT INTO -> INSERT IGNORE INTO
    $insertBlock = $insertBlock -replace 'INSERT INTO', 'INSERT IGNORE INTO'

    $header = @"
-- ============================================================
-- Seed: $($entry.Table)
-- Idempotency: INSERT IGNORE
-- ============================================================
SET NAMES utf8mb4;
USE facturas_monrachem;

"@

    $final = $header + $insertBlock.Trim() + "`n"
    [System.IO.File]::WriteAllText($dest, $final, [System.Text.Encoding]::UTF8)
    Write-Host "  OK  $($entry.Num)_$($entry.Table).sql"
}

Write-Host "Done. Seeds ready in: $seedsDir"
