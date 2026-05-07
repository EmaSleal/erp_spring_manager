-- ============================================================
-- SCRIPT MAESTRO: EJECUTAR TODOS LOS CATÁLOGOS DE UBICACIONES
-- ============================================================
-- Ejecuta todos los scripts en el orden correcto

-- 1. Crear tablas
SOURCE CREATE_TABLAS_UBICACIONES_CR.sql;

-- 2. Insertar provincias
SOURCE INSERT_PROVINCIAS_CR.sql;

-- 3. Insertar cantones
SOURCE INSERT_CANTONES_CR.sql;

-- 4. Insertar distritos
SOURCE INSERT_DISTRITOS_CR.sql;

-- 5. Insertar barrios (si existe)
-- SOURCE INSERT_BARRIOS_CR.sql;

-- Verificación final
SELECT 
    (SELECT COUNT(*) FROM cat_provincia_cr) AS total_provincias,
    (SELECT COUNT(*) FROM cat_canton_cr) AS total_cantones,
    (SELECT COUNT(*) FROM cat_distrito_cr) AS total_distritos,
    (SELECT COUNT(*) FROM cat_barrio_cr) AS total_barrios;

SELECT 'Catálogos de ubicaciones CR cargados exitosamente' AS status;
