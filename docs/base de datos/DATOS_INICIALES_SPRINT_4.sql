-- ============================================================================
-- DATOS INICIALES SPRINT 4 - CONFIGURACIÓN
-- ERP Orders Manager
-- Fecha: 1 de diciembre de 2025
-- ============================================================================

USE facturas_monrachem;

-- ============================================================================
-- 1. CONFIGURACIÓN DE EMPRESA (Opcional - se puede crear desde UI)
-- ============================================================================

-- Verificar si ya existe configuración de empresa
SELECT COUNT(*) FROM configuracion_empresa;

-- Si no existe, insertar configuración de ejemplo
INSERT INTO configuracion_empresa (
    razon_social,
    nombre_comercial,
    rfc,
    regimen_fiscal,
    direccion_calle,
    direccion_numero,
    direccion_colonia,
    direccion_ciudad,
    direccion_estado,
    direccion_codigo_postal,
    direccion_pais,
    telefono,
    email,
    sitio_web,
    logo_url,
    color_primario,
    color_secundario,
    create_date,
    update_date
) VALUES (
    'Monrachem S.A. de C.V.',                    -- razón social
    'Monrachem',                                  -- nombre comercial
    'MCH123456ABC',                               -- RFC (ejemplo)
    '601 - General de Ley Personas Morales',     -- régimen fiscal
    'Av. Principal',                              -- calle
    '123',                                        -- número
    'Centro',                                     -- colonia
    'Ciudad de México',                           -- ciudad
    'CDMX',                                       -- estado
    '01000',                                      -- código postal
    'México',                                     -- país
    '+52 55 1234 5678',                          -- teléfono
    'contacto@monrachem.com',                     -- email
    'https://www.monrachem.com',                  -- sitio web
    '/img/logo-empresa.png',                      -- logo URL
    '#007bff',                                    -- color primario
    '#6c757d',                                    -- color secundario
    NOW(),                                        -- create_date
    NOW()                                         -- update_date
)
ON DUPLICATE KEY UPDATE 
    update_date = NOW();

-- ============================================================================
-- 2. CONFIGURACIÓN DE FACTURACIÓN (Opcional - se puede crear desde UI)
-- ============================================================================

-- Verificar si ya existe configuración de facturación
SELECT COUNT(*) FROM configuracion_facturacion;

-- Si no existe, insertar configuración de ejemplo
INSERT INTO configuracion_facturacion (
    serie_factura,
    prefijo_factura,
    numero_inicial,
    numero_actual,
    formato_numero,
    igv,
    incluir_igv_en_precio,
    moneda,
    simbolo_moneda,
    decimales,
    terminos_condiciones,
    nota_pie_pagina,
    activo,
    createDate,
    updateDate
) VALUES (
    'A',                                          -- serie
    'FAC',                                        -- prefijo
    1,                                            -- número inicial
    1,                                            -- número actual
    '{serie}-{prefijo}-{numero}',                 -- formato
    16.00,                                        -- IGV/IVA (16%)
    FALSE,                                        -- incluir IGV en precio
    'MXN',                                        -- moneda
    '$',                                          -- símbolo
    2,                                            -- decimales
    'Pago contra entrega. Garantía de 30 días.', -- términos
    'Gracias por su preferencia',                -- nota pie
    TRUE,                                         -- activo
    NOW(),                                        -- createDate
    NOW()                                         -- updateDate
)
ON DUPLICATE KEY UPDATE 
    updateDate = NOW();

-- ============================================================================
-- 3. CONFIGURACIÓN DE EMAIL SMTP (IMPORTANTE - Actualizar con datos reales)
-- ============================================================================

-- Verificar si ya existe configuración de email
SELECT COUNT(*) FROM configuracion_email;

-- Insertar configuración de ejemplo (GMAIL)
-- ⚠️ IMPORTANTE: Cambiar username y password por credenciales reales
INSERT INTO configuracion_email (
    smtp_host,
    smtp_port,
    smtp_usuario,
    smtp_password,
    smtp_ssl,
    smtp_tls,
    smtp_auth,
    email_remitente,
    nombre_remitente,
    email_copia,
    email_copia_oculta,
    activo,
    create_date,
    update_date
) VALUES (
    'smtp.gmail.com',                             -- host SMTP
    587,                                          -- puerto (587 para TLS)
    'tu-email@gmail.com',                         -- ⚠️ CAMBIAR: username
    'tu-app-password',                            -- ⚠️ CAMBIAR: password (App Password)
    FALSE,                                        -- usar SSL (no si usamos TLS)
    TRUE,                                         -- usar TLS
    TRUE,                                         -- autenticación requerida
    'tu-email@gmail.com',                         -- ⚠️ CAMBIAR: email remitente
    'Monrachem - Sistema de Facturas',           -- nombre remitente
    NULL,                                         -- email copia (opcional)
    NULL,                                         -- email copia oculta (opcional)
    FALSE,                                        -- ⚠️ DESACTIVADO hasta configurar
    NOW(),                                        -- create_date
    NOW()                                         -- update_date
)
ON DUPLICATE KEY UPDATE 
    update_date = NOW();

-- ============================================================================
-- 4. PARÁMETROS DEL SISTEMA (17 PARÁMETROS CRÍTICOS)
-- ============================================================================

-- Verificar parámetros existentes
SELECT COUNT(*) FROM parametro_sistema;

-- Limpiar tabla si existe (CUIDADO: Solo en primera instalación)
-- TRUNCATE TABLE parametro_sistema;

-- Insertar 17 parámetros del sistema
INSERT INTO parametro_sistema (clave, valor, tipo_dato, descripcion, categoria, editable, create_date, update_date) VALUES
-- GENERAL (4 parámetros)
('APP_NOMBRE', 'ERP Orders Manager', 'STRING', 'Nombre de la aplicación', 'GENERAL', TRUE, NOW(), NOW()),
('APP_VERSION', '1.0.0', 'STRING', 'Versión actual de la aplicación', 'GENERAL', FALSE, NOW(), NOW()),
('TIEMPO_SESION', '30', 'INTEGER', 'Tiempo de inactividad antes de cerrar sesión (minutos)', 'GENERAL', TRUE, NOW(), NOW()),
('PAGINA_REGISTROS', '10', 'INTEGER', 'Número de registros por página en listados', 'GENERAL', TRUE, NOW(), NOW()),

-- WHATSAPP (5 parámetros)
('WHATSAPP_ACTIVO', 'true', 'BOOLEAN', 'Habilitar integración con WhatsApp', 'WHATSAPP', TRUE, NOW(), NOW()),
('WHATSAPP_RESPUESTA_AUTO', 'true', 'BOOLEAN', 'Habilitar respuestas automáticas', 'WHATSAPP', TRUE, NOW(), NOW()),
('WHATSAPP_MENSAJE_BIENVENIDA', 'Hola! Bienvenido a Monrachem. ¿En qué podemos ayudarte?', 'STRING', 'Mensaje de bienvenida automático', 'WHATSAPP', TRUE, NOW(), NOW()),
('WHATSAPP_HORARIO_ACTIVO_INICIO', '09:00', 'STRING', 'Hora de inicio de atención (HH:MM)', 'WHATSAPP', TRUE, NOW(), NOW()),
('WHATSAPP_HORARIO_ACTIVO_FIN', '18:00', 'STRING', 'Hora de fin de atención (HH:MM)', 'WHATSAPP', TRUE, NOW(), NOW()),

-- FACTURACIÓN (4 parámetros)
('FACTURA_DIAS_VENCIMIENTO', '30', 'INTEGER', 'Días predeterminados para vencimiento de facturas', 'FACTURACION', TRUE, NOW(), NOW()),
('FACTURA_ALERTA_VENCIMIENTO', '5', 'INTEGER', 'Días antes de vencimiento para enviar alerta', 'FACTURACION', TRUE, NOW(), NOW()),
('FACTURA_INCLUIR_LOGO', 'true', 'BOOLEAN', 'Incluir logo de empresa en facturas PDF', 'FACTURACION', TRUE, NOW(), NOW()),
('FACTURA_DESCUENTO_MAXIMO', '20.00', 'DECIMAL', 'Porcentaje máximo de descuento permitido', 'FACTURACION', TRUE, NOW(), NOW()),

-- NOTIFICACIONES (4 parámetros)
('NOTIF_EMAIL_ACTIVO', 'true', 'BOOLEAN', 'Habilitar notificaciones por email', 'NOTIFICACIONES', TRUE, NOW(), NOW()),
('NOTIF_WHATSAPP_ACTIVO', 'false', 'BOOLEAN', 'Habilitar notificaciones por WhatsApp', 'NOTIFICACIONES', TRUE, NOW(), NOW()),
('NOTIF_WEB_ACTIVO', 'true', 'BOOLEAN', 'Habilitar notificaciones web (en tiempo real)', 'NOTIFICACIONES', TRUE, NOW(), NOW()),
('NOTIF_DIAS_RETENER', '90', 'INTEGER', 'Días para retener notificaciones en el sistema', 'NOTIFICACIONES', TRUE, NOW(), NOW())

ON DUPLICATE KEY UPDATE 
    valor = VALUES(valor),
    update_date = NOW();

-- ============================================================================
-- 5. VERIFICACIÓN DE DATOS INSERTADOS
-- ============================================================================

-- Verificar configuración de empresa
SELECT 
    'EMPRESA' as tabla,
    COUNT(*) as total_registros
FROM configuracion_empresa;

-- Verificar configuración de facturación
SELECT 
    'FACTURACION' as tabla,
    COUNT(*) as total_registros,
    SUM(CASE WHEN activo = TRUE THEN 1 ELSE 0 END) as activos
FROM configuracion_facturacion;

-- Verificar configuración de email
SELECT 
    'EMAIL' as tabla,
    COUNT(*) as total_registros,
    SUM(CASE WHEN activo = TRUE THEN 1 ELSE 0 END) as activos
FROM configuracion_email;

-- Verificar parámetros del sistema
SELECT 
    categoria,
    COUNT(*) as total,
    SUM(CASE WHEN editable = TRUE THEN 1 ELSE 0 END) as editables,
    SUM(CASE WHEN editable = FALSE THEN 1 ELSE 0 END) as sistema
FROM parametro_sistema
GROUP BY categoria
ORDER BY categoria;

-- Resumen general
SELECT 
    'RESUMEN' as detalle,
    (SELECT COUNT(*) FROM configuracion_empresa) as empresas,
    (SELECT COUNT(*) FROM configuracion_facturacion) as facturaciones,
    (SELECT COUNT(*) FROM configuracion_email) as emails,
    (SELECT COUNT(*) FROM parametro_sistema) as parametros;

-- ============================================================================
-- 6. NOTAS IMPORTANTES
-- ============================================================================

/*
⚠️ ANTES DE USAR ESTE SCRIPT:

1. CONFIGURACIÓN DE EMAIL:
   - Cambiar 'tu-email@gmail.com' por tu email real
   - Cambiar 'tu-app-password' por tu App Password de Gmail
   - Para Gmail: https://myaccount.google.com/apppasswords
   - Activar 'activo = TRUE' después de configurar

2. CONFIGURACIÓN DE EMPRESA:
   - Actualizar con datos reales de tu empresa
   - Cambiar RFC por uno válido
   - Actualizar dirección, teléfono, email

3. PARÁMETROS DEL SISTEMA:
   - Los parámetros con editable = FALSE son del sistema
   - Los parámetros con editable = TRUE pueden modificarse desde UI
   - Ajustar valores según necesidades

4. INICIALIZACIÓN AUTOMÁTICA:
   - También puedes usar el endpoint POST /api/configuracion/parametros/inicializar
   - Esto creará los 17 parámetros desde la aplicación

5. PERMISOS MYSQL:
   - Si tienes error "Access denied", ejecuta:
     CREATE USER 'root'@'%' IDENTIFIED BY 'password';
     GRANT ALL PRIVILEGES ON facturas_monrachem.* TO 'root'@'%';
     FLUSH PRIVILEGES;
*/

-- ============================================================================
-- FIN DEL SCRIPT
-- ============================================================================
