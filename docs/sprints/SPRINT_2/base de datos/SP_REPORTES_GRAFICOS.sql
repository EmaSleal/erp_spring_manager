-- ============================================================================
-- STORED PROCEDURES PARA REPORTES Y GRÁFICOS
-- WhatsApp Orders Manager - Sprint 2 Fase 6
-- ============================================================================
-- Descripción: Procedimientos almacenados optimizados para obtener datos
--              estadísticos de reportes y gráficos de forma eficiente.
--              Reduce la carga del servidor de aplicaciones.
-- ============================================================================
-- Fecha: 18/10/2025
-- Autor: GitHub Copilot
-- ============================================================================

USE whatsapp_orders_manager;

-- ============================================================================
-- SP 1: VENTAS POR MES
-- ============================================================================
-- Descripción: Obtiene el total de ventas agrupado por mes
-- Parámetros: 
--   @meses: Número de meses hacia atrás a consultar (default: 12)
-- Retorna: mes (VARCHAR), total_ventas (DECIMAL)
-- ============================================================================

DELIMITER //

DROP PROCEDURE IF EXISTS sp_obtener_ventas_por_mes//

CREATE PROCEDURE sp_obtener_ventas_por_mes(
    IN p_meses INT
)
BEGIN
    DECLARE v_fecha_inicio DATE;
    
    -- Calcular fecha de inicio (N meses atrás)
    SET v_fecha_inicio = DATE_SUB(CURDATE(), INTERVAL p_meses MONTH);
    
    -- Obtener ventas agrupadas por mes
    SELECT 
        DATE_FORMAT(f.fecha_emision, '%b %Y') AS mes,
        COALESCE(SUM(f.total), 0) AS total_ventas
    FROM factura f
    WHERE f.fecha_emision >= v_fecha_inicio
        AND f.fecha_emision <= CURDATE()
    GROUP BY DATE_FORMAT(f.fecha_emision, '%Y-%m'),
             DATE_FORMAT(f.fecha_emision, '%b %Y')
    ORDER BY DATE_FORMAT(f.fecha_emision, '%Y-%m') ASC;
    
END//

DELIMITER ;

-- ============================================================================
-- SP 2: CLIENTES NUEVOS POR MES
-- ============================================================================
-- Descripción: Obtiene la cantidad de clientes nuevos por mes
-- Parámetros: 
--   @meses: Número de meses hacia atrás a consultar (default: 12)
-- Retorna: mes (VARCHAR), cantidad_clientes (INT)
-- ============================================================================

DELIMITER //

DROP PROCEDURE IF EXISTS sp_obtener_clientes_nuevos_por_mes//

CREATE PROCEDURE sp_obtener_clientes_nuevos_por_mes(
    IN p_meses INT
)
BEGIN
    DECLARE v_fecha_inicio DATE;
    
    -- Calcular fecha de inicio (N meses atrás)
    SET v_fecha_inicio = DATE_SUB(CURDATE(), INTERVAL p_meses MONTH);
    
    -- Obtener clientes nuevos agrupados por mes
    SELECT 
        DATE_FORMAT(c.create_date, '%b %Y') AS mes,
        COUNT(*) AS cantidad_clientes
    FROM cliente c
    WHERE c.create_date >= v_fecha_inicio
        AND c.create_date <= CURDATE()
    GROUP BY DATE_FORMAT(c.create_date, '%Y-%m'),
             DATE_FORMAT(c.create_date, '%b %Y')
    ORDER BY DATE_FORMAT(c.create_date, '%Y-%m') ASC;
    
END//

DELIMITER ;

-- ============================================================================
-- SP 3: PRODUCTOS MÁS VENDIDOS
-- ============================================================================
-- Descripción: Obtiene el ranking de productos más vendidos
-- Parámetros: 
--   @limite: Número de productos a retornar (default: 10)
-- Retorna: producto (VARCHAR), cantidad_vendida (DECIMAL)
-- ============================================================================

DELIMITER //

DROP PROCEDURE IF EXISTS sp_obtener_productos_mas_vendidos//

CREATE PROCEDURE sp_obtener_productos_mas_vendidos(
    IN p_limite INT
)
BEGIN
    -- Obtener productos más vendidos
    SELECT 
        p.descripcion AS producto,
        COALESCE(SUM(lf.cantidad), 0) AS cantidad_vendida
    FROM producto p
    LEFT JOIN linea_factura lf ON p.id_producto = lf.id_producto
    LEFT JOIN factura f ON lf.id_factura = f.id_factura
    WHERE p.active = 1
    GROUP BY p.id_producto, p.descripcion
    HAVING cantidad_vendida > 0
    ORDER BY cantidad_vendida DESC
    LIMIT p_limite;
    
END//

DELIMITER ;

-- ============================================================================
-- SP 4: VENTAS POR DÍA (Para gráfico de ventas filtradas)
-- ============================================================================
-- Descripción: Obtiene ventas agrupadas por día en un rango de fechas
-- Parámetros: 
--   @fecha_inicio: Fecha de inicio del rango (opcional)
--   @fecha_fin: Fecha de fin del rango (opcional)
--   @cliente_id: ID del cliente para filtrar (opcional)
-- Retorna: fecha (DATE), total_ventas (DECIMAL)
-- ============================================================================

DELIMITER //

DROP PROCEDURE IF EXISTS sp_obtener_ventas_por_dia//

CREATE PROCEDURE sp_obtener_ventas_por_dia(
    IN p_fecha_inicio DATE,
    IN p_fecha_fin DATE,
    IN p_cliente_id INT
)
BEGIN
    -- Obtener ventas agrupadas por día
    SELECT 
        DATE(f.fecha_emision) AS fecha,
        COALESCE(SUM(f.total), 0) AS total_ventas
    FROM factura f
    WHERE (p_fecha_inicio IS NULL OR f.fecha_emision >= p_fecha_inicio)
        AND (p_fecha_fin IS NULL OR f.fecha_emision <= p_fecha_fin)
        AND (p_cliente_id IS NULL OR f.id_cliente = p_cliente_id)
    GROUP BY DATE(f.fecha_emision)
    ORDER BY fecha ASC;
    
END//

DELIMITER ;

-- ============================================================================
-- SP 5: ESTADÍSTICAS GENERALES DE VENTAS
-- ============================================================================
-- Descripción: Obtiene estadísticas agregadas de ventas
-- Parámetros: 
--   @fecha_inicio: Fecha de inicio del rango (opcional)
--   @fecha_fin: Fecha de fin del rango (opcional)
-- Retorna: total_facturas, total_ventas, ticket_promedio, ventas_pendientes, ventas_pagadas
-- ============================================================================

DELIMITER //

DROP PROCEDURE IF EXISTS sp_obtener_estadisticas_ventas//

CREATE PROCEDURE sp_obtener_estadisticas_ventas(
    IN p_fecha_inicio DATE,
    IN p_fecha_fin DATE
)
BEGIN
    SELECT 
        COUNT(*) AS total_facturas,
        COALESCE(SUM(f.total), 0) AS total_ventas,
        COALESCE(AVG(f.total), 0) AS ticket_promedio,
        COALESCE(SUM(CASE WHEN f.tipo = 'PENDIENTE' THEN f.total ELSE 0 END), 0) AS ventas_pendientes,
        COALESCE(SUM(CASE WHEN f.tipo = 'PAGADO' THEN f.total ELSE 0 END), 0) AS ventas_pagadas,
        COALESCE(SUM(CASE WHEN f.entregado = 1 THEN 1 ELSE 0 END), 0) AS facturas_entregadas,
        COALESCE(SUM(CASE WHEN f.entregado = 0 THEN 1 ELSE 0 END), 0) AS facturas_pendientes_entrega
    FROM factura f
    WHERE (p_fecha_inicio IS NULL OR f.fecha_emision >= p_fecha_inicio)
        AND (p_fecha_fin IS NULL OR f.fecha_emision <= p_fecha_fin);
    
END//

DELIMITER ;

-- ============================================================================
-- SP 6: TOP CLIENTES POR VOLUMEN DE COMPRAS
-- ============================================================================
-- Descripción: Obtiene el ranking de clientes con mayor volumen de compras
-- Parámetros: 
--   @limite: Número de clientes a retornar (default: 10)
-- Retorna: cliente, total_compras, cantidad_facturas
-- ============================================================================

DELIMITER //

DROP PROCEDURE IF EXISTS sp_obtener_top_clientes//

CREATE PROCEDURE sp_obtener_top_clientes(
    IN p_limite INT
)
BEGIN
    -- Obtener top clientes por volumen de compras
    SELECT 
        c.nombre AS cliente,
        COALESCE(SUM(f.total), 0) AS total_compras,
        COUNT(f.id_factura) AS cantidad_facturas
    FROM cliente c
    LEFT JOIN factura f ON c.id_cliente = f.id_cliente
    WHERE c.activo = 1
    GROUP BY c.id_cliente, c.nombre
    HAVING total_compras > 0
    ORDER BY total_compras DESC
    LIMIT p_limite;
    
END//

DELIMITER ;

-- ============================================================================
-- PRUEBAS DE LOS STORED PROCEDURES
-- ============================================================================

-- Prueba SP 1: Ventas por mes (últimos 12 meses)
-- CALL sp_obtener_ventas_por_mes(12);

-- Prueba SP 2: Clientes nuevos (últimos 12 meses)
-- CALL sp_obtener_clientes_nuevos_por_mes(12);

-- Prueba SP 3: Top 10 productos más vendidos
-- CALL sp_obtener_productos_mas_vendidos(10);

-- Prueba SP 4: Ventas por día (rango de fechas)
-- CALL sp_obtener_ventas_por_dia('2025-01-01', '2025-12-31', NULL);

-- Prueba SP 5: Estadísticas generales de ventas
-- CALL sp_obtener_estadisticas_ventas('2025-01-01', '2025-12-31');

-- Prueba SP 6: Top 10 clientes
-- CALL sp_obtener_top_clientes(10);

-- ============================================================================
-- ÍNDICES RECOMENDADOS PARA OPTIMIZACIÓN
-- ============================================================================

-- Índice en fecha_emision para ventas por mes
CREATE INDEX IF NOT EXISTS idx_factura_fecha_emision 
    ON factura(fecha_emision);

-- Índice en create_date de cliente
CREATE INDEX IF NOT EXISTS idx_cliente_create_date 
    ON cliente(create_date);

-- Índice en id_producto de linea_factura
CREATE INDEX IF NOT EXISTS idx_linea_factura_producto 
    ON linea_factura(id_producto);

-- Índice compuesto para filtros de facturas
CREATE INDEX IF NOT EXISTS idx_factura_fecha_cliente 
    ON factura(fecha_emision, id_cliente);

-- ============================================================================
-- FIN DEL SCRIPT
-- ============================================================================

-- Mensaje de confirmación
SELECT 'Stored Procedures creados exitosamente' AS mensaje,
       '6 procedimientos disponibles' AS detalle,
       'Optimizados para reportes y gráficos' AS observacion;
