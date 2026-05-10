-- ============================================================================
-- INICIALIZAR PREFERENCIAS DE NOTIFICACIÓN
-- ERP Orders Manager
-- ============================================================================
-- Este script crea preferencias predeterminadas para todos los usuarios
-- que NO tienen preferencias configuradas.
--
-- Ejecutar este script cuando:
-- 1. Se han creado usuarios ANTES de implementar el sistema de notificaciones
-- 2. La tabla preferencia_notificacion está vacía
-- 3. Se necesita resetear las preferencias a valores por defecto
-- ============================================================================

-- ============================================================================
-- PASO 1: Verificar usuarios sin preferencias
-- ============================================================================
SELECT 
    u.id_usuario,
    u.nombre,
    u.telefono,
    u.rol,
    COUNT(pn.id_preferencia) as preferencias_existentes
FROM usuario u
LEFT JOIN preferencia_notificacion pn ON u.id_usuario = pn.id_usuario
GROUP BY u.id_usuario, u.nombre, u.telefono, u.rol
HAVING COUNT(pn.id_preferencia) = 0;

-- ============================================================================
-- PASO 2: Crear preferencias globales para usuarios sin configuración
-- ============================================================================

-- Preferencia GLOBAL (aplica para todos los tipos y canales)
-- Solo se crea si el usuario NO tiene ninguna preferencia
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
)
SELECT 
    u.id_usuario,
    NULL as tipo_notificacion,  -- NULL = aplica para todos
    NULL as canal,               -- NULL = aplica para todos
    true as activa,
    false as notificaciones_desactivadas_global,
    'INMEDIATA' as frecuencia,
    false as solo_horario_laboral,
    CURRENT_TIMESTAMP as create_date,
    CURRENT_TIMESTAMP as update_date
FROM usuario u
WHERE u.activo = true
  AND NOT EXISTS (
      SELECT 1 
      FROM preferencia_notificacion pn 
      WHERE pn.id_usuario = u.id_usuario
  );

-- ============================================================================
-- PASO 3: Crear preferencias específicas para eventos críticos
-- ============================================================================

-- FACTURA_VENCIDA - WEB (para todos los usuarios sin preferencias)
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
)
SELECT 
    u.id_usuario,
    'FACTURA_VENCIDA',
    'WEB',
    true,
    false,
    'INMEDIATA',
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM usuario u
WHERE u.activo = true
  AND NOT EXISTS (
      SELECT 1 
      FROM preferencia_notificacion pn 
      WHERE pn.id_usuario = u.id_usuario 
        AND pn.tipo_notificacion = 'FACTURA_VENCIDA'
        AND pn.canal = 'WEB'
  );

-- FACTURA_VENCIDA - EMAIL (para todos los usuarios sin preferencias)
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
)
SELECT 
    u.id_usuario,
    'FACTURA_VENCIDA',
    'EMAIL',
    true,
    false,
    'INMEDIATA',
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM usuario u
WHERE u.activo = true
  AND u.email IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 
      FROM preferencia_notificacion pn 
      WHERE pn.id_usuario = u.id_usuario 
        AND pn.tipo_notificacion = 'FACTURA_VENCIDA'
        AND pn.canal = 'EMAIL'
  );

-- STOCK_BAJO - WEB (para todos los usuarios sin preferencias)
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
)
SELECT 
    u.id_usuario,
    'STOCK_BAJO',
    'WEB',
    true,
    false,
    'INMEDIATA',
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM usuario u
WHERE u.activo = true
  AND NOT EXISTS (
      SELECT 1 
      FROM preferencia_notificacion pn 
      WHERE pn.id_usuario = u.id_usuario 
        AND pn.tipo_notificacion = 'STOCK_BAJO'
        AND pn.canal = 'WEB'
  );

-- STOCK_BAJO - EMAIL (para usuarios con email)
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
)
SELECT 
    u.id_usuario,
    'STOCK_BAJO',
    'EMAIL',
    true,
    false,
    'DIARIA',  -- Resumen diario para evitar spam
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM usuario u
WHERE u.activo = true
  AND u.email IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 
      FROM preferencia_notificacion pn 
      WHERE pn.id_usuario = u.id_usuario 
        AND pn.tipo_notificacion = 'STOCK_BAJO'
        AND pn.canal = 'EMAIL'
  );

-- ============================================================================
-- PASO 4: Verificar preferencias creadas
-- ============================================================================
SELECT 
    u.id_usuario,
    u.nombre,
    u.rol,
    COUNT(pn.id_preferencia) as total_preferencias,
    SUM(CASE WHEN pn.activa = true THEN 1 ELSE 0 END) as preferencias_activas,
    SUM(CASE WHEN pn.tipo_notificacion IS NULL THEN 1 ELSE 0 END) as preferencias_globales
FROM usuario u
LEFT JOIN preferencia_notificacion pn ON u.id_usuario = pn.id_usuario
WHERE u.activo = true
GROUP BY u.id_usuario, u.nombre, u.rol
ORDER BY u.id_usuario;

-- ============================================================================
-- PASO 5: Detalle de preferencias por usuario
-- ============================================================================
SELECT 
    u.nombre as usuario,
    u.rol,
    pn.tipo_notificacion,
    pn.canal,
    pn.activa,
    pn.frecuencia,
    pn.notificaciones_desactivadas_global
FROM usuario u
INNER JOIN preferencia_notificacion pn ON u.id_usuario = pn.id_usuario
WHERE u.activo = true
ORDER BY u.id_usuario, pn.tipo_notificacion, pn.canal;

-- ============================================================================
-- OPCIONAL: Limpiar todas las preferencias (usar con precaución)
-- ============================================================================
-- DELETE FROM preferencia_notificacion;

-- ============================================================================
-- RESUMEN DE ESTADÍSTICAS
-- ============================================================================
SELECT 
    'Total usuarios activos' as metrica,
    COUNT(*) as valor
FROM usuario 
WHERE activo = true

UNION ALL

SELECT 
    'Usuarios con preferencias',
    COUNT(DISTINCT id_usuario)
FROM preferencia_notificacion

UNION ALL

SELECT 
    'Total preferencias creadas',
    COUNT(*)
FROM preferencia_notificacion

UNION ALL

SELECT 
    'Preferencias activas',
    COUNT(*)
FROM preferencia_notificacion
WHERE activa = true

UNION ALL

SELECT 
    'Preferencias globales',
    COUNT(*)
FROM preferencia_notificacion
WHERE tipo_notificacion IS NULL AND canal IS NULL;
