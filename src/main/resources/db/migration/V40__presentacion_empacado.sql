-- ============================================================================
-- V40: presentacion.empacado y codigos de unidad validos (ADR-041, T-034)
-- ============================================================================
-- Esta migracion:
--   1) agrega presentacion.empacado BIT(1) NOT NULL DEFAULT 1 (solo si no existe);
--   2) fija empacado = 0 en las presentaciones que se venden en la magnitud
--      exacta (ids 1, 3, 4, 8, 23 y 24..36) y empacado = 1 en el resto;
--   3) reescribe codigo_unidad_fe a valores del enum UnidadMedidaType del XSD
--      v4.4: Galon->Gal, l->L, m->M, mol->Mol, cd->Cd; los valores guardados con
--      codificacion doble (m con 2 o 3 supraindice leidos como CP437) pasan
--      a m2, m3, m/s2, kg/m3 con el supraindice correcto; m3/s no existe en el enum y queda 'Otros';
--   4) corrige el nombre de las filas 31, 35 y 36 (mojibake de la u con tilde).
-- El archivo es ASCII puro: los caracteres no ASCII se escriben con
-- CONVERT(UNHEX(..) USING utf8mb4) para que el resultado no dependa de la
-- codificacion de la conexion. Se puede reejecutar sin efecto adicional.
-- ============================================================================

-- 1) Columna (guardada: reejecutable)
SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'presentacion' AND COLUMN_NAME = 'empacado');
SET @ddl := IF(@existe = 0,
               'ALTER TABLE presentacion ADD COLUMN empacado BIT(1) NOT NULL DEFAULT b''1''',
               'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) Clasificacion por id
UPDATE presentacion SET empacado = b'0'
 WHERE id_presentacion IN (1, 3, 4, 8, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36);
UPDATE presentacion SET empacado = b'1'
 WHERE id_presentacion NOT IN (1, 3, 4, 8, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36);

-- 3) Codigos de unidad: mojibake por id (supraindice 2 = C2B2, supraindice 3 = C2B3)
UPDATE presentacion SET codigo_unidad_fe = CONCAT('m', CONVERT(UNHEX('C2B2') USING utf8mb4))
 WHERE id_presentacion = 30;
UPDATE presentacion SET codigo_unidad_fe = CONCAT('m', CONVERT(UNHEX('C2B3') USING utf8mb4))
 WHERE id_presentacion = 31;
UPDATE presentacion SET codigo_unidad_fe = CONCAT('m/s', CONVERT(UNHEX('C2B2') USING utf8mb4))
 WHERE id_presentacion = 33;
UPDATE presentacion SET codigo_unidad_fe = CONCAT('kg/m', CONVERT(UNHEX('C2B3') USING utf8mb4))
 WHERE id_presentacion = 35;
UPDATE presentacion SET codigo_unidad_fe = 'Otros'
 WHERE id_presentacion = 36;

-- 3) Codigos de unidad: valores que no estan en el enum (comparacion exacta, sensible a mayusculas)
UPDATE presentacion SET codigo_unidad_fe = 'Gal' WHERE BINARY codigo_unidad_fe = BINARY 'Galon';
UPDATE presentacion SET codigo_unidad_fe = 'L'   WHERE BINARY codigo_unidad_fe = BINARY 'l';
UPDATE presentacion SET codigo_unidad_fe = 'M'   WHERE BINARY codigo_unidad_fe = BINARY 'm';
UPDATE presentacion SET codigo_unidad_fe = 'Mol' WHERE BINARY codigo_unidad_fe = BINARY 'mol';
UPDATE presentacion SET codigo_unidad_fe = 'Cd'  WHERE BINARY codigo_unidad_fe = BINARY 'cd';

-- 4) Nombres con mojibake (u con tilde = C3BA)
UPDATE presentacion SET nombre = CONCAT('Metro c', CONVERT(UNHEX('C3BA') USING utf8mb4), 'bico')
 WHERE id_presentacion = 31;
UPDATE presentacion SET nombre = CONCAT('Kilogramo por metro c', CONVERT(UNHEX('C3BA') USING utf8mb4), 'bico')
 WHERE id_presentacion = 35;
UPDATE presentacion SET nombre = CONCAT('Metro c', CONVERT(UNHEX('C3BA') USING utf8mb4), 'bico por segundo')
 WHERE id_presentacion = 36;
