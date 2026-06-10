-- ============================================================
-- Seed: configuracion_facturacion
-- Idempotency: INSERT IGNORE
-- ============================================================
SET NAMES utf8mb4;
USE facturas_monrachem;

INSERT IGNORE INTO `configuracion_facturacion` (`id`, `activo`, `create_by`, `create_date`, `decimales`, `formato_numero`, `igv`, `incluir_igv_en_precio`, `moneda`, `nota_pie_pagina`, `numero_actual`, `numero_inicial`, `prefijo_factura`, `serie_factura`, `simbolo_moneda`, `terminos_condiciones`, `update_by`, `update_date`) VALUES (1,_binary '',2,'2025-10-13 17:52:04.152074',2,'{serie}-{numero}',13.00,_binary '','CRC',NULL,13,1,'FAC','FA01','Ôéí',NULL,2,'2026-05-08 02:58:38.513133');
