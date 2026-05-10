-- ============================================================================
-- DATOS INICIALES - SPRINT 4 (VERSIÓN COMPLETA)
-- ERP Orders Manager
-- Fecha: 22 de diciembre de 2025
-- ============================================================================
-- Inserta configuraciones iniciales para:
-- - Configuración de Empresa
-- - Configuración de Facturación  
-- - Configuración de Email
-- - Parámetros del Sistema (50+ parámetros organizados)
--
-- INSTRUCCIONES:
-- 1. Asegúrate de que Hibernate ya creó las tablas
-- 2. Ejecuta este script: source EJECUTAR_DATOS_INICIALES.sql
-- 3. Verifica los datos desde el panel de configuración
-- ============================================================================

USE facturas_monrachem;

-- ============================================================================
-- 1. CONFIGURACIÓN DE EMPRESA
-- ============================================================================

INSERT INTO configuracion_empresa (
    nombre_empresa,
    razon_social,
    rfc,
    direccion,
    ciudad,
    estado,
    codigo_postal,
    pais,
    telefono,
    email,
    sitio_web,
    eslogan,
    moneda_predeterminada,
    idioma_predeterminado,
    zona_horaria,
    formato_fecha,
    activa,
    create_date
) VALUES (
    'Monrachem',
    'Monrachem S.A. de C.V.',
    'MCH850101ABC',
    'Av. Principal #123, Col. Centro',
    'Ciudad de México',
    'CDMX',
    '01000',
    'México',
    '55-1234-5678',
    'contacto@monrachem.com',
    'https://www.monrachem.com',
    'Soluciones químicas de calidad',
    'MXN',
    'es',
    'America/Costa_Rica',
    'dd/MM/yyyy',
    TRUE,
    CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE
    nombre_empresa = VALUES(nombre_empresa),
    update_date = CURRENT_TIMESTAMP;

-- ============================================================================
-- 2. CONFIGURACIÓN DE FACTURACIÓN
-- ============================================================================

INSERT INTO configuracion_facturacion (
    prefijo_factura,
    numero_siguiente,
    numero_digitos,
    terminos_condiciones,
    nota_pie_factura,
    mostrar_impuestos,
    impuesto_predeterminado,
    dias_vencimiento_predeterminado,
    mensaje_vencimiento,
    formato_numero_factura,
    permitir_descuentos,
    descuento_maximo_porcentaje,
    enviar_email_automatico,
    plantilla_email,
    activa,
    create_date
) VALUES (
    'F',
    1,
    4,
    'Pago a realizar dentro de los días especificados. Intereses moratorios del 2% mensual.',
    'Gracias por su preferencia. Para dudas: soporte@monrachem.com',
    TRUE,
    16.00,
    30,
    'Factura vencida. Por favor proceda con el pago.',
    'PREFIJO-NUMERO',
    TRUE,
    20.00,
    TRUE,
    'default',
    TRUE,
    CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE
    prefijo_factura = VALUES(prefijo_factura),
    update_date = CURRENT_TIMESTAMP;

-- ============================================================================
-- 3. CONFIGURACIÓN DE EMAIL  
-- ============================================================================

INSERT INTO configuracion_email (
    host,
    puerto,
    usuario,
    `password`,
    usar_ssl,
    usar_tls,
    remitente_nombre,
    remitente_email,
    timeout_conexion,
    reintentos_envio,
    intervalo_reintentos,
    plantilla_bienvenida,
    plantilla_factura,
    plantilla_pago,
    plantilla_vencimiento,
    habilitar_logs,
    activa,
    create_date
) VALUES (
    'smtp.gmail.com',
    587,
    'EMAIL_PLACEHOLDER',
    NULL, -- Password desde .env.local
    FALSE,
    TRUE,
    'Monrachem - Sistema ERP',
    'noreply@monrachem.com',
    30000,
    3,
    60000,
    'bienvenida',
    'factura_creada',
    'pago_recibido',
    'factura_vencida',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE
    host = VALUES(host),
    update_date = CURRENT_TIMESTAMP;

-- ============================================================================
-- 4. PARÁMETROS DEL SISTEMA
-- ============================================================================
-- NOTA: Solo se insertan parámetros que NO existen en la BD actual
-- Los siguientes ya existen y NO se duplicarán:
--   - APP_NOMBRE, APP_VERSION, TIEMPO_SESION, PAGINA_REGISTROS (GENERAL)
--   - WHATSAPP_ACTIVO, WHATSAPP_RESPUESTA_AUTO, WHATSAPP_MENSAJE_BIENVENIDA (WHATSAPP)
--   - WHATSAPP_HORARIO_ACTIVO_INICIO, WHATSAPP_HORARIO_ACTIVO_FIN (WHATSAPP)
--   - FACTURA_DIAS_VENCIMIENTO, FACTURA_ALERTA_VENCIMIENTO (FACTURACION)
--   - FACTURA_INCLUIR_LOGO, FACTURA_DESCUENTO_MAXIMO (FACTURACION)
--   - NOTIF_EMAIL_ACTIVO, NOTIF_WHATSAPP_ACTIVO, NOTIF_WEB_ACTIVO, NOTIF_DIAS_RETENER (NOTIFICACIONES)

-- 4.1 GENERAL (Solo nuevos)
INSERT INTO parametro_sistema (clave, valor, tipo_dato, categoria, descripcion, es_editable, activo) VALUES
('MODO_MANTENIMIENTO', 'false', 'BOOLEAN', 'GENERAL', 'Modo mantenimiento del sistema', TRUE, TRUE),
('HABILITAR_REGISTRO', 'true', 'BOOLEAN', 'GENERAL', 'Permitir registro de nuevos usuarios', TRUE, TRUE)
ON DUPLICATE KEY UPDATE valor = VALUES(valor);

-- 4.2 SEGURIDAD (Todos nuevos)
INSERT INTO parametro_sistema (clave, valor, tipo_dato, categoria, descripcion, es_editable, activo) VALUES
('SESION_TIMEOUT', '3600', 'INTEGER', 'SEGURIDAD', 'Timeout de sesión en segundos', TRUE, TRUE),
('INTENTOS_LOGIN_MAX', '5', 'INTEGER', 'SEGURIDAD', 'Intentos máximos de login fallidos', TRUE, TRUE),
('BLOQUEO_DURACION', '900', 'INTEGER', 'SEGURIDAD', 'Duración de bloqueo en segundos', TRUE, TRUE),
('REQUERIR_2FA', 'false', 'BOOLEAN', 'SEGURIDAD', 'Requerir autenticación 2FA', TRUE, TRUE),
('PASSWORD_MIN_LENGTH', '8', 'INTEGER', 'SEGURIDAD', 'Longitud mínima de contraseña', TRUE, TRUE),
('PASSWORD_EXPIRACION_DIAS', '90', 'INTEGER', 'SEGURIDAD', 'Días para expiración de contraseña', TRUE, TRUE)
ON DUPLICATE KEY UPDATE valor = VALUES(valor);

-- 4.3 FACTURACIÓN (Solo nuevos - evitando duplicados)
INSERT INTO parametro_sistema (clave, valor, tipo_dato, categoria, descripcion, es_editable, activo) VALUES
('FACTURA_IVA_PORCENTAJE', '13', 'DECIMAL', 'FACTURACION', 'IVA por defecto (Costa Rica)', TRUE, TRUE),
('FACTURA_PERMITIR_DESCUENTO', 'true', 'BOOLEAN', 'FACTURACION', 'Permitir aplicar descuentos', TRUE, TRUE),
('FACTURA_MONEDA', 'CRC', 'STRING', 'FACTURACION', 'Moneda por defecto (Colones)', TRUE, TRUE),
('FACTURA_TIPO_CAMBIO_USD', '520.00', 'DECIMAL', 'FACTURACION', 'Tipo de cambio Colón/Dólar', TRUE, TRUE)
ON DUPLICATE KEY UPDATE valor = VALUES(valor);

-- 4.4 NOTIFICACIONES (Solo nuevos - evitando duplicados)
INSERT INTO parametro_sistema (clave, valor, tipo_dato, categoria, descripcion, es_editable, activo) VALUES
('NOTIF_MAX_REINTENTOS', '3', 'INTEGER', 'NOTIFICACIONES', 'Reintentos máximos de envío', TRUE, TRUE),
('NOTIF_INTERVALO_REINTENTOS', '60', 'INTEGER', 'NOTIFICACIONES', 'Minutos entre reintentos', TRUE, TRUE)
ON DUPLICATE KEY UPDATE valor = VALUES(valor);

-- 4.5 WHATSAPP (Solo nuevos - evitando duplicados)
INSERT INTO parametro_sistema (clave, valor, tipo_dato, categoria, descripcion, es_editable, activo) VALUES
('WHATSAPP_API_VERSION', 'v18.0', 'STRING', 'WHATSAPP', 'Versión API WhatsApp Business', TRUE, TRUE),
('WHATSAPP_TIMEOUT', '30', 'INTEGER', 'WHATSAPP', 'Timeout API en segundos', TRUE, TRUE),
('WHATSAPP_MAX_MENSAJES_DIA', '1000', 'INTEGER', 'WHATSAPP', 'Límite de mensajes por día', TRUE, TRUE),
('WHATSAPP_HABILITAR_PLANTILLAS', 'true', 'BOOLEAN', 'WHATSAPP', 'Usar plantillas aprobadas', TRUE, TRUE)
ON DUPLICATE KEY UPDATE valor = VALUES(valor);

-- 4.6 REPORTES
INSERT INTO parametro_sistema (clave, valor, tipo_dato, categoria, descripcion, es_editable, activo) VALUES
('REPORTE_CACHE_HABILITADO', 'true', 'BOOLEAN', 'REPORTES', 'Caché de reportes', TRUE, TRUE),
('REPORTE_CACHE_TIEMPO', '300', 'INTEGER', 'REPORTES', 'Tiempo caché (segundos)', TRUE, TRUE),
('REPORTE_EXPORT_MAX_REGISTROS', '10000', 'INTEGER', 'REPORTES', 'Máx registros exportación', TRUE, TRUE),
('REPORTE_FORMATO_FECHA', 'dd/MM/yyyy', 'STRING', 'REPORTES', 'Formato fecha reportes', TRUE, TRUE)
ON DUPLICATE KEY UPDATE valor = VALUES(valor);

-- 4.7 SISTEMA
INSERT INTO parametro_sistema (clave, valor, tipo_dato, categoria, descripcion, es_editable, activo) VALUES
('SISTEMA_ZONA_HORARIA', 'America/Costa_Rica', 'STRING', 'SISTEMA', 'Zona horaria', TRUE, TRUE),
('SISTEMA_IDIOMA', 'es', 'STRING', 'SISTEMA', 'Idioma del sistema', TRUE, TRUE),
('SISTEMA_LOGS_NIVEL', 'INFO', 'STRING', 'SISTEMA', 'Nivel de logging', TRUE, TRUE),
('SISTEMA_BACKUP_AUTOMATICO', 'true', 'BOOLEAN', 'SISTEMA', 'Backups automáticos', TRUE, TRUE),
('SISTEMA_BACKUP_HORA', '03:00', 'STRING', 'SISTEMA', 'Hora de backup', TRUE, TRUE),
('SISTEMA_PAGINACION_SIZE', '10', 'INTEGER', 'SISTEMA', 'Registros por página', TRUE, TRUE)
ON DUPLICATE KEY UPDATE valor = VALUES(valor);

-- 4.8 INTEGRACIONES
INSERT INTO parametro_sistema (clave, valor, tipo_dato, categoria, descripcion, es_editable, activo) VALUES
('INTEGRACION_WHATSAPP_ACTIVA', 'true', 'BOOLEAN', 'INTEGRACIONES', 'WhatsApp Business API', TRUE, TRUE),
('INTEGRACION_EMAIL_ACTIVA', 'true', 'BOOLEAN', 'INTEGRACIONES', 'Servicio de email', TRUE, TRUE),
('INTEGRACION_WEBHOOK_TIMEOUT', '30', 'INTEGER', 'INTEGRACIONES', 'Timeout webhooks (seg)', TRUE, TRUE)
ON DUPLICATE KEY UPDATE valor = VALUES(valor);

-- ============================================================================
-- VERIFICACIÓN
-- ============================================================================

SELECT '==================== VERIFICACIÓN DE DATOS ====================' as '';

SELECT 'Configuración Empresa:' as '', COUNT(*) as total FROM configuracion_empresa;
SELECT 'Configuración Facturación:' as '', COUNT(*) as total FROM configuracion_facturacion;
SELECT 'Configuración Email:' as '', COUNT(*) as total FROM configuracion_email;

SELECT 'Parámetros por Categoría:' as '';
SELECT categoria, COUNT(*) as total 
FROM parametro_sistema 
WHERE activo = TRUE 
GROUP BY categoria 
ORDER BY categoria;

SELECT 'Total Parámetros Activos:' as '', COUNT(*) as total 
FROM parametro_sistema 
WHERE activo = TRUE;

SELECT '==================== INSTALACIÓN COMPLETADA ===================' as '';

/*
✅ DATOS INICIALES INSTALADOS

Próximos pasos:
1. Verificar en /configuracion/empresa
2. Ajustar valores según necesidad
3. Configurar .env.local con credenciales reales
4. Probar envío de email de prueba

Personalización:
- Cambiar nombre_empresa, RFC, dirección
- Actualizar configuración SMTP
- Ajustar parámetros según negocio
*/
