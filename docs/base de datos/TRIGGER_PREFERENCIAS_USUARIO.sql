-- ============================================================================
-- TRIGGER: CREAR PREFERENCIAS AUTOMÁTICAMENTE AL CREAR USUARIO
-- WhatsApp Orders Manager
-- ============================================================================
-- Este script crea un trigger que automáticamente genera preferencias de
-- notificación predeterminadas cada vez que se inserta un nuevo usuario.
--
-- Beneficios:
-- 1. Automatización completa - No necesitas ejecutar scripts manualmente
-- 2. Consistencia - Todos los usuarios tendrán preferencias desde el inicio
-- 3. Mantenibilidad - La lógica está centralizada en un stored procedure
-- ============================================================================

DELIMITER $$

-- ============================================================================
-- STORED PROCEDURE: Crear Preferencias Predeterminadas
-- ============================================================================
-- Este procedimiento contiene la lógica para crear las preferencias de un usuario.
-- Puede ser llamado desde el trigger o manualmente.
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
    -- 4. FACTURA_VENCIDA - Notificaciones WHATSAPP (solo si tiene teléfono)
    -- ========================================================================
    IF v_telefono_disponible THEN
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
            'WHATSAPP',
            true,
            false,
            'INMEDIATA',
            false,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
    END IF;
    
    -- ========================================================================
    -- 5. STOCK_BAJO - Notificaciones WEB
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
    -- 6. STOCK_BAJO - Notificaciones EMAIL (solo si tiene email, DIARIA)
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
            'STOCK_BAJO',
            'EMAIL',
            true,
            false,
            'DIARIA',              -- Resumen diario para evitar spam
            false,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
    END IF;
    
    -- ========================================================================
    -- 7. NUEVA_FACTURA - Notificaciones WEB (para todos)
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
        'NUEVA_FACTURA',
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
-- TRIGGER: Crear preferencias después de insertar usuario
-- ============================================================================
-- Se ejecuta automáticamente después de cada INSERT en la tabla usuario
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

-- ============================================================================
-- TRIGGER: Actualizar preferencias cuando se actualiza email/teléfono
-- ============================================================================
-- Opcional: Si un usuario agrega email o teléfono después, se crean las
-- preferencias para esos canales si no existen
-- ============================================================================

DROP TRIGGER IF EXISTS trg_after_update_usuario$$

CREATE TRIGGER trg_after_update_usuario
AFTER UPDATE ON usuario
FOR EACH ROW
BEGIN
    DECLARE v_tiene_email_nuevo BOOLEAN;
    DECLARE v_tiene_telefono_nuevo BOOLEAN;
    DECLARE v_tiene_email_viejo BOOLEAN;
    DECLARE v_tiene_telefono_viejo BOOLEAN;
    
    -- Verificar estado de email
    SET v_tiene_email_nuevo = (NEW.email IS NOT NULL AND NEW.email != '');
    SET v_tiene_email_viejo = (OLD.email IS NOT NULL AND OLD.email != '');
    
    -- Verificar estado de teléfono
    SET v_tiene_telefono_nuevo = (NEW.telefono IS NOT NULL AND NEW.telefono != '');
    SET v_tiene_telefono_viejo = (OLD.telefono IS NOT NULL AND OLD.telefono != '');
    
    -- ========================================================================
    -- Si se agregó EMAIL por primera vez
    -- ========================================================================
    IF v_tiene_email_nuevo AND NOT v_tiene_email_viejo THEN
        
        -- FACTURA_VENCIDA - EMAIL
        INSERT IGNORE INTO preferencia_notificacion (
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
            NEW.id_usuario,
            'FACTURA_VENCIDA',
            'EMAIL',
            true,
            false,
            'INMEDIATA',
            false,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
        
        -- STOCK_BAJO - EMAIL
        INSERT IGNORE INTO preferencia_notificacion (
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
            NEW.id_usuario,
            'STOCK_BAJO',
            'EMAIL',
            true,
            false,
            'DIARIA',
            false,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
        
    END IF;
    
    -- ========================================================================
    -- Si se agregó TELÉFONO por primera vez
    -- ========================================================================
    IF v_tiene_telefono_nuevo AND NOT v_tiene_telefono_viejo THEN
        
        -- FACTURA_VENCIDA - WHATSAPP
        INSERT IGNORE INTO preferencia_notificacion (
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
            NEW.id_usuario,
            'FACTURA_VENCIDA',
            'WHATSAPP',
            true,
            false,
            'INMEDIATA',
            false,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
        
    END IF;
    
END$$

DELIMITER ;

-- ============================================================================
-- VERIFICACIÓN Y PRUEBAS
-- ============================================================================

-- Ver los triggers creados
SHOW TRIGGERS LIKE 'usuario';

-- Ver el procedimiento almacenado
SHOW PROCEDURE STATUS WHERE Db = DATABASE() AND Name = 'sp_crear_preferencias_usuario';

-- ============================================================================
-- PRUEBA MANUAL: Crear un usuario de prueba
-- ============================================================================
/*
-- Insertar usuario de prueba
INSERT INTO usuario (
    nombre,
    telefono,
    email,
    password,
    rol,
    activo,
    bloqueado,
    require_cambio_password,
    create_date,
    update_date
) VALUES (
    'Usuario Prueba Trigger',
    '12345678',
    'prueba.trigger@example.com',
    '$2a$10$abcdefghijklmnopqrstuv',  -- Password hasheado
    'VENDEDOR',
    true,
    false,
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Verificar que se crearon las preferencias automáticamente
SELECT 
    u.nombre,
    pn.tipo_notificacion,
    pn.canal,
    pn.activa,
    pn.frecuencia,
    pn.create_date
FROM usuario u
INNER JOIN preferencia_notificacion pn ON u.id_usuario = pn.id_usuario
WHERE u.nombre = 'Usuario Prueba Trigger'
ORDER BY pn.tipo_notificacion, pn.canal;

-- Eliminar usuario de prueba (opcional)
-- DELETE FROM usuario WHERE nombre = 'Usuario Prueba Trigger';
*/

-- ============================================================================
-- LLAMADA MANUAL AL PROCEDIMIENTO (para usuarios existentes)
-- ============================================================================
/*
-- Crear preferencias para un usuario específico que ya existe
CALL sp_crear_preferencias_usuario(1);  -- Reemplazar 1 con el ID del usuario

-- Crear preferencias para todos los usuarios sin preferencias
DELIMITER $$
CREATE PROCEDURE sp_inicializar_preferencias_todos()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_id_usuario INT;
    DECLARE cur CURSOR FOR 
        SELECT u.id_usuario 
        FROM usuario u
        WHERE u.activo = true
          AND NOT EXISTS (
              SELECT 1 
              FROM preferencia_notificacion pn 
              WHERE pn.id_usuario = u.id_usuario
          );
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO v_id_usuario;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        CALL sp_crear_preferencias_usuario(v_id_usuario);
    END LOOP;
    
    CLOSE cur;
END$$
DELIMITER ;

-- Ejecutar para todos los usuarios sin preferencias
-- CALL sp_inicializar_preferencias_todos();
*/

-- ============================================================================
-- ESTADÍSTICAS Y VERIFICACIÓN
-- ============================================================================

-- Contar preferencias por usuario
SELECT 
    u.id_usuario,
    u.nombre,
    u.email IS NOT NULL AS tiene_email,
    u.telefono IS NOT NULL AS tiene_telefono,
    COUNT(pn.id_preferencia) AS total_preferencias,
    SUM(CASE WHEN pn.activa = true THEN 1 ELSE 0 END) AS preferencias_activas
FROM usuario u
LEFT JOIN preferencia_notificacion pn ON u.id_usuario = pn.id_usuario
WHERE u.activo = true
GROUP BY u.id_usuario, u.nombre, u.email, u.telefono
ORDER BY u.id_usuario;

-- Ver detalle de preferencias por tipo y canal
SELECT 
    tipo_notificacion,
    canal,
    COUNT(*) AS total_usuarios,
    SUM(CASE WHEN activa = true THEN 1 ELSE 0 END) AS usuarios_activos
FROM preferencia_notificacion
GROUP BY tipo_notificacion, canal
ORDER BY tipo_notificacion, canal;

-- ============================================================================
-- DESACTIVAR TRIGGERS (solo si es necesario)
-- ============================================================================
/*
-- Desactivar trigger de INSERT
DROP TRIGGER IF EXISTS trg_after_insert_usuario;

-- Desactivar trigger de UPDATE
DROP TRIGGER IF EXISTS trg_after_update_usuario;

-- Eliminar procedimiento almacenado
DROP PROCEDURE IF EXISTS sp_crear_preferencias_usuario;
*/

-- ============================================================================
-- NOTAS IMPORTANTES
-- ============================================================================
/*
1. El trigger se ejecuta AUTOMÁTICAMENTE cada vez que se inserta un usuario
2. El trigger de UPDATE se ejecuta cuando se agrega email o teléfono
3. Las preferencias se crean solo si el usuario está activo (activo = true)
4. Se crean entre 3 y 7 preferencias por usuario dependiendo de:
   - Email disponible: +2 preferencias (EMAIL para FACTURA_VENCIDA y STOCK_BAJO)
   - Teléfono disponible: +1 preferencia (WHATSAPP para FACTURA_VENCIDA)
5. El procedimiento almacenado puede llamarse manualmente para usuarios existentes
6. Se usa INSERT IGNORE en el trigger de UPDATE para evitar duplicados

CANALES POR CONDICIÓN:
- WEB: Siempre disponible (3 preferencias: GLOBAL, FACTURA_VENCIDA, STOCK_BAJO, NUEVA_FACTURA)
- EMAIL: Solo si usuario tiene email (2 preferencias adicionales)
- WHATSAPP: Solo si usuario tiene teléfono (1 preferencia adicional)

TIPOS DE NOTIFICACIÓN PREDETERMINADOS:
- NULL (GLOBAL): Configuración base para todo
- FACTURA_VENCIDA: WEB, EMAIL (si disponible), WHATSAPP (si disponible)
- STOCK_BAJO: WEB, EMAIL con frecuencia DIARIA (si disponible)
- NUEVA_FACTURA: WEB
*/
