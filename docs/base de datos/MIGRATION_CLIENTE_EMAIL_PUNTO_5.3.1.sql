-- =============================================
-- MIGRACIÓN: Agregar campo email a tabla Cliente
-- Punto: 5.3.1 - Envío de facturas por email
-- Fecha: 2025-01-13
-- Descripción: Agrega campo email a la tabla cliente
--              para enviar facturas y comunicaciones
-- =============================================

USE whats_orders_manager;

-- Agregar columna email a la tabla cliente
ALTER TABLE cliente
ADD COLUMN email VARCHAR(100) NULL COMMENT 'Email del cliente para facturas y comunicaciones';

-- Crear índice para búsquedas rápidas por email
CREATE INDEX idx_cliente_email ON cliente(email);

-- Opcional: Copiar emails de la tabla usuario relacionada (si existe la relación)
-- UPDATE cliente c
-- INNER JOIN usuario u ON c.idUsuario = u.telefono
-- SET c.email = u.email
-- WHERE c.email IS NULL AND u.email IS NOT NULL;

SELECT 'Migración completada: Campo email agregado a tabla cliente' AS resultado;
