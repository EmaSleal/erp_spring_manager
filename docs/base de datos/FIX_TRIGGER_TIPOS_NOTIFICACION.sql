-- ============================================================================
-- FIX: Corregir valores de tipo_notificacion en trigger
-- Fecha: 26 de diciembre de 2025
-- Problema: Data truncated for column 'tipo_notificacion' at row 1
-- ============================================================================
-- El trigger usa 'NUEVA_FACTURA' pero el enum Java tiene 'FACTURA_CREADA'
-- Esto causa que se intente insertar un valor inválido en la columna
-- ============================================================================

USE facturas_monrachem;

DELIMITER $$

-- ============================================================================
-- STORED PROCEDURE: Crear Preferencias Predeterminadas (CORREGIDO)
-- ============================================================================

DROP PROCEDURE IF EXISTS sp_crear_preferencias_usuario$$

CREATE PROCEDURE sp_crear_preferencias_usuario(
    IN p_id_usuario INT
)
BEGIN
    DECLARE v_email_disponible BOOLEAN;
    DECLARE v_telefono_disponible BOOLEAN;
    
    -- Verificar si el usuario tiene email
    SELECT 
        (email IS NOT NULL AND email != '') INTO v_email_disponible
    FROM usuario 
    WHERE id_usuario = p_id_usuario;
    
    -- Verificar si el usuario tiene teléfono
    SELECT 
        (telefono IS NOT NULL AND telefono != '') INTO v_telefono_disponible
    FROM usuario 
    WHERE id_usuario = p_id_usuario;
    
    -- ========================================================================
    -- 1. PREFERENCIA GLOBAL (aplica para todos los tipos y canales)
    -- ========================================================================
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
        p_id_usuario,
        NULL,                   -- NULL = aplica para todos los tipos
        NULL,                   -- NULL = aplica para todos los canales
        true,                   -- Activa por defecto
        false,                  -- No desactivadas globalmente
        'INMEDIATA',           -- Frecuencia inmediata
        false,                 -- No solo horario laboral
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );
    
    -- ========================================================================
    -- 2. FACTURA_VENCIDA - Notificaciones WEB (siempre disponible)
    -- ========================================================================
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
        p_id_usuario,
        'FACTURA_VENCIDA',
        'WEB',
        true,
        false,
        'INMEDIATA',
        false,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );
    
    -- ========================================================================
    -- 3. FACTURA_VENCIDA - Notificaciones EMAIL (solo si tiene email)
    -- ========================================================================
    IF v_email_disponible THEN
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
            p_id_usuario,
            'FACTURA_VENCIDA',
            'EMAIL',
            true,
            false,
            'INMEDIATA',
            false,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
    END IF;
    
    -- ========================================================================
    -- 4. FACTURA_PROXIMA_VENCER - Notificaciones WEB
    -- ========================================================================
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
        p_id_usuario,
        'FACTURA_PROXIMA_VENCER',
        'WEB',
        true,
        false,
        'INMEDIATA',
        false,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );
    
    -- ========================================================================
    -- 5. FACTURA_CREADA - Notificaciones WEB (CORREGIDO: era NUEVA_FACTURA)
    -- ========================================================================
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
        p_id_usuario,
        'FACTURA_CREADA',      -- ✅ CORREGIDO
        'WEB',
        true,
        false,
        'INMEDIATA',
        false,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );
    
    -- ========================================================================
    -- 6. STOCK_BAJO - Notificaciones WEB
    -- ========================================================================
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
        p_id_usuario,
        'STOCK_BAJO',
        'WEB',
        true,
        false,
        'INMEDIATA',
        false,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );
    
    -- ========================================================================
    -- 7. PAGO_RECIBIDO - Notificaciones WEB
    -- ========================================================================
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
        p_id_usuario,
        'PAGO_RECIBIDO',
        'WEB',
        true,
        false,
        'INMEDIATA',
        false,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );
    
END$$

-- ============================================================================
-- TRIGGER: Crear preferencias después de insertar usuario (sin cambios)
-- ============================================================================

DROP TRIGGER IF EXISTS trg_after_insert_usuario$$

CREATE TRIGGER trg_after_insert_usuario
AFTER INSERT ON usuario
FOR EACH ROW
BEGIN
    -- Solo crear preferencias si el usuario está activo
    IF NEW.activo = true THEN
        -- Llamar al procedimiento almacenado
        CALL sp_crear_preferencias_usuario(NEW.id_usuario);
    END IF;
END$$

DELIMITER ;

-- ============================================================================
-- VERIFICACIÓN
-- ============================================================================

-- Ver el stored procedure actualizado
SHOW PROCEDURE STATUS WHERE Db = 'facturas_monrachem' AND Name = 'sp_crear_preferencias_usuario';

-- Ver el trigger actualizado
SHOW TRIGGERS LIKE 'usuario';

-- ============================================================================
-- PRUEBA (OPCIONAL)
-- ============================================================================

/*
-- Crear un usuario de prueba para verificar que funciona:
INSERT INTO usuario (
    nombre, 
    telefono, 
    email, 
    password, 
    rol, 
    activo
) VALUES (
    'Usuario Prueba',
    '5555555555',
    'prueba@test.com',
    'password_hash',
    'USER',
    true
);

-- Verificar que se crearon las preferencias correctamente:
SELECT 
    pn.id_preferencia,
    u.nombre AS usuario,
    pn.tipo_notificacion,
    pn.canal,
    pn.activa
FROM preferencia_notificacion pn
INNER JOIN usuario u ON pn.id_usuario = u.id_usuario
WHERE u.telefono = '5555555555'
ORDER BY pn.id_preferencia;

-- Limpiar la prueba:
-- DELETE FROM usuario WHERE telefono = '5555555555';
-- Las preferencias se eliminan automáticamente por CASCADE
*/

-- ============================================================================
-- NOTAS IMPORTANTES
-- ============================================================================

/*
1. CAMBIOS REALIZADOS:
   ✅ 'NUEVA_FACTURA' → 'FACTURA_CREADA' (línea 144)
   
2. VALORES VÁLIDOS DEL ENUM TipoNotificacion (Java):
   - FACTURA_CREADA
   - FACTURA_VENCIDA
   - FACTURA_PROXIMA_VENCER
   - PAGO_RECIBIDO
   - STOCK_BAJO
   - NUEVO_CLIENTE
   - NUEVO_USUARIO
   - MENSAJE_WHATSAPP
   - SISTEMA

3. PREFERENCIAS QUE SE CREAN POR DEFECTO:
   - 1 GLOBAL (NULL, NULL) - Aplica para todos los tipos y canales
   - FACTURA_VENCIDA - WEB (todos los usuarios)
   - FACTURA_VENCIDA - EMAIL (solo si tiene email)
   - FACTURA_PROXIMA_VENCER - WEB
   - FACTURA_CREADA - WEB (CORREGIDO)
   - STOCK_BAJO - WEB
   - PAGO_RECIBIDO - WEB

4. CANALES DISPONIBLES:
   - WEB: Siempre disponible
   - EMAIL: Solo si el usuario tiene email configurado
   - WHATSAPP: Se puede agregar después si es necesario

5. TRIGGER:
   - Se ejecuta automáticamente al insertar un nuevo usuario
   - Solo si el usuario está activo (activo = true)
   - Llama al stored procedure sp_crear_preferencias_usuario

6. IMPACTO:
   - Los nuevos usuarios se crearán correctamente
   - Los usuarios existentes no se ven afectados
   - Si hay usuarios sin preferencias, ejecutar INIT_PREFERENCIAS_NOTIFICACION.sql
*/

-- ============================================================================
-- RESULTADO ESPERADO
-- ============================================================================
/*
✅ Stored procedure corregido con tipo_notificacion válido
✅ Trigger actualizado y funcional
✅ Nuevos usuarios se crean sin errores de truncamiento
✅ Preferencias de notificación se crean automáticamente
*/
