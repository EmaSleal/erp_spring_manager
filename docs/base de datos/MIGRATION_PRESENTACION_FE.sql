-- ================================================
-- MIGRACIÓN: Agregar código de unidad FE a presentaciones
-- Facturación Electrónica Costa Rica v4.4
-- Fecha: 2026-02-21
-- ================================================

-- 1. Agregar columna a la tabla presentacion
ALTER TABLE presentacion 
ADD COLUMN codigo_unidad_fe VARCHAR(10) NULL COMMENT 'Código de unidad de medida para Facturación Electrónica Costa Rica';

-- 2. Actualizar presentaciones existentes con códigos de Hacienda
UPDATE presentacion SET codigo_unidad_fe = 'Unid' WHERE id_presentacion = 1;  -- Ud -> Unidad
UPDATE presentacion SET codigo_unidad_fe = 'l' WHERE id_presentacion = 2;     -- 1/2 litro -> Litro
UPDATE presentacion SET codigo_unidad_fe = 'Galon' WHERE id_presentacion = 3; -- Galón
UPDATE presentacion SET codigo_unidad_fe = 'l' WHERE id_presentacion = 4;     -- Litro
UPDATE presentacion SET codigo_unidad_fe = 'Galon' WHERE id_presentacion = 5; -- 1/2 galón -> Galón
UPDATE presentacion SET codigo_unidad_fe = 'kg' WHERE id_presentacion = 6;    -- 200 g -> Kilogramo
UPDATE presentacion SET codigo_unidad_fe = 'kg' WHERE id_presentacion = 7;    -- 50 g -> Kilogramo
UPDATE presentacion SET codigo_unidad_fe = 'kg' WHERE id_presentacion = 8;    -- 1 kg -> Kilogramo
UPDATE presentacion SET codigo_unidad_fe = 'l' WHERE id_presentacion = 9;     -- Pichinga -> Litro
UPDATE presentacion SET codigo_unidad_fe = 'l' WHERE id_presentacion = 10;    -- 250 ml -> Litro
UPDATE presentacion SET codigo_unidad_fe = 'kg' WHERE id_presentacion = 11;   -- 10 kg -> Kilogramo
UPDATE presentacion SET codigo_unidad_fe = 'kg' WHERE id_presentacion = 12;   -- 2,5 kg -> Kilogramo
UPDATE presentacion SET codigo_unidad_fe = 'kg' WHERE id_presentacion = 13;   -- 20 kg -> Kilogramo
UPDATE presentacion SET codigo_unidad_fe = 'kg' WHERE id_presentacion = 14;   -- 5 kg -> Kilogramo
UPDATE presentacion SET codigo_unidad_fe = 'l' WHERE id_presentacion = 15;    -- 100 ml -> Litro
UPDATE presentacion SET codigo_unidad_fe = 'l' WHERE id_presentacion = 16;    -- 500 ml -> Litro
UPDATE presentacion SET codigo_unidad_fe = 'Unid' WHERE id_presentacion = 17; -- Amarillo -> Unidad
UPDATE presentacion SET codigo_unidad_fe = 'Unid' WHERE id_presentacion = 18; -- Blanco -> Unidad
UPDATE presentacion SET codigo_unidad_fe = 'Unid' WHERE id_presentacion = 19; -- Rosado -> Unidad
UPDATE presentacion SET codigo_unidad_fe = 'l' WHERE id_presentacion = 20;    -- Tambor 208l -> Litro
UPDATE presentacion SET codigo_unidad_fe = 'Unid' WHERE id_presentacion = 21; -- Eco -> Unidad
UPDATE presentacion SET codigo_unidad_fe = 'Unid' WHERE id_presentacion = 22; -- Botella -> Unidad

-- 3. Insertar nuevas presentaciones con códigos de FE que no existen
-- Servicios Profesionales
INSERT INTO presentacion (nombre, codigo_unidad_fe) VALUES ('Servicios Profesionales', 'Sp');

-- Unidades de medida del Sistema Internacional (SI) y derivadas
INSERT INTO presentacion (nombre, codigo_unidad_fe) VALUES ('Metro', 'm');
INSERT INTO presentacion (nombre, codigo_unidad_fe) VALUES ('Segundo', 's');
INSERT INTO presentacion (nombre, codigo_unidad_fe) VALUES ('Amperio', 'A');
INSERT INTO presentacion (nombre, codigo_unidad_fe) VALUES ('Kelvin', 'K');
INSERT INTO presentacion (nombre, codigo_unidad_fe) VALUES ('Mol', 'mol');
INSERT INTO presentacion (nombre, codigo_unidad_fe) VALUES ('Candela', 'cd');

-- Unidades derivadas
INSERT INTO presentacion (nombre, codigo_unidad_fe) VALUES ('Metro cuadrado', 'm²');
INSERT INTO presentacion (nombre, codigo_unidad_fe) VALUES ('Metro cúbico', 'm³');
INSERT INTO presentacion (nombre, codigo_unidad_fe) VALUES ('Metro por segundo', 'm/s');
INSERT INTO presentacion (nombre, codigo_unidad_fe) VALUES ('Metro por segundo cuadrado', 'm/s²');
INSERT INTO presentacion (nombre, codigo_unidad_fe) VALUES ('Uno por metro', '1/m');
INSERT INTO presentacion (nombre, codigo_unidad_fe) VALUES ('Kilogramo por metro cúbico', 'kg/m³');
INSERT INTO presentacion (nombre, codigo_unidad_fe) VALUES ('Metro cúbico por segundo', 'm³/s');

-- Otros
INSERT INTO presentacion (nombre, codigo_unidad_fe) VALUES ('Otros', 'Otros');

-- 4. Verificar resultados
SELECT 
    id_presentacion,
    nombre,
    codigo_unidad_fe,
    CASE 
        WHEN codigo_unidad_fe IS NULL THEN '❌ Sin código FE'
        ELSE '✅ OK'
    END AS estado
FROM presentacion
ORDER BY id_presentacion;

-- ================================================
-- NOTAS:
-- - Los códigos de unidad FE deben coincidir con el catálogo de Hacienda
-- - Unid: Unidad genérica
-- - Sp: Servicios Profesionales
-- - l: Litro
-- - kg: Kilogramo
-- - Galon: Galón (aceptado en Costa Rica)
-- - m: Metro
-- - Otros: Para casos especiales
-- ================================================
