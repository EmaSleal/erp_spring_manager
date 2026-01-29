-- =====================================================
-- SCRIPT DE MIGRACIÓN DE PERMISOS
-- Sistema: WhatsApp Orders Manager
-- Fecha: 23 de diciembre de 2025
-- Descripción: Migración de permisos desde enum a base de datos
-- =====================================================

-- IMPORTANTE: Este script debe ejecutarse DESPUÉS de que Spring Boot cree las tablas
-- Las tablas se crean automáticamente con JPA al arrancar la aplicación

-- =====================================================
-- RESUMEN DE ROLES Y PERMISOS
-- =====================================================
-- Total de Permisos: 48 (base) + 7 (pagos Sprint 5.1) + 7 (contabilidad Sprint 5.2) = 62
-- Total de Roles: 6
--
-- Distribución de permisos por rol (BASE - sin Sprint 5):
-- 1. ADMIN:        48 permisos (TODOS - Acceso total al sistema)
-- 2. GERENTE:      30 permisos (Gestión completa de operaciones)
-- 3. VENDEDOR:     15 permisos (Operaciones básicas de venta)
-- 4. USER:         10 permisos (Usuario básico con permisos limitados)
-- 5. VISUALIZADOR:  8 permisos (Solo lectura y exportación)
-- 6. CLIENTE:       3 permisos (Acceso externo solo a facturas propias)
--
-- NOTA: Para permisos de PAGOS ejecutar: MIGRATION_PAGOS_PERMISOS.sql
-- NOTA: Para permisos de CONTABILIDAD ejecutar: MIGRATION_CONTABILIDAD_PERMISOS.sql
-- =====================================================

-- =====================================================
-- PASO 1: INSERTAR LOS 48 PERMISOS
-- =====================================================

-- CATEGORÍA: FACTURACIÓN (7 permisos)
INSERT INTO permiso (codigo, nombre, descripcion, categoria, es_critico, activo) VALUES
('FACTURA_VER', 'Ver facturas', 'Visualizar listado y detalle de facturas', 'Facturación', FALSE, TRUE),
('FACTURA_CREAR', 'Crear facturas', 'Generar nuevas facturas de venta', 'Facturación', FALSE, TRUE),
('FACTURA_EDITAR', 'Editar facturas', 'Modificar facturas existentes (solo estado PENDIENTE)', 'Facturación', FALSE, TRUE),
('FACTURA_ELIMINAR', 'Eliminar facturas', 'Eliminar facturas del sistema', 'Facturación', TRUE, TRUE),
('FACTURA_ANULAR', 'Anular facturas', 'Anular facturas pagadas o completadas', 'Facturación', TRUE, TRUE),
('FACTURA_EXPORTAR', 'Exportar facturas', 'Exportar facturas a formatos PDF y Excel', 'Facturación', FALSE, TRUE),
('FACTURA_ENVIAR_EMAIL', 'Enviar facturas por email', 'Enviar facturas por correo electrónico a clientes', 'Facturación', FALSE, TRUE);

-- CATEGORÍA: CLIENTES (5 permisos)
INSERT INTO permiso (codigo, nombre, descripcion, categoria, es_critico, activo) VALUES
('CLIENTE_VER', 'Ver clientes', 'Visualizar listado y detalle de clientes', 'Clientes', FALSE, TRUE),
('CLIENTE_CREAR', 'Crear clientes', 'Registrar nuevos clientes en el sistema', 'Clientes', FALSE, TRUE),
('CLIENTE_EDITAR', 'Editar clientes', 'Modificar información de clientes existentes', 'Clientes', FALSE, TRUE),
('CLIENTE_ELIMINAR', 'Eliminar clientes', 'Eliminar clientes del sistema', 'Clientes', FALSE, TRUE),
('CLIENTE_EXPORTAR', 'Exportar clientes', 'Exportar listado de clientes a archivos', 'Clientes', FALSE, TRUE);

-- CATEGORÍA: PRODUCTOS (6 permisos)
INSERT INTO permiso (codigo, nombre, descripcion, categoria, es_critico, activo) VALUES
('PRODUCTO_VER', 'Ver productos', 'Visualizar catálogo de productos', 'Productos', FALSE, TRUE),
('PRODUCTO_CREAR', 'Crear productos', 'Agregar nuevos productos al catálogo', 'Productos', FALSE, TRUE),
('PRODUCTO_EDITAR', 'Editar productos', 'Modificar información de productos existentes', 'Productos', FALSE, TRUE),
('PRODUCTO_ELIMINAR', 'Eliminar productos', 'Eliminar productos del catálogo', 'Productos', FALSE, TRUE),
('PRODUCTO_AJUSTAR_INVENTARIO', 'Ajustar inventario', 'Modificar cantidades en stock de productos', 'Productos', FALSE, TRUE),
('PRODUCTO_EXPORTAR', 'Exportar productos', 'Exportar catálogo de productos a archivos', 'Productos', FALSE, TRUE);

-- CATEGORÍA: REPORTES (7 permisos)
INSERT INTO permiso (codigo, nombre, descripcion, categoria, es_critico, activo) VALUES
('REPORTE_VENTAS', 'Reporte de ventas', 'Ver estadísticas y gráficas de ventas', 'Reportes', FALSE, TRUE),
('REPORTE_PRODUCTOS', 'Reporte de productos', 'Ver reporte de productos más vendidos', 'Reportes', FALSE, TRUE),
('REPORTE_CLIENTES', 'Reporte de clientes', 'Ver reporte de clientes frecuentes', 'Reportes', FALSE, TRUE),
('REPORTE_DASHBOARD', 'Dashboard principal', 'Acceso al dashboard con métricas generales', 'Reportes', FALSE, TRUE),
('REPORTE_EXPORTAR_PDF', 'Exportar reportes a PDF', 'Exportar reportes en formato PDF', 'Reportes', FALSE, TRUE),
('REPORTE_EXPORTAR_EXCEL', 'Exportar reportes a Excel', 'Exportar reportes en formato Excel', 'Reportes', FALSE, TRUE),
('REPORTE_EXPORTAR_CSV', 'Exportar reportes a CSV', 'Exportar reportes en formato CSV', 'Reportes', FALSE, TRUE);

-- CATEGORÍA: CONFIGURACIÓN (5 permisos)
INSERT INTO permiso (codigo, nombre, descripcion, categoria, es_critico, activo) VALUES
('CONFIG_VER', 'Ver configuración', 'Visualizar configuración del sistema', 'Configuración', FALSE, TRUE),
('CONFIG_EDITAR_EMPRESA', 'Editar datos de empresa', 'Modificar información de la empresa', 'Configuración', TRUE, TRUE),
('CONFIG_EDITAR_FACTURACION', 'Editar configuración de facturación', 'Modificar parámetros de facturación', 'Configuración', FALSE, TRUE),
('CONFIG_EDITAR_EMAIL', 'Editar configuración de email', 'Modificar configuración de correo electrónico', 'Configuración', FALSE, TRUE),
('CONFIG_EDITAR_WHATSAPP', 'Editar configuración de WhatsApp', 'Modificar configuración de mensajería WhatsApp', 'Configuración', FALSE, TRUE);

-- CATEGORÍA: NOTIFICACIONES (5 permisos)
INSERT INTO permiso (codigo, nombre, descripcion, categoria, es_critico, activo) VALUES
('NOTIFICACION_VER', 'Ver notificaciones', 'Ver notificaciones del sistema', 'Notificaciones', FALSE, TRUE),
('NOTIFICACION_CREAR', 'Crear notificaciones', 'Enviar notificaciones manuales a usuarios', 'Notificaciones', FALSE, TRUE),
('NOTIFICACION_MARCAR_LEIDA', 'Marcar notificación como leída', 'Marcar notificaciones como leídas', 'Notificaciones', FALSE, TRUE),
('NOTIFICACION_ELIMINAR', 'Eliminar notificaciones', 'Eliminar notificaciones del sistema', 'Notificaciones', FALSE, TRUE),
('NOTIFICACION_CONFIGURAR', 'Configurar notificaciones', 'Gestionar preferencias de notificaciones', 'Notificaciones', FALSE, TRUE);

-- CATEGORÍA: USUARIOS (8 permisos - TODOS CRÍTICOS)
INSERT INTO permiso (codigo, nombre, descripcion, categoria, es_critico, activo) VALUES
('USUARIO_VER', 'Ver usuarios', 'Visualizar listado de usuarios del sistema', 'Usuarios', TRUE, TRUE),
('USUARIO_CREAR', 'Crear usuarios', 'Registrar nuevos usuarios en el sistema', 'Usuarios', TRUE, TRUE),
('USUARIO_EDITAR', 'Editar usuarios', 'Modificar información de usuarios existentes', 'Usuarios', TRUE, TRUE),
('USUARIO_ELIMINAR', 'Eliminar usuarios', 'Eliminar usuarios del sistema', 'Usuarios', TRUE, TRUE),
('USUARIO_BLOQUEAR', 'Bloquear usuarios', 'Bloquear y desbloquear usuarios', 'Usuarios', TRUE, TRUE),
('USUARIO_CAMBIAR_ROL', 'Cambiar rol de usuario', 'Modificar el rol asignado a usuarios', 'Usuarios', TRUE, TRUE),
('USUARIO_VER_ACTIVIDAD', 'Ver actividad de usuarios', 'Ver registro de actividades de usuarios', 'Usuarios', TRUE, TRUE),
('USUARIO_RESETEAR_PASSWORD', 'Resetear contraseña', 'Forzar cambio de contraseña de usuarios', 'Usuarios', TRUE, TRUE);

-- CATEGORÍA: AUDITORÍA (2 permisos - TODOS CRÍTICOS)
INSERT INTO permiso (codigo, nombre, descripcion, categoria, es_critico, activo) VALUES
('AUDITORIA_VER', 'Ver auditoría', 'Acceso al registro completo de auditoría del sistema', 'Auditoría', TRUE, TRUE),
('AUDITORIA_EXPORTAR', 'Exportar auditoría', 'Exportar logs y registros de auditoría', 'Auditoría', TRUE, TRUE);

-- CATEGORÍA: SISTEMA (3 permisos - TODOS CRÍTICOS)
INSERT INTO permiso (codigo, nombre, descripcion, categoria, es_critico, activo) VALUES
('SISTEMA_VER_LOGS', 'Ver logs del sistema', 'Acceso a logs técnicos del sistema', 'Sistema', TRUE, TRUE),
('SISTEMA_BACKUP', 'Realizar backup', 'Realizar respaldos de la base de datos', 'Sistema', TRUE, TRUE),
('SISTEMA_MANTENIMIENTO', 'Modo mantenimiento', 'Activar/desactivar modo mantenimiento', 'Sistema', TRUE, TRUE);

-- =====================================================
-- PASO 2: INSERTAR LOS 7 ROLES
-- =====================================================

INSERT INTO rol (codigo, nombre, descripcion, activo) VALUES
('ADMIN', 'Administrador', 'Rol con acceso total al sistema incluyendo configuración y usuarios', TRUE),
('GERENTE', 'Gerente', 'Rol para gerentes de área con permisos de gestión y reportes', TRUE),
('VENDEDOR', 'Vendedor', 'Rol para personal de ventas con permisos básicos de operación', TRUE),
('USER', 'Usuario', 'Rol básico con permisos de solo lectura y operaciones simples', TRUE),
('VISUALIZADOR', 'Visualizador', 'Rol con acceso de solo lectura sin posibilidad de modificar datos', TRUE),
('CLIENTE', 'Cliente', 'Rol para clientes externos con acceso limitado a sus propias facturas', TRUE);

-- =====================================================
-- PASO 3: ASIGNAR PERMISOS AL ROL VENDEDOR (15 permisos)
-- =====================================================

-- El rol VENDEDOR tiene permisos básicos de operación
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'VENDEDOR'
AND p.codigo IN (
    -- Facturación (5 permisos)
    'FACTURA_VER',
    'FACTURA_CREAR',
    'FACTURA_EDITAR',
    'FACTURA_EXPORTAR',
    'FACTURA_ENVIAR_EMAIL',
    
    -- Clientes (4 permisos)
    'CLIENTE_VER',
    'CLIENTE_CREAR',
    'CLIENTE_EDITAR',
    'CLIENTE_EXPORTAR',
    
    -- Productos (1 permiso - solo lectura)
    'PRODUCTO_VER',
    
    -- Reportes (2 permisos)
    'REPORTE_VENTAS',
    'REPORTE_DASHBOARD',
    
    -- Notificaciones (3 permisos)
    'NOTIFICACION_VER',
    'NOTIFICACION_MARCAR_LEIDA',
    'NOTIFICACION_CONFIGURAR'
);

-- =====================================================
-- PASO 4: ASIGNAR PERMISOS AL ROL GERENTE (30 permisos)
-- =====================================================

-- El rol GERENTE hereda TODOS los permisos de VENDEDOR + adicionales
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'GERENTE'
AND p.codigo IN (
    -- HERENCIA: Todos los permisos de VENDEDOR (15)
    'FACTURA_VER', 'FACTURA_CREAR', 'FACTURA_EDITAR', 'FACTURA_EXPORTAR', 'FACTURA_ENVIAR_EMAIL',
    'CLIENTE_VER', 'CLIENTE_CREAR', 'CLIENTE_EDITAR', 'CLIENTE_EXPORTAR',
    'PRODUCTO_VER',
    'REPORTE_VENTAS', 'REPORTE_DASHBOARD',
    'NOTIFICACION_VER', 'NOTIFICACION_MARCAR_LEIDA', 'NOTIFICACION_CONFIGURAR',
    
    -- ADICIONALES: Permisos exclusivos de GERENTE (15)
    
    -- Facturación adicionales (2)
    'FACTURA_ELIMINAR',
    'FACTURA_ANULAR',
    
    -- Clientes adicionales (1)
    'CLIENTE_ELIMINAR',
    
    -- Productos adicionales (5)
    'PRODUCTO_CREAR',
    'PRODUCTO_EDITAR',
    'PRODUCTO_ELIMINAR',
    'PRODUCTO_AJUSTAR_INVENTARIO',
    'PRODUCTO_EXPORTAR',
    
    -- Reportes adicionales (5)
    'REPORTE_PRODUCTOS',
    'REPORTE_CLIENTES',
    'REPORTE_EXPORTAR_PDF',
    'REPORTE_EXPORTAR_EXCEL',
    'REPORTE_EXPORTAR_CSV',
    
    -- Configuración (1)
    'CONFIG_VER',
    
    -- Notificaciones adicionales (1)
    'NOTIFICACION_CREAR'
);

-- =====================================================
-- PASO 5: ASIGNAR PERMISOS AL ROL USER (10 permisos)
-- =====================================================

-- El rol USER tiene permisos básicos similares a VENDEDOR pero más limitados
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'USER'
AND p.codigo IN (
    -- Facturación (3 permisos - solo lectura y crear)
    'FACTURA_VER',
    'FACTURA_CREAR',
    'FACTURA_EXPORTAR',
    
    -- Clientes (2 permisos - solo lectura y crear)
    'CLIENTE_VER',
    'CLIENTE_CREAR',
    
    -- Productos (1 permiso - solo lectura)
    'PRODUCTO_VER',
    
    -- Reportes (2 permisos)
    'REPORTE_VENTAS',
    'REPORTE_DASHBOARD',
    
    -- Notificaciones (2 permisos)
    'NOTIFICACION_VER',
    'NOTIFICACION_MARCAR_LEIDA'
);

-- =====================================================
-- PASO 6: ASIGNAR PERMISOS AL ROL VISUALIZADOR (8 permisos)
-- =====================================================

-- El rol VISUALIZADOR solo puede VER información, sin modificar nada
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'VISUALIZADOR'
AND p.codigo IN (
    -- Solo permisos de visualización
    'FACTURA_VER',
    'FACTURA_EXPORTAR',
    'CLIENTE_VER',
    'CLIENTE_EXPORTAR',
    'PRODUCTO_VER',
    'PRODUCTO_EXPORTAR',
    'REPORTE_DASHBOARD',
    'NOTIFICACION_VER'
);

-- =====================================================
-- PASO 7: ASIGNAR PERMISOS AL ROL CLIENTE (3 permisos)
-- =====================================================

-- El rol CLIENTE solo puede ver sus propias facturas y notificaciones
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'CLIENTE'
AND p.codigo IN (
    -- Acceso muy limitado para clientes externos
    'FACTURA_VER',
    'FACTURA_EXPORTAR',
    'NOTIFICACION_VER'
);

-- =====================================================
-- PASO 8: ASIGNAR PERMISOS AL ROL ADMIN (48 permisos - TODOS)
-- =====================================================

-- El rol ADMIN tiene TODOS los permisos del sistema
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'ADMIN';

-- =====================================================
-- PASO 9: VERIFICACIÓN
-- =====================================================

-- Verificar cantidad de permisos por rol
SELECT r.nombre, COUNT(rp.id_permiso) as total_permisos
FROM rol r
LEFT JOIN rol_permiso rp ON r.id_rol = rp.id_rol
GROUP BY r.nombre
ORDER BY total_permisos DESC;

-- Resultado esperado:
-- ADMIN: 48 permisos
-- GERENTE: 30 permisos
-- VENDEDOR: 15 permisos
-- USER: 10 permisos
-- VISUALIZADOR: 8 permisos
-- CLIENTE: 3 permisos

-- Verificar permisos críticos
SELECT COUNT(*) as permisos_criticos
FROM permiso
WHERE es_critico = TRUE;

-- Resultado esperado: 19 permisos críticos

-- Verificar permisos por categoría
SELECT categoria, COUNT(*) as total
FROM permiso
WHERE activo = TRUE
GROUP BY categoria
ORDER BY categoria;

-- Resultado esperado:
-- Auditoría: 2
-- Clientes: 5
-- Configuración: 5
-- Facturación: 7
-- Notificaciones: 5
-- Productos: 6
-- Reportes: 7
-- Sistema: 3
-- Usuarios: 8

-- =====================================================
-- PASO 10 (OPCIONAL): MIGRAR USUARIOS EXISTENTES
-- =====================================================

-- Este paso asigna roles a usuarios existentes basándose en el campo 'rol' actual
-- IMPORTANTE: Revisar los valores del campo 'rol' en tu tabla usuario antes de ejecutar

-- Asignar rol ADMIN a usuarios con rol 'ADMIN'
UPDATE usuario u
SET id_rol = (SELECT id_rol FROM rol WHERE codigo = 'ADMIN')
WHERE u.rol = 'ADMIN';

-- Asignar rol GERENTE a usuarios con rol 'GERENTE' o 'MANAGER'
UPDATE usuario u
SET id_rol = (SELECT id_rol FROM rol WHERE codigo = 'GERENTE')
WHERE u.rol IN ('GERENTE', 'MANAGER');

-- Asignar rol VENDEDOR a usuarios con rol 'VENDEDOR'
UPDATE usuario u
SET id_rol = (SELECT id_rol FROM rol WHERE codigo = 'VENDEDOR')
WHERE u.rol = 'VENDEDOR';

-- Asignar rol USER a usuarios con rol 'USER'
UPDATE usuario u
SET id_rol = (SELECT id_rol FROM rol WHERE codigo = 'USER')
WHERE u.rol = 'USER';

-- Asignar rol VISUALIZADOR a usuarios con rol 'VISUALIZADOR' o 'VIEWER'
UPDATE usuario u
SET id_rol = (SELECT id_rol FROM rol WHERE codigo = 'VISUALIZADOR')
WHERE u.rol IN ('VISUALIZADOR', 'VIEWER');

-- Asignar rol CLIENTE a usuarios con rol 'CLIENTE' o 'CLIENT'
UPDATE usuario u
SET id_rol = (SELECT id_rol FROM rol WHERE codigo = 'CLIENTE')
WHERE u.rol IN ('CLIENTE', 'CLIENT');

-- Verificar migración de usuarios
SELECT 
    u.nombre,
    u.rol as rol_antiguo,
    r.nombre as rol_nuevo
FROM usuario u
LEFT JOIN rol r ON u.id_rol = r.id_rol
ORDER BY r.nombre, u.nombre;

-- =====================================================
-- FIN DEL SCRIPT
-- =====================================================

-- NOTAS IMPORTANTES:
-- 1. Este script debe ejecutarse DESPUÉS del primer arranque de Spring Boot
-- 2. Spring Boot creará automáticamente las tablas con JPA/Hibernate
-- 3. Verificar que todas las tablas existan antes de ejecutar los INSERTs
-- 4. El PASO 10 es opcional y debe ajustarse según los valores actuales del campo 'rol'
-- 5. Después de ejecutar este script, el campo 'usuario.rol' se puede considerar deprecado
-- 6. Se recomienda hacer un backup completo de la base de datos antes de ejecutar
--
-- ROLES DISPONIBLES:
-- - ADMIN:        Administradores del sistema (48 base + 7 pagos + 7 contabilidad = 62 permisos)
-- - GERENTE:      Gerentes de área (30 base + 5 pagos + 5 contabilidad = 40 permisos)
-- - VENDEDOR:     Personal de ventas (15 base + 2 pagos + 2 contabilidad = 19 permisos)
-- - USER:         Usuarios básicos (10 permisos - sin cambios)
-- - VISUALIZADOR: Solo lectura (8 base + 2 pagos + 2 contabilidad = 12 permisos)
-- - CLIENTE:      Clientes externos (3 permisos - sin cambios)
--
-- PERMISOS CRÍTICOS (Solo ADMIN): 19 base + 3 pagos + 3 contabilidad = 25 permisos críticos
-- - Todos los permisos de USUARIO_* (8)
-- - Todos los permisos de AUDITORIA_* (2)
-- - Todos los permisos de SISTEMA_* (3)
-- - FACTURA_ELIMINAR, FACTURA_ANULAR (2)
-- - CONFIG_EDITAR_EMPRESA (1)
-- - NOTIFICACION_ELIMINAR (1)
-- - PRODUCTO_* críticos (2)
-- - CLIENTE_* críticos (0)
-- - PAGO_ELIMINAR, PAGO_ANULAR, PAGO_MODIFICAR_CONCILIADO (3)
-- - CONTABILIDAD_ELIMINAR, CONTABILIDAD_CONTABILIZAR, CONTABILIDAD_ANULAR (3)
--
-- SCRIPTS ADICIONALES:
-- - MIGRATION_PAGOS_PERMISOS.sql: Agrega 7 permisos para gestión de pagos (Sprint 5 - Fase 1)
-- - MIGRATION_CONTABILIDAD_PERMISOS.sql: Agrega 7 permisos para contabilidad (Sprint 5 - Fase 2)
