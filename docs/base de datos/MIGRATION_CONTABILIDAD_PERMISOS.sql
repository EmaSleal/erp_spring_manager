-- =====================================================
-- MIGRACIÓN DE PERMISOS DE CONTABILIDAD
-- Sistema: WhatsApp Orders Manager
-- Fecha: 17 de enero de 2026
-- Descripción: Permisos para el módulo de contabilidad (Sprint 5 - Fase 2)
-- =====================================================

-- IMPORTANTE: Ejecutar este script DESPUÉS de MIGRATION_PERMISOS_DINAMICOS.sql
-- Este script agrega 7 nuevos permisos al sistema

-- =====================================================
-- RESUMEN DE PERMISOS DE CONTABILIDAD
-- =====================================================
-- Total de Permisos Nuevos: 7
-- Categoría: Contabilidad
--
-- Permisos:
-- 1. CONTABILIDAD_VER         - Ver plan de cuentas y asientos
-- 2. CONTABILIDAD_CREAR       - Crear cuentas y asientos
-- 3. CONTABILIDAD_EDITAR      - Editar cuentas y asientos
-- 4. CONTABILIDAD_ELIMINAR    - Eliminar cuentas y asientos (CRÍTICO)
-- 5. CONTABILIDAD_CONTABILIZAR - Contabilizar asientos (cambiar de borrador a contabilizado)
-- 6. CONTABILIDAD_ANULAR      - Anular asientos contabilizados (CRÍTICO)
-- 7. CONTABILIDAD_REPORTES    - Ver reportes financieros (Balance, Estado de Resultados, etc.)
-- =====================================================

-- =====================================================
-- PASO 1: INSERTAR LOS 7 PERMISOS DE CONTABILIDAD
-- =====================================================

-- CATEGORÍA: CONTABILIDAD (7 permisos)
INSERT INTO permiso (codigo, nombre, descripcion, categoria, es_critico, activo) VALUES
('CONTABILIDAD_VER', 'Ver contabilidad', 'Visualizar plan de cuentas y asientos contables', 'Contabilidad', FALSE, TRUE),
('CONTABILIDAD_CREAR', 'Crear registros contables', 'Crear cuentas contables y asientos en borrador', 'Contabilidad', FALSE, TRUE),
('CONTABILIDAD_EDITAR', 'Editar contabilidad', 'Modificar cuentas y asientos en borrador', 'Contabilidad', FALSE, TRUE),
('CONTABILIDAD_ELIMINAR', 'Eliminar registros contables', 'Eliminar cuentas sin movimientos y asientos en borrador', 'Contabilidad', TRUE, TRUE),
('CONTABILIDAD_CONTABILIZAR', 'Contabilizar asientos', 'Cambiar asientos de borrador a contabilizado (afecta saldos)', 'Contabilidad', TRUE, TRUE),
('CONTABILIDAD_ANULAR', 'Anular asientos', 'Anular asientos contabilizados (operación crítica)', 'Contabilidad', TRUE, TRUE),
('CONTABILIDAD_REPORTES', 'Reportes contables', 'Ver reportes financieros: Balance, Estado de Resultados, Libro Mayor', 'Contabilidad', FALSE, TRUE);

-- =====================================================
-- PASO 2: ASIGNAR PERMISOS AL ROL ADMIN
-- =====================================================

-- El rol ADMIN tiene TODOS los permisos, incluyendo los nuevos
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'ADMIN'
AND p.codigo IN (
    'CONTABILIDAD_VER',
    'CONTABILIDAD_CREAR',
    'CONTABILIDAD_EDITAR',
    'CONTABILIDAD_ELIMINAR',
    'CONTABILIDAD_CONTABILIZAR',
    'CONTABILIDAD_ANULAR',
    'CONTABILIDAD_REPORTES'
);

-- =====================================================
-- PASO 3: ASIGNAR PERMISOS AL ROL GERENTE
-- =====================================================

-- El rol GERENTE tiene acceso completo a contabilidad EXCEPTO anular
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'GERENTE'
AND p.codigo IN (
    'CONTABILIDAD_VER',
    'CONTABILIDAD_CREAR',
    'CONTABILIDAD_EDITAR',
    'CONTABILIDAD_CONTABILIZAR',
    'CONTABILIDAD_REPORTES'
);

-- =====================================================
-- PASO 4: ASIGNAR PERMISOS AL ROL VENDEDOR
-- =====================================================

-- El rol VENDEDOR solo puede VER contabilidad y reportes básicos
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'VENDEDOR'
AND p.codigo IN (
    'CONTABILIDAD_VER',
    'CONTABILIDAD_REPORTES'
);

-- =====================================================
-- PASO 5: ASIGNAR PERMISOS AL ROL VISUALIZADOR
-- =====================================================

-- El rol VISUALIZADOR solo puede VER contabilidad y reportes
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'VISUALIZADOR'
AND p.codigo IN (
    'CONTABILIDAD_VER',
    'CONTABILIDAD_REPORTES'
);

-- =====================================================
-- PASO 6: VERIFICACIÓN
-- =====================================================

-- Verificar que los permisos se insertaron correctamente
SELECT * FROM permiso WHERE categoria = 'Contabilidad' ORDER BY codigo;

-- Resultado esperado: 7 permisos de contabilidad

-- Verificar permisos críticos de contabilidad
SELECT codigo, nombre, es_critico
FROM permiso
WHERE categoria = 'Contabilidad' AND es_critico = TRUE;

-- Resultado esperado:
-- CONTABILIDAD_ELIMINAR
-- CONTABILIDAD_CONTABILIZAR
-- CONTABILIDAD_ANULAR

-- Verificar asignación de permisos por rol
SELECT 
    r.nombre as rol,
    COUNT(CASE WHEN p.categoria = 'Contabilidad' THEN 1 END) as permisos_contabilidad
FROM rol r
LEFT JOIN rol_permiso rp ON r.id_rol = rp.id_rol
LEFT JOIN permiso p ON rp.id_permiso = p.id_permiso
GROUP BY r.nombre
ORDER BY permisos_contabilidad DESC;

-- Resultado esperado:
-- ADMIN: 7 permisos
-- GERENTE: 5 permisos
-- VENDEDOR: 2 permisos
-- VISUALIZADOR: 2 permisos
-- USER: 0 permisos
-- CLIENTE: 0 permisos

-- Verificar total de permisos por rol (después de agregar contabilidad)
SELECT r.nombre, COUNT(rp.id_permiso) as total_permisos
FROM rol r
LEFT JOIN rol_permiso rp ON r.id_rol = rp.id_rol
GROUP BY r.nombre
ORDER BY total_permisos DESC;

-- Resultado esperado:
-- ADMIN: 55 permisos (48 + 7)
-- GERENTE: 35 permisos (30 + 5)
-- VENDEDOR: 17 permisos (15 + 2)
-- USER: 10 permisos (sin cambios)
-- VISUALIZADOR: 10 permisos (8 + 2)
-- CLIENTE: 3 permisos (sin cambios)

-- =====================================================
-- FIN DEL SCRIPT
-- =====================================================

-- NOTAS IMPORTANTES:
-- 1. Este script debe ejecutarse DESPUÉS de MIGRATION_PERMISOS_DINAMICOS.sql
-- 2. Los permisos de contabilidad permiten:
--    - Gestión completa del plan de cuentas contables jerárquico
--    - Creación y edición de asientos contables en borrador
--    - Contabilización de asientos (registra movimientos definitivos)
--    - Anulación de asientos contabilizados (solo ADMIN)
--    - Generación de reportes financieros (Balance General, Estado de Resultados, Libro Mayor)
-- 3. PERMISOS CRÍTICOS:
--    - CONTABILIDAD_ELIMINAR: Puede afectar la integridad del plan de cuentas
--    - CONTABILIDAD_CONTABILIZAR: Registra movimientos que afectan los saldos
--    - CONTABILIDAD_ANULAR: Puede alterar el historial contable
-- 4. El sistema sigue el principio de partida doble:
--    - Cada asiento debe estar cuadrado (debe = haber)
--    - Los asientos contabilizados no pueden modificarse, solo anularse
--    - Las cuentas con movimientos no pueden eliminarse
-- 5. Integración con otros módulos:
--    - Genera asientos automáticos desde facturas (débito: Clientes, crédito: Ventas + IVA)
--    - Genera asientos automáticos desde pagos (débito: Caja/Banco, crédito: Clientes)
--
-- DISTRIBUCIÓN RECOMENDADA:
-- - ADMIN:        Acceso total (7/7 permisos)
-- - GERENTE:      Gestión sin anulación (5/7 permisos)
-- - VENDEDOR:     Solo consulta (2/7 permisos)
-- - VISUALIZADOR: Solo consulta (2/7 permisos)
-- - USER:         Sin acceso (0/7 permisos)
-- - CLIENTE:      Sin acceso (0/7 permisos)
