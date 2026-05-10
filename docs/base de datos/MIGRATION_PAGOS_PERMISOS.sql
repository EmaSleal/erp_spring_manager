-- =====================================================
-- MIGRACIÓN DE PERMISOS DE PAGOS
-- Sistema: WhatsApp Orders Manager
-- Fecha: 18 de enero de 2026
-- Sprint: 5 - Fase 1
-- Descripción: Permisos para el módulo de pagos
-- =====================================================

-- IMPORTANTE: Ejecutar este script DESPUÉS de MIGRATION_PERMISOS_DINAMICOS.sql
-- Este script agrega 8 nuevos permisos al sistema

-- =====================================================
-- RESUMEN DE PERMISOS DE PAGOS
-- =====================================================
-- Total de Permisos Nuevos: 8
-- Categoría: Pagos
--
-- Permisos:
-- 1. PAGO_VER              - Ver listado y detalle de pagos
-- 2. PAGO_CREAR            - Registrar nuevos pagos de clientes
-- 3. PAGO_EDITAR           - Modificar pagos pendientes
-- 4. PAGO_ELIMINAR         - Eliminar pagos en borrador (CRÍTICO)
-- 5. PAGO_CONFIRMAR        - Confirmar pagos y generar asiento contable (CRÍTICO)
-- 6. PAGO_ANULAR           - Anular pagos confirmados (CRÍTICO)
-- 7. PAGO_CONCILIAR        - Marcar pagos como conciliados
-- 8. PAGO_ESTADO_CUENTA    - Ver estado de cuenta de clientes
-- =====================================================

-- =====================================================
-- PASO 1: INSERTAR LOS 8 PERMISOS DE PAGOS
-- =====================================================

-- CATEGORÍA: PAGOS (8 permisos)
INSERT INTO permiso (codigo, nombre, descripcion, categoria, es_critico, activo) VALUES
('PAGO_VER', 'Ver pagos', 'Visualizar listado y detalle de pagos', 'Pagos', FALSE, TRUE),
('PAGO_CREAR', 'Crear pagos', 'Registrar nuevos pagos de clientes', 'Pagos', FALSE, TRUE),
('PAGO_EDITAR', 'Editar pagos', 'Modificar pagos pendientes', 'Pagos', FALSE, TRUE),
('PAGO_ELIMINAR', 'Eliminar pagos', 'Eliminar pagos en borrador', 'Pagos', TRUE, TRUE),
('PAGO_CONFIRMAR', 'Confirmar pagos', 'Confirmar pagos y generar asiento contable', 'Pagos', TRUE, TRUE),
('PAGO_ANULAR', 'Anular pagos', 'Anular pagos confirmados (operación crítica)', 'Pagos', TRUE, TRUE),
('PAGO_CONCILIAR', 'Conciliar pagos', 'Marcar pagos como conciliados contra extracto bancario', 'Pagos', FALSE, TRUE),
('PAGO_ESTADO_CUENTA', 'Estado de cuenta', 'Ver estado de cuenta de clientes', 'Pagos', FALSE, TRUE);

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
    'PAGO_VER',
    'PAGO_CREAR',
    'PAGO_EDITAR',
    'PAGO_ELIMINAR',
    'PAGO_CONFIRMAR',
    'PAGO_ANULAR',
    'PAGO_CONCILIAR',
    'PAGO_ESTADO_CUENTA'
);

-- =====================================================
-- PASO 3: ASIGNAR PERMISOS AL ROL GERENTE
-- =====================================================

-- El rol GERENTE tiene acceso completo a pagos EXCEPTO eliminar y anular
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'GERENTE'
AND p.codigo IN (
    'PAGO_VER',
    'PAGO_CREAR',
    'PAGO_EDITAR',
    'PAGO_CONFIRMAR',
    'PAGO_CONCILIAR',
    'PAGO_ESTADO_CUENTA'
);

-- =====================================================
-- PASO 4: ASIGNAR PERMISOS AL ROL VENDEDOR
-- =====================================================

-- El rol VENDEDOR puede ver pagos, crear, y ver estado de cuenta
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'VENDEDOR'
AND p.codigo IN (
    'PAGO_VER',
    'PAGO_CREAR',
    'PAGO_ESTADO_CUENTA'
);

-- =====================================================
-- PASO 5: ASIGNAR PERMISOS AL ROL VISUALIZADOR
-- =====================================================

-- El rol VISUALIZADOR solo puede VER pagos y estado de cuenta
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'VISUALIZADOR'
AND p.codigo IN (
    'PAGO_VER',
    'PAGO_ESTADO_CUENTA'
);

-- =====================================================
-- PASO 6: ASIGNAR PERMISOS AL ROL CONTADOR
-- =====================================================

-- El rol CONTADOR tiene acceso a pagos para conciliación y reportes
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'CONTADOR'
AND p.codigo IN (
    'PAGO_VER',
    'PAGO_CONCILIAR',
    'PAGO_ESTADO_CUENTA'
);

-- =====================================================
-- VERIFICACIÓN DE INSTALACIÓN
-- =====================================================

-- Ver todos los permisos de pagos creados
SELECT * FROM permiso WHERE categoria = 'Pagos';

-- Verificar asignación por rol
SELECT 
    r.nombre AS rol,
    COUNT(DISTINCT p.codigo) AS total_permisos,
    GROUP_CONCAT(p.codigo ORDER BY p.codigo SEPARATOR ', ') AS permisos
FROM rol r
LEFT JOIN rol_permiso rp ON r.id_rol = rp.id_rol
LEFT JOIN permiso p ON rp.id_permiso = p.id_permiso
WHERE p.categoria = 'Pagos'
GROUP BY r.id_rol
ORDER BY r.id_rol;

-- =====================================================
-- MATRIZ DE PERMISOS POR ROL
-- =====================================================
-- +---------------+--------+---------+----------+--------------+----------+
-- | Permiso       | ADMIN  | GERENTE | VENDEDOR | VISUALIZADOR | CONTADOR |
-- +---------------+--------+---------+----------+--------------+----------+
-- | PAGO_VER      |   ✓    |    ✓    |    ✓     |      ✓       |    ✓     |
-- | PAGO_CREAR    |   ✓    |    ✓    |    ✓     |      ✗       |    ✗     |
-- | PAGO_EDITAR   |   ✓    |    ✓    |    ✗     |      ✗       |    ✗     |
-- | PAGO_ELIMINAR |   ✓    |    ✗    |    ✗     |      ✗       |    ✗     |
-- | PAGO_CONFIRMAR|   ✓    |    ✓    |    ✗     |      ✗       |    ✗     |
-- | PAGO_ANULAR   |   ✓    |    ✗    |    ✗     |      ✗       |    ✗     |
-- | PAGO_CONCILIAR|   ✓    |    ✓    |    ✗     |      ✗       |    ✓     |
-- | PAGO_ESTADO_  |   ✓    |    ✓    |    ✓     |      ✓       |    ✓     |
-- |   CUENTA      |        |         |          |              |          |
-- +---------------+--------+---------+----------+--------------+----------+
-- =====================================================

-- =====================================================
-- NOTAS IMPORTANTES
-- =====================================================
-- 1. PAGO_VER: Permiso base para acceder al módulo de pagos
-- 2. PAGO_CREAR: Solo para usuarios que registran pagos de clientes
-- 3. PAGO_EDITAR: Solo para pagos en estado PENDIENTE
-- 4. PAGO_ELIMINAR: CRÍTICO - Solo ADMIN puede eliminar pagos
-- 5. PAGO_CONFIRMAR: CRÍTICO - Genera asiento contable automático
-- 6. PAGO_ANULAR: CRÍTICO - Revierte asiento contable, solo ADMIN
-- 7. PAGO_CONCILIAR: Para contadores que validan contra extractos bancarios
-- 8. PAGO_ESTADO_CUENTA: Para ver historial de pagos del cliente
-- =====================================================

-- FIN DE LA MIGRACIÓN
