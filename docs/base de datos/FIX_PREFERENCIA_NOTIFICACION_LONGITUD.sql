-- ============================================================================
-- FIX: Aumentar longitud de columnas en preferencia_notificacion
-- Fecha: 26 de diciembre de 2025
-- Problema: Data truncated for column 'tipo_notificacion' at row 1
-- ============================================================================
-- Al crear un nuevo usuario, el trigger que crea las preferencias automáticas
-- está fallando porque las columnas tipo_notificacion y canal tienen una
-- longitud insuficiente.
-- ============================================================================

USE facturas_monrachem;

-- ============================================================================
-- PASO 1: Verificar estructura actual
-- ============================================================================
DESCRIBE preferencia_notificacion;

-- ============================================================================
-- PASO 2: Modificar columnas para aumentar longitud y permitir NULL
-- ============================================================================

-- Modificar tipo_notificacion: aumentar de 50 a 100 caracteres
ALTER TABLE preferencia_notificacion 
MODIFY COLUMN tipo_notificacion VARCHAR(100) NULL
COMMENT 'Tipo de notificación (NULL = aplica para todos los tipos)';

-- Modificar canal: aumentar de 30 a 50 caracteres
ALTER TABLE preferencia_notificacion 
MODIFY COLUMN canal VARCHAR(50) NULL
COMMENT 'Canal de notificación (NULL = aplica para todos los canales)';

-- ============================================================================
-- PASO 3: Verificar cambios
-- ============================================================================
DESCRIBE preferencia_notificacion;

-- Verificar que no hay datos truncados
SELECT 
    id_preferencia,
    id_usuario,
    tipo_notificacion,
    LENGTH(tipo_notificacion) as longitud_tipo,
    canal,
    LENGTH(canal) as longitud_canal,
    activa
FROM preferencia_notificacion
WHERE tipo_notificacion IS NOT NULL OR canal IS NOT NULL
ORDER BY id_usuario, tipo_notificacion;

-- ============================================================================
-- PASO 4: Probar inserción (opcional - para verificar)
-- ============================================================================

/*
-- Ejemplo de inserción que antes fallaba:
INSERT INTO preferencia_notificacion (
    id_usuario,
    tipo_notificacion,
    canal,
    activa,
    notificaciones_desactivadas_global,
    frecuencia,
    solo_horario_laboral,
    create_date,
    update_date
) VALUES (
    1,  -- Reemplazar con un id_usuario válido
    'FACTURA_PROXIMA_VENCER',  -- 23 caracteres (antes fallaba si columna era < 23)
    'WHATSAPP',                 -- 8 caracteres
    true,
    false,
    'INMEDIATA',
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Limpiar la prueba:
-- DELETE FROM preferencia_notificacion WHERE id_preferencia = LAST_INSERT_ID();
*/

-- ============================================================================
-- NOTAS IMPORTANTES
-- ============================================================================

/*
1. VALORES DEL ENUM TipoNotificacion (Java):
   - FACTURA_CREADA (15 chars)
   - FACTURA_VENCIDA (15 chars)
   - FACTURA_PROXIMA_VENCER (23 chars) ⚠️ El más largo
   - PAGO_RECIBIDO (13 chars)
   - STOCK_BAJO (10 chars)
   - NUEVO_CLIENTE (14 chars)
   - NUEVO_USUARIO (13 chars)
   - MENSAJE_WHATSAPP (16 chars)
   - SISTEMA (7 chars)
   
   → Longitud máxima necesaria: 23 caracteres
   → Longitud configurada: 100 caracteres (con margen para futuros tipos)

2. VALORES DEL ENUM CanalNotificacion (Java):
   - EMAIL (5 chars)
   - WEB (3 chars)
   - WHATSAPP (8 chars)
   
   → Longitud máxima necesaria: 8 caracteres
   → Longitud configurada: 50 caracteres (con margen para futuros canales)

3. NULL PERMITIDO:
   - tipo_notificacion puede ser NULL (significa "todos los tipos")
   - canal puede ser NULL (significa "todos los canales")
   - Esto es necesario para la preferencia global del trigger

4. IMPACTO:
   - Los cambios son compatibles con versiones anteriores
   - No afecta datos existentes
   - Permite que el trigger funcione correctamente
   - Hibernate generará las columnas con estas especificaciones en adelante

5. TRIGGER AFECTADO:
   - TRIGGER_PREFERENCIAS_USUARIO.sql
   - Stored Procedure: sp_crear_preferencias_usuario
   - Ahora podrá crear preferencias con tipo_notificacion NULL sin errores
*/

-- ============================================================================
-- RESULTADO ESPERADO
-- ============================================================================
/*
✅ Columna tipo_notificacion modificada a VARCHAR(100) NULL
✅ Columna canal modificada a VARCHAR(50) NULL
✅ Trigger puede crear preferencias sin errores de truncamiento
✅ Usuarios nuevos se crean correctamente con preferencias automáticas
*/
