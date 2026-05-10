-- ============================================================
-- Seed: rol
-- Idempotency: INSERT IGNORE
-- ============================================================
SET NAMES utf8mb4;
USE facturas_monrachem;
INSERT IGNORE INTO `rol` (`id_rol`, `activo`, `codigo`, `descripcion`, `nombre`, `create_by`, `create_date`, `update_by`, `update_date`) VALUES (1,_binary '','ADMIN','Rol con acceso total al sistema incluyendo configuraci├│n y usuarios','Administrador',NULL,NULL,NULL,NULL);
INSERT IGNORE INTO `rol` (`id_rol`, `activo`, `codigo`, `descripcion`, `nombre`, `create_by`, `create_date`, `update_by`, `update_date`) VALUES (2,_binary '','GERENTE','Rol para gerentes de ├írea con permisos de gesti├│n y reportes','Gerente',NULL,NULL,NULL,NULL);
INSERT IGNORE INTO `rol` (`id_rol`, `activo`, `codigo`, `descripcion`, `nombre`, `create_by`, `create_date`, `update_by`, `update_date`) VALUES (3,_binary '','VENDEDOR','Rol para personal de ventas con permisos b├ísicos de operaci├│n','Vendedor',NULL,NULL,NULL,NULL);
INSERT IGNORE INTO `rol` (`id_rol`, `activo`, `codigo`, `descripcion`, `nombre`, `create_by`, `create_date`, `update_by`, `update_date`) VALUES (4,_binary '','USER','Rol b├ísico con permisos de solo lectura y operaciones simples','Usuario',NULL,NULL,NULL,NULL);
INSERT IGNORE INTO `rol` (`id_rol`, `activo`, `codigo`, `descripcion`, `nombre`, `create_by`, `create_date`, `update_by`, `update_date`) VALUES (5,_binary '','VISUALIZADOR','Rol con acceso de solo lectura sin posibilidad de modificar datos','Visualizador',NULL,NULL,NULL,NULL);
INSERT IGNORE INTO `rol` (`id_rol`, `activo`, `codigo`, `descripcion`, `nombre`, `create_by`, `create_date`, `update_by`, `update_date`) VALUES (6,_binary '','CLIENTE','Rol para clientes externos con acceso limitado a sus propias facturas','Cliente',NULL,NULL,NULL,NULL);
