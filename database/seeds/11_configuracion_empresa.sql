-- ============================================================
-- Seed: configuracion_empresa
-- Idempotency: INSERT IGNORE
-- ============================================================
SET NAMES utf8mb4;
USE facturas_monrachem;

INSERT IGNORE INTO `configuracion_empresa` (`id_configuracion`, `color_primario`, `color_secundario`, `create_by`, `create_date`, `direccion_calle`, `direccion_ciudad`, `direccion_codigo_postal`, `direccion_colonia`, `direccion_estado`, `direccion_numero`, `direccion_pais`, `email`, `logo_url`, `nombre_comercial`, `razon_social`, `regimen_fiscal`, `rfc`, `sitio_web`, `telefono`, `update_by`, `update_date`, `barrio`, `canton`, `codigo_actividad`, `codigo_provincia`, `distrito`, `nombre_comercial_fe`, `numero_identificacion`, `otras_senas`, `tipo_identificacion`) VALUES (7,'#007bff','#6c757d',2,'2026-05-08 21:00:18.531285','Av. Principal','alajuela','20104',NULL,'Alajuela',NULL,'Costa Rica','contacto@monrachem.com','https://www.monrachem.com/img/logo-empresa.png','Monrachem','EMANUEL SOTO LEAL','R├®gimen simplificado','MCH123456ABC','https://www.monrachem.com','86386259',2,'2026-05-08 21:00:18.531285',NULL,'01','4773.0','2','04','EMANUEL SOTO LEAL','118200878',NULL,'CEDULA_FISICA');
