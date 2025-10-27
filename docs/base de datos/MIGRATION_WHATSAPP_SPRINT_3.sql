-- ============================================
-- MIGRACIÓN: WHATSAPP INTEGRATION - SPRINT 3
-- Autor: EmaSleal
-- Fecha: 26 octubre 2025
-- Descripción: Tablas para integración Meta WhatsApp Business API
-- Versión: 1.0
-- ============================================
-- 
-- CAMBIOS INCLUIDOS:
-- 1. Tabla mensaje_whatsapp - Registro de mensajes enviados/recibidos
-- 2. Tabla plantilla_whatsapp - Gestión de plantillas aprobadas
-- 3. Índices optimizados para consultas frecuentes
-- 4. Particionamiento por año en mensaje_whatsapp
-- 5. Inserción de 5 plantillas aprobadas en Fase 0
-- 
-- DEPENDENCIAS:
-- - Tabla factura debe existir (FK)
-- - MySQL 8.0+ (para particionamiento y JSON)
-- 
-- INSTRUCCIONES:
-- 1. Ejecutar en base de datos: facturas_monrachem
-- 2. Verificar resultados con queries de validación al final
-- 3. Actualizar docs/ESTADO_PROYECTO.md con fecha de ejecución
-- 
-- ============================================

USE facturas_monrachem;

-- ============================================
-- LIMPIEZA (solo si re-ejecutas)
-- ============================================
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS mensaje_whatsapp;
DROP TABLE IF EXISTS plantilla_whatsapp;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- TABLA: mensaje_whatsapp
-- ============================================
-- Propósito: Registrar todos los mensajes de WhatsApp (enviados y recibidos)
-- Particionamiento: Por año de fecha_envio para optimizar consultas históricas
-- Estimación: ~1000 mensajes/mes = 12,000/año
-- ============================================

CREATE TABLE mensaje_whatsapp (
    -- Identificadores
    id_mensaje BIGINT PRIMARY KEY AUTO_INCREMENT 
        COMMENT 'ID único del mensaje en nuestra BD',
    
    -- Datos del mensaje
    telefono VARCHAR(20) NOT NULL 
        COMMENT 'Número en formato internacional +525512345678',
    mensaje TEXT 
        COMMENT 'Contenido del mensaje de texto',
    tipo ENUM('ENVIADO', 'RECIBIDO') NOT NULL 
        COMMENT 'Dirección del mensaje: enviado por nosotros o recibido del cliente',
    
    -- Estado del mensaje (ciclo de vida)
    estado ENUM('PENDIENTE', 'ENVIADO', 'ENTREGADO', 'LEIDO', 'FALLIDO') 
        NOT NULL DEFAULT 'PENDIENTE'
        COMMENT 'Estado actual del mensaje según webhooks de Meta',
    
    -- Relaciones
    id_factura BIGINT NULL
        COMMENT 'FK a factura si el mensaje está relacionado con una factura',
    
    -- Metadatos de WhatsApp
    id_mensaje_whatsapp VARCHAR(255) NULL
        COMMENT 'ID del mensaje en Meta (wamid.xxx) - único en WhatsApp',
    
    -- Timestamps
    fecha_envio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP 
        COMMENT 'Cuándo se envió o recibió el mensaje',
    fecha_entrega TIMESTAMP NULL 
        COMMENT 'Cuándo Meta confirmó entrega (webhook)',
    fecha_lectura TIMESTAMP NULL 
        COMMENT 'Cuándo el destinatario leyó el mensaje (webhook)',
    
    -- Control de errores
    error TEXT NULL
        COMMENT 'Mensaje de error si el estado es FALLIDO',
    
    -- Datos adicionales (JSON)
    metadata JSON NULL
        COMMENT 'Datos adicionales: plantilla usada, parámetros, headers, etc.',
    
    -- ========================================
    -- ÍNDICES
    -- ========================================
    -- Búsquedas por teléfono y fecha (historial)
    INDEX idx_telefono_fecha (telefono, fecha_envio DESC),
    
    -- Búsquedas por factura relacionada
    INDEX idx_factura (id_factura),
    
    -- Filtros por estado (pendientes, fallidos)
    INDEX idx_estado (estado),
    
    -- Filtros por tipo (enviados vs recibidos)
    INDEX idx_tipo (tipo),
    
    -- Búsqueda por ID de WhatsApp (actualizar estados via webhook)
    INDEX idx_mensaje_whatsapp (id_mensaje_whatsapp),
    
    -- Ordenamiento cronológico
    INDEX idx_fecha_envio (fecha_envio DESC),
    
    -- ========================================
    -- FOREIGN KEYS
    -- ========================================
    CONSTRAINT fk_mensaje_factura 
        FOREIGN KEY (id_factura) 
        REFERENCES factura(id_factura)
        ON DELETE SET NULL
        ON UPDATE CASCADE
        
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Registro de mensajes WhatsApp enviados y recibidos'
  
  -- ========================================
  -- PARTICIONAMIENTO POR AÑO
  -- ========================================
  -- Mejora el rendimiento de consultas históricas
  -- Facilita el mantenimiento (eliminar particiones antiguas)
  PARTITION BY RANGE (YEAR(fecha_envio)) (
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p2027 VALUES LESS THAN (2028),
    PARTITION p2028 VALUES LESS THAN (2029),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- ============================================
-- TABLA: plantilla_whatsapp
-- ============================================
-- Propósito: Gestionar plantillas de mensajes aprobadas por Meta
-- Sincronización: Manual (plantillas aprobadas en Meta Business Manager)
-- Estimación: ~10-20 plantillas máximo
-- ============================================

CREATE TABLE plantilla_whatsapp (
    -- Identificadores
    id_plantilla INT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'ID único de la plantilla en nuestra BD',
    
    -- Nombres
    nombre VARCHAR(50) UNIQUE NOT NULL 
        COMMENT 'Nombre interno en nuestra app (ej: factura_generada)',
    codigo_meta VARCHAR(50) NOT NULL 
        COMMENT 'Nombre de la plantilla en Meta Business Manager (puede ser igual)',
    
    -- Configuración
    categoria ENUM('UTILITY', 'MARKETING', 'AUTHENTICATION') 
        NOT NULL DEFAULT 'UTILITY'
        COMMENT 'Categoría según Meta: UTILITY (transaccional), MARKETING (promocional), AUTHENTICATION (OTP)',
    idioma VARCHAR(10) NOT NULL DEFAULT 'es_MX' 
        COMMENT 'Código de idioma ISO (es_MX, en_US, etc.)',
    
    -- Contenido
    contenido TEXT NOT NULL 
        COMMENT 'Texto de la plantilla con placeholders {{1}}, {{2}}, etc.',
    parametros JSON NULL
        COMMENT 'Array JSON con nombres descriptivos de parámetros ["nombre_cliente", "numero_factura", ...]',
    
    -- Estado en Meta
    estado_meta ENUM('PENDING', 'APPROVED', 'REJECTED') 
        NOT NULL DEFAULT 'PENDING'
        COMMENT 'Estado de aprobación en Meta Business Manager',
    template_id VARCHAR(50) NULL
        COMMENT 'ID de la plantilla en Meta (obtenido de Business Manager)',
    
    -- Control interno
    activo BOOLEAN NOT NULL DEFAULT TRUE 
        COMMENT 'Si está activa para usar en el sistema (podemos desactivar sin eliminar)',
    
    -- Timestamps
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Cuándo se creó en nuestra BD',
    fecha_aprobacion TIMESTAMP NULL 
        COMMENT 'Cuándo Meta aprobó la plantilla',
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        COMMENT 'Última actualización de la plantilla',
    
    -- ========================================
    -- ÍNDICES
    -- ========================================
    INDEX idx_nombre (nombre),
    INDEX idx_codigo_meta (codigo_meta),
    INDEX idx_estado (estado_meta),
    INDEX idx_activo (activo)
    
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Plantillas de mensajes WhatsApp aprobadas por Meta';

-- ============================================
-- DATOS INICIALES: PLANTILLAS APROBADAS
-- ============================================
-- Estas son las 5 plantillas aprobadas en Fase 0 (26 oct 2025)
-- Template IDs obtenidos de Meta Business Manager
-- ============================================

INSERT INTO plantilla_whatsapp 
    (nombre, codigo_meta, categoria, idioma, contenido, parametros, estado_meta, activo, fecha_aprobacion) 
VALUES
    -- Plantilla 1: Factura Generada
    (
        'factura_generada',
        'factura_generada',
        'UTILITY',
        'es_MX',
        'Hola {{1}}, tu factura #{{2}} por {{3}} ha sido generada.

📅 Fecha de vencimiento: {{4}}
🔗 Ver detalles: {{5}}

Gracias por tu preferencia.',
        '["nombre_cliente", "numero_factura", "monto", "fecha_vencimiento", "url_detalle"]',
        'APPROVED',
        TRUE,
        NOW()
    ),
    
    -- Plantilla 2: Recordatorio de Pago
    (
        'recordatorio_pago',
        'recordatorio_pago',
        'UTILITY',
        'es_MX',
        'Hola {{1}},

🔔 Recordatorio amistoso:
Tu factura #{{2}} por {{3}} vence {{4}}.

💳 Puedes realizar el pago en:
{{5}}

¿Tienes alguna pregunta? Responde a este mensaje.',
        '["nombre_cliente", "numero_factura", "monto", "fecha_vencimiento", "metodos_pago"]',
        'APPROVED',
        TRUE,
        NOW()
    ),
    
    -- Plantilla 3: Pago Recibido
    (
        'pago_recibido',
        'pago_recibido',
        'UTILITY',
        'es_MX',
        '✅ ¡Pago confirmado!

Hola {{1}}, hemos recibido tu pago de {{2}} por la factura #{{3}}.

📅 Fecha de pago: {{4}}
💳 Método: {{5}}

Gracias por tu puntualidad.',
        '["nombre_cliente", "monto", "numero_factura", "fecha_pago", "metodo_pago"]',
        'APPROVED',
        TRUE,
        NOW()
    ),
    
    -- Plantilla 4: Factura Vencida
    (
        'factura_vencida',
        'factura_vencida',
        'UTILITY',
        'es_MX',
        '⚠️ Factura vencida

Hola {{1}},

La factura #{{2}} por {{3}} venció el {{4}}.

Por favor, realiza el pago a la brevedad para evitar cargos adicionales.

📞 ¿Necesitas ayuda? Contáctanos respondiendo este mensaje.',
        '["nombre_cliente", "numero_factura", "monto", "fecha_vencimiento"]',
        'APPROVED',
        TRUE,
        NOW()
    ),
    
    -- Plantilla 5: Bienvenida Cliente
    (
        'bienvenida_cliente',
        'bienvenida_cliente',
        'MARKETING',
        'es_MX',
        '¡Bienvenido/a {{1}}! 👋

Gracias por confiar en nosotros. A partir de ahora recibirás notificaciones de tus facturas y pedidos por WhatsApp.

✨ Beneficios:
• Alertas de facturas
• Recordatorios de pago
• Atención personalizada

Responde "AYUDA" para ver opciones disponibles.',
        '["nombre_cliente"]',
        'APPROVED',
        TRUE,
        NOW()
    );

-- ============================================
-- QUERIES DE VALIDACIÓN
-- ============================================
-- Ejecutar para verificar que la migración fue exitosa
-- ============================================

-- 1. Verificar creación de tablas
SELECT 
    TABLE_NAME,
    ENGINE,
    TABLE_ROWS,
    CREATE_TIME
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'facturas_monrachem' 
  AND TABLE_NAME IN ('mensaje_whatsapp', 'plantilla_whatsapp');

-- 2. Verificar índices de mensaje_whatsapp
SHOW INDEX FROM mensaje_whatsapp;

-- 3. Verificar particiones de mensaje_whatsapp
SELECT 
    PARTITION_NAME,
    PARTITION_METHOD,
    PARTITION_EXPRESSION,
    PARTITION_DESCRIPTION,
    TABLE_ROWS
FROM information_schema.PARTITIONS
WHERE TABLE_SCHEMA = 'facturas_monrachem' 
  AND TABLE_NAME = 'mensaje_whatsapp';

-- 4. Verificar plantillas insertadas
SELECT 
    id_plantilla,
    nombre,
    categoria,
    estado_meta,
    activo,
    JSON_LENGTH(parametros) as num_parametros,
    fecha_aprobacion
FROM plantilla_whatsapp
ORDER BY id_plantilla;

-- 5. Verificar foreign key
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    REFERENCED_TABLE_NAME,
    DELETE_RULE,
    UPDATE_RULE
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'facturas_monrachem'
  AND TABLE_NAME = 'mensaje_whatsapp';

-- ============================================
-- QUERIES DE PRUEBA (OPCIONAL)
-- ============================================
-- Usar para probar las estructuras creadas
-- ============================================

-- Insertar mensaje de prueba
/*
INSERT INTO mensaje_whatsapp 
    (telefono, mensaje, tipo, estado, id_factura, metadata)
VALUES
    ('+525512345678', 'Mensaje de prueba', 'ENVIADO', 'PENDIENTE', NULL, '{"test": true}');

-- Verificar inserción
SELECT * FROM mensaje_whatsapp WHERE id_mensaje = LAST_INSERT_ID();

-- Eliminar mensaje de prueba
DELETE FROM mensaje_whatsapp WHERE metadata LIKE '%"test": true%';
*/

-- ============================================
-- RESULTADO ESPERADO
-- ============================================
-- ✅ 2 tablas creadas (mensaje_whatsapp, plantilla_whatsapp)
-- ✅ 8 índices en mensaje_whatsapp
-- ✅ 4 índices en plantilla_whatsapp
-- ✅ 5 particiones en mensaje_whatsapp
-- ✅ 1 foreign key configurada
-- ✅ 5 plantillas insertadas
-- ============================================

-- Fin de migración
-- Fecha de ejecución: _____________
-- Ejecutado por: _____________
-- Resultado: [ ] EXITOSO  [ ] CON ERRORES
