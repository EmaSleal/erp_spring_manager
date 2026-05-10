-- ============================================================================
-- MIGRATION: Crear tabla configuracion_notificaciones
-- Fecha: 13 de octubre de 2025
-- Sprint: Sprint 2 - Fase 5 (Notificaciones)
-- Punto: 5.4.1 - Modelo ConfiguracionNotificaciones
-- ============================================================================

USE whats_orders_manager;

-- ============================================================================
-- PASO 1: Crear tabla configuracion_notificaciones
-- ============================================================================

CREATE TABLE IF NOT EXISTS configuracion_notificaciones (
    id_configuracion INT AUTO_INCREMENT PRIMARY KEY,
    
    -- Configuración general
    activar_email BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Activa/desactiva el sistema de notificaciones por email',
    enviar_factura_automatica BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Enviar factura automáticamente al crearla',
    
    -- Configuración de recordatorios
    dias_recordatorio_preventivo INT DEFAULT 3 COMMENT 'Días antes del vencimiento para recordatorio preventivo',
    dias_recordatorio_pago INT DEFAULT 0 COMMENT 'Días después del vencimiento para enviar recordatorios',
    frecuencia_recordatorios INT DEFAULT 7 COMMENT 'Cada cuántos días enviar recordatorios (después del primero)',
    
    -- Notificaciones administrativas
    notificar_nuevo_cliente BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Notificar al admin cuando se crea un cliente',
    notificar_nuevo_usuario BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Notificar al admin cuando se crea un usuario',
    email_admin VARCHAR(100) COMMENT 'Email del administrador para notificaciones',
    
    -- Otras configuraciones
    email_copia_facturas VARCHAR(100) COMMENT 'Email para recibir copia de todas las facturas (BCC)',
    activo BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Indica si es la configuración activa',
    
    -- Auditoría
    create_by INT COMMENT 'ID del usuario que creó el registro',
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by INT COMMENT 'ID del usuario que modificó el registro',
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_activo (activo),
    FOREIGN KEY (create_by) REFERENCES usuario(id_usuario),
    FOREIGN KEY (update_by) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Configuración de notificaciones del sistema';

-- ============================================================================
-- PASO 2: Insertar configuración por defecto
-- ============================================================================

INSERT INTO configuracion_notificaciones (
    activar_email,
    enviar_factura_automatica,
    dias_recordatorio_preventivo,
    dias_recordatorio_pago,
    frecuencia_recordatorios,
    notificar_nuevo_cliente,
    notificar_nuevo_usuario,
    activo
    -- create_by se deja NULL para registros del sistema
) VALUES (
    TRUE,           -- activar_email: sistema activo por defecto
    FALSE,          -- enviar_factura_automatica: desactivado por defecto (usuario decide manualmente)
    3,              -- dias_recordatorio_preventivo: 3 días antes del vencimiento
    0,              -- dias_recordatorio_pago: enviar recordatorio el mismo día del vencimiento
    7,              -- frecuencia_recordatorios: cada 7 días después del primer recordatorio
    FALSE,          -- notificar_nuevo_cliente: desactivado
    FALSE,          -- notificar_nuevo_usuario: desactivado
    TRUE            -- activo: configuración activa
);

-- ============================================================================
-- PASO 3: Verificación
-- ============================================================================

-- Verificar la estructura de la tabla:
DESCRIBE configuracion_notificaciones;

-- Verificar que se insertó el registro por defecto:
SELECT * FROM configuracion_notificaciones;

-- ============================================================================
-- ROLLBACK (En caso de error)
-- ============================================================================
-- Si necesitas revertir los cambios:

-- DROP TABLE IF EXISTS configuracion_notificaciones;

-- ============================================================================
-- NOTAS IMPORTANTES
-- ============================================================================

/*
1. CONFIGURACIÓN POR DEFECTO:
   - Sistema de notificaciones ACTIVO
   - Envío automático de facturas DESACTIVADO (se envía manualmente)
   - Recordatorio preventivo: 3 días antes del vencimiento
   - Recordatorio de pago: el mismo día del vencimiento (0 días)
   - Frecuencia de recordatorios: cada 7 días

2. LÓGICA DE RECORDATORIOS:
   - Preventivo: se envía X días ANTES de fechaPago
   - Vencimiento: se envía cuando fechaPago < HOY
   - Frecuencia: después del primer recordatorio, se envía cada X días

3. SOLO UN REGISTRO ACTIVO:
   - Solo debe existir un registro con activo = TRUE
   - El sistema siempre usa el registro activo
   - Si se crea uno nuevo, el anterior debe desactivarse

4. VALIDACIONES:
   - dias_recordatorio_preventivo >= 0
   - dias_recordatorio_pago >= 0
   - frecuencia_recordatorios >= 1
   - Si notificar_nuevo_cliente = TRUE, debe haber email_admin
   - Si notificar_nuevo_usuario = TRUE, debe haber email_admin

5. INTEGRACIÓN CON SCHEDULER:
   - El RecordatorioPagoScheduler consultará esta configuración
   - Si activar_email = FALSE, no enviará notificaciones
   - Usará dias_recordatorio_pago para determinar qué facturas recordar

6. EMAIL DE COPIA (BCC):
   - Si email_copia_facturas está configurado, todas las facturas
     enviadas incluirán este email en copia oculta (BCC)
   - Útil para departamento de contabilidad o respaldo

7. PRÓXIMOS PASOS:
   - Crear ConfiguracionNotificacionesRepository
   - Crear ConfiguracionNotificacionesService
   - Crear vista configuracion/notificaciones.html
   - Integrar con FacturaController (envío automático)
   - Integrar con RecordatorioPagoScheduler
*/

-- ============================================================================
-- FIN DE LA MIGRACIÓN
-- ============================================================================

/*
RESULTADO ESPERADO:
✅ Tabla configuracion_notificaciones creada
✅ Registro por defecto insertado
✅ Índice en campo activo
✅ Estructura lista para implementar UI de configuración
*/
