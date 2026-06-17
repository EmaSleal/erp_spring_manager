-- ============================================================
-- Seed: cuenta_bancaria
-- Cuentas bancarias de la empresa para mostrar en facturas PDF.
-- Idempotency: INSERT IGNORE
-- ============================================================
SET NAMES utf8mb4;
USE facturas_monrachem;

-- Ejemplo de cuentas (ajustar según la empresa real)
INSERT IGNORE INTO `cuenta_bancaria`
    (`id_cuenta_bancaria`, `empresa_id`, `entidad`, `cuenta_iban`, `cuenta_banco`, `moneda`, `activa`, `orden`, `create_date`, `update_date`, `create_by`, `update_by`)
VALUES
    (1, 1, 'Banco Nacional - Colones',  'CR74015104210010004988', '100-01-042-000498-5', 'CRC', 1, 1, NOW(), NOW(), 1, 1),
    (2, 1, 'Banco Nacional - Dólares',  'CR76015104210026001319', '100-02-042-600131-2', 'USD', 1, 2, NOW(), NOW(), 1, 1),
    (3, 1, 'BAC San José - Colones',    'CR28010200009050908416', '90509084',             'CRC', 1, 3, NOW(), NOW(), 1, 1),
    (4, 1, 'BAC San José - Dólares',    'CR77010200009176559879', '91765598',             'USD', 1, 4, NOW(), NOW(), 1, 1);
