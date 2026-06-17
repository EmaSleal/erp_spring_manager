-- ============================================================
-- Seed: empresa
-- Idempotency: INSERT IGNORE
-- ============================================================
SET NAMES utf8mb4;
USE facturas_monrachem;

INSERT IGNORE INTO `empresa`
    (`id_empresa`, `activo`, `direccion`, `email`, `favicon`, `logo`,
     `nombre_comercial`, `nombre_empresa`, `ruc`, `sitio_web`, `telefono`,
     `create_by`, `create_date`, `update_by`, `update_date`,
     `barrio`, `canton`, `codigo_actividad`, `descripcion_actividad`, `distrito`,
     `nombre_comercial_fe`, `numero_identificacion`, `otras_senas`, `provincia`, `tipo_identificacion`,
     `sello_tipo_empresa`, `texto_legal`)
VALUES
    (1, _binary '', 'Alajuela, alajuela, Costa Rica', 'contacto@miempresa.com', NULL, NULL,
     'EMANUEL SOTO LEAL', 'Monrachem', '118200878', '', '+50686386259',
     NULL, NULL, 2, '2025-10-27 02:47:24.908984',
     NULL, NULL, NULL, NULL, NULL,
     NULL, NULL, NULL, NULL, NULL,
     NULL, NULL);
