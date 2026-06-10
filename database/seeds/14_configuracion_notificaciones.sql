-- ============================================================
-- Seed: configuracion_notificaciones
-- Idempotency: INSERT IGNORE
-- ============================================================
SET NAMES utf8mb4;
USE facturas_monrachem;

INSERT IGNORE INTO `configuracion_notificaciones` (`id_configuracion`, `activar_email`, `activo`, `create_by`, `create_date`, `dias_recordatorio_pago`, `dias_recordatorio_preventivo`, `email_admin`, `email_copia_facturas`, `enviar_factura_automatica`, `frecuencia_recordatorios`, `notificar_nuevo_cliente`, `notificar_nuevo_usuario`, `update_by`, `update_date`) VALUES (1,_binary '\0',_binary '',NULL,NULL,0,2,'','',_binary '\0',7,_binary '\0',_binary '\0',2,'2026-03-13 15:44:47.231980');
