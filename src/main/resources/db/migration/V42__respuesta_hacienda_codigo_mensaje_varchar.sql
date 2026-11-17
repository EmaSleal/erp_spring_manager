-- ============================================================================
-- V42: respuesta_hacienda.codigo_mensaje como VARCHAR(50) (T-036)
-- ============================================================================
-- La columna la creo Hibernate hace tiempo como ENUM nativo de MySQL con los nombres
-- de las constantes viejas de MensajeHacienda (RECHAZADO_PARCIAL ya no existe; el
-- enum sigue ahora el Anexo v4.4: 1 aceptado, 2 aceptacion parcial, 3 rechazado).
-- La entidad la declara VARCHAR(50) NOT NULL (@Enumerated STRING). Esta migracion la
-- deja asi, solo si la tabla y la columna existen. respuesta_hacienda tiene 0 filas
-- (la aplicacion no la escribe), asi que no hay nada que remapear. Reejecutable.
-- comprobante_electronico.codigo_respuesta no se toca.
-- ============================================================================
SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'respuesta_hacienda'
                  AND COLUMN_NAME = 'codigo_mensaje');
SET @ddl := IF(@existe = 1,
               'ALTER TABLE respuesta_hacienda MODIFY COLUMN codigo_mensaje VARCHAR(50) NOT NULL',
               'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
