-- ============================================================================
-- FIX: Cambiar tipo de campos de auditoría en configuracion_notificaciones
-- Fecha: 13 de octubre de 2025
-- Sprint: Sprint 2 - Fase 5 (Notificaciones)
-- Punto: Fix 4 - Campos de auditoría deben ser INT, no VARCHAR
-- ============================================================================

USE whats_orders_manager;

-- ============================================================================
-- PROBLEMA DETECTADO:
-- ============================================================================
/*
Error al guardar configuración de notificaciones:
Caused by: java.lang.ClassCastException: Cannot cast java.lang.Integer to java.lang.String

CAUSA:
- AuditorAwareImpl retorna Optional<Integer> (ID del usuario)
- Los campos create_by y update_by eran VARCHAR(50)
- Spring Data JPA intenta castear Integer a String y falla

SOLUCIÓN:
- Cambiar create_by y update_by a INT
- Agregar foreign keys a tabla usuario
*/

-- ============================================================================
-- PASO 1: Verificar datos existentes
-- ============================================================================

-- Ver configuración actual
SELECT * FROM configuracion_notificaciones;

-- ============================================================================
-- PASO 2: Modificar estructura de la tabla
-- ============================================================================

-- Cambiar tipo de create_by de VARCHAR a INT
ALTER TABLE configuracion_notificaciones 
MODIFY COLUMN create_by INT COMMENT 'ID del usuario que creó el registro';

-- Cambiar tipo de update_by de VARCHAR a INT  
ALTER TABLE configuracion_notificaciones 
MODIFY COLUMN update_by INT COMMENT 'ID del usuario que modificó el registro';

-- ============================================================================
-- PASO 3: Agregar foreign keys
-- ============================================================================

-- Agregar FK para create_by (si hay datos, deben ser IDs válidos de usuario)
ALTER TABLE configuracion_notificaciones 
ADD CONSTRAINT fk_configuracion_notif_create_by 
FOREIGN KEY (create_by) REFERENCES usuario(id_usuario);

-- Agregar FK para update_by
ALTER TABLE configuracion_notificaciones 
ADD CONSTRAINT fk_configuracion_notif_update_by 
FOREIGN KEY (update_by) REFERENCES usuario(id_usuario);

-- ============================================================================
-- PASO 4: Limpiar datos inválidos (si los hay)
-- ============================================================================

/*
Si el registro insertado inicialmente tiene create_by = 'SYSTEM' (string),
necesitamos limpiarlo o convertirlo a NULL.

Opción 1: Poner NULL (recomendado para datos de sistema)
*/
UPDATE configuracion_notificaciones 
SET create_by = NULL 
WHERE create_by IS NOT NULL AND create_by NOT IN (SELECT id_usuario FROM usuario);

/*
Opción 2: Asignar al primer usuario admin (si existe)
-- Esto solo se ejecutaría si hay un usuario admin
UPDATE configuracion_notificaciones 
SET create_by = (SELECT id_usuario FROM usuario WHERE rol = 'ADMIN' LIMIT 1)
WHERE create_by IS NOT NULL AND create_by NOT IN (SELECT id_usuario FROM usuario);
*/

-- ============================================================================
-- PASO 5: Verificación
-- ============================================================================

-- Verificar la nueva estructura
DESCRIBE configuracion_notificaciones;

-- Verificar las foreign keys
SELECT 
    CONSTRAINT_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM 
    INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE 
    TABLE_SCHEMA = 'whats_orders_manager'
    AND TABLE_NAME = 'configuracion_notificaciones'
    AND REFERENCED_TABLE_NAME IS NOT NULL;

-- Verificar datos después del cambio
SELECT * FROM configuracion_notificaciones;

-- ============================================================================
-- ROLLBACK (En caso de error)
-- ============================================================================

/*
-- Eliminar foreign keys
ALTER TABLE configuracion_notificaciones DROP FOREIGN KEY fk_configuracion_notif_create_by;
ALTER TABLE configuracion_notificaciones DROP FOREIGN KEY fk_configuracion_notif_update_by;

-- Volver create_by y update_by a VARCHAR
ALTER TABLE configuracion_notificaciones MODIFY COLUMN create_by VARCHAR(50);
ALTER TABLE configuracion_notificaciones MODIFY COLUMN update_by VARCHAR(50);
*/

-- ============================================================================
-- NOTAS IMPORTANTES
-- ============================================================================

/*
1. CONSISTENCIA CON OTRAS TABLAS:
   - Todas las entidades del sistema usan AuditorAwareImpl
   - AuditorAwareImpl retorna Integer (ID del usuario)
   - Todas las tablas deben tener create_by y update_by como INT

2. DATOS EXISTENTES:
   - Si create_by = 'SYSTEM' (string), se pondrá NULL
   - Los registros creados por el sistema no tienen usuario asociado
   - Es aceptable que create_by sea NULL para datos de sistema

3. FOREIGN KEYS:
   - Aseguran integridad referencial
   - Evitan IDs de usuario inválidos
   - ON DELETE y ON UPDATE no especificados (comportamiento por defecto: RESTRICT)

4. PRÓXIMOS PASOS:
   - Reiniciar aplicación después de ejecutar este script
   - Probar guardar configuración de notificaciones
   - Verificar que create_by y update_by se guarden correctamente como Integer

5. PREVENCIÓN:
   - Al crear nuevas entidades, usar Integer para campos de auditoría
   - Mantener consistencia con AuditorAwareImpl<Integer>
*/

-- ============================================================================
-- FIN DEL FIX
-- ============================================================================

/*
RESULTADO ESPERADO:
✅ Campos create_by y update_by son INT
✅ Foreign keys agregadas a tabla usuario
✅ Datos inválidos limpiados (SYSTEM → NULL)
✅ ConfiguracionNotificaciones.java usa Integer para auditoría
✅ Error de ClassCastException resuelto
*/
