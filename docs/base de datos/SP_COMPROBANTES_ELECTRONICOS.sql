-- =============================================
-- STORED PROCEDURES PARA COMPROBANTES ELECTRÓNICOS
-- Sistema de Facturación Electrónica - Costa Rica
-- Sprint 5 - Fase 3
-- =============================================

USE orders_db;

DELIMITER $$

-- =============================================
-- SP: Listar comprobantes por empresa con paginación
-- Columnas en el orden exacto que espera convertirResultadosADTO:
--   [0] id, [1] clave_numerica, [2] consecutivo, [3] tipo_comprobante,
--   [4] fecha_emision, [5] estado, [6] factura_id, [7] empresa_id,
--   [8] codigo_respuesta, [9] mensaje_respuesta, [10] intentos_envio, [11] enviado_email
-- =============================================
DROP PROCEDURE IF EXISTS sp_listar_comprobantes_por_empresa$$
CREATE PROCEDURE sp_listar_comprobantes_por_empresa(
    IN p_empresa_id INT,
    IN p_page INT,
    IN p_size INT
)
BEGIN
    DECLARE v_offset INT;
    SET v_offset = p_page * p_size;

    SELECT
        ce.id,
        ce.clave_numerica,
        ce.consecutivo,
        ce.tipo_comprobante,
        ce.fecha_emision,
        ce.estado,
        ce.factura_id,
        ce.empresa_id,
        ce.codigo_respuesta,
        ce.mensaje_respuesta,
        ce.intentos_envio,
        ce.enviado_email
    FROM comprobante_electronico ce
    INNER JOIN configuracion_empresa e ON e.id_configuracion = ce.empresa_id
    LEFT JOIN factura f ON f.id_factura = ce.factura_id
    WHERE ce.empresa_id = p_empresa_id
    ORDER BY ce.fecha_emision DESC
    LIMIT p_size OFFSET v_offset;
END$$

-- =============================================
-- SP: Listar comprobantes por empresa y estado
-- =============================================
DROP PROCEDURE IF EXISTS sp_listar_comprobantes_por_estado$$
CREATE PROCEDURE sp_listar_comprobantes_por_estado(
    IN p_empresa_id INT,
    IN p_estado VARCHAR(20),
    IN p_page INT,
    IN p_size INT
)
BEGIN
    DECLARE v_offset INT;
    SET v_offset = p_page * p_size;

    SELECT
        ce.id,
        ce.clave_numerica,
        ce.consecutivo,
        ce.tipo_comprobante,
        ce.fecha_emision,
        ce.estado,
        ce.factura_id,
        ce.empresa_id,
        ce.codigo_respuesta,
        ce.mensaje_respuesta,
        ce.intentos_envio,
        ce.enviado_email
    FROM comprobante_electronico ce
    INNER JOIN configuracion_empresa e ON e.id_configuracion = ce.empresa_id
    LEFT JOIN factura f ON f.id_factura = ce.factura_id
    WHERE ce.empresa_id = p_empresa_id
      AND ce.estado = p_estado
    ORDER BY ce.fecha_emision DESC
    LIMIT p_size OFFSET v_offset;
END$$

-- =============================================
-- SP: Listar comprobantes por rango de fechas
-- =============================================
DROP PROCEDURE IF EXISTS sp_listar_comprobantes_por_fechas$$
CREATE PROCEDURE sp_listar_comprobantes_por_fechas(
    IN p_empresa_id INT,
    IN p_fecha_inicio DATETIME,
    IN p_fecha_fin DATETIME,
    IN p_page INT,
    IN p_size INT
)
BEGIN
    DECLARE v_offset INT;
    SET v_offset = p_page * p_size;

    SELECT
        ce.id,
        ce.clave_numerica,
        ce.consecutivo,
        ce.tipo_comprobante,
        ce.fecha_emision,
        ce.estado,
        ce.factura_id,
        ce.empresa_id,
        ce.codigo_respuesta,
        ce.mensaje_respuesta,
        ce.intentos_envio,
        ce.enviado_email
    FROM comprobante_electronico ce
    INNER JOIN configuracion_empresa e ON e.id_configuracion = ce.empresa_id
    LEFT JOIN factura f ON f.id_factura = ce.factura_id
    WHERE ce.empresa_id = p_empresa_id
      AND ce.fecha_emision BETWEEN p_fecha_inicio AND p_fecha_fin
    ORDER BY ce.fecha_emision DESC
    LIMIT p_size OFFSET v_offset;
END$$

-- =============================================
-- SP: Contar comprobantes por estado
-- =============================================
DROP PROCEDURE IF EXISTS sp_contar_comprobantes_por_estado$$
CREATE PROCEDURE sp_contar_comprobantes_por_estado(
    IN p_empresa_id INT,
    IN p_estado VARCHAR(20)
)
BEGIN
    SELECT COUNT(*) AS total
    FROM comprobante_electronico
    WHERE empresa_id = p_empresa_id 
      AND estado = p_estado;
END$$

-- =============================================
-- SP: Listar comprobantes pendientes de envío
-- =============================================
DROP PROCEDURE IF EXISTS sp_listar_comprobantes_pendientes$$
CREATE PROCEDURE sp_listar_comprobantes_pendientes()
BEGIN
    SELECT 
        ce.id,
        ce.factura_id AS facturaId,
        ce.empresa_id AS empresaId,
        ce.tipo_comprobante AS tipoComprobante,
        ce.clave_numerica AS claveNumerica,
        ce.consecutivo,
        ce.fecha_emision AS fechaEmision,
        ce.estado,
        ce.intentos_envio AS intentosEnvio,
        ce.xml_comprobante AS xmlComprobante,
        ce.ultimo_error AS ultimoError,
        ce.created_at AS createdAt
    FROM comprobante_electronico ce
    WHERE ce.estado IN ('GENERADO', 'FIRMADO', 'ERROR')
      AND ce.intentos_envio < 3
    ORDER BY ce.created_at ASC
    LIMIT 100; -- Limitar para procesamiento por lotes
END$$

-- =============================================
-- SP: Obtener comprobante por factura
-- =============================================
DROP PROCEDURE IF EXISTS sp_obtener_comprobante_por_factura$$
CREATE PROCEDURE sp_obtener_comprobante_por_factura(
    IN p_id_factura INT
)
BEGIN
    SELECT 
        ce.*,
        e.nombre_empresa,
        COALESCE(e.nombre_comercial, e.nombre_empresa) AS nombre_comercial,
        f.numero_factura
    FROM comprobante_electronico ce
    INNER JOIN empresa e ON e.id_empresa = ce.empresa_id
    INNER JOIN factura f ON f.id_factura = ce.factura_id
    WHERE ce.factura_id = p_id_factura
    LIMIT 1;
END$$

-- =============================================
-- SP: Obtener comprobante por clave numérica
-- =============================================
DROP PROCEDURE IF EXISTS sp_obtener_comprobante_por_clave$$
CREATE PROCEDURE sp_obtener_comprobante_por_clave(
    IN p_clave_numerica VARCHAR(50)
)
BEGIN
    SELECT 
        ce.*,
        e.nombre_empresa,
        COALESCE(e.nombre_comercial, e.nombre_empresa) AS nombre_comercial,
        f.numero_factura
    FROM comprobante_electronico ce
    INNER JOIN empresa e ON e.id_empresa = ce.empresa_id
    LEFT JOIN factura f ON f.id_factura = ce.factura_id
    WHERE ce.clave_numerica = p_clave_numerica
    LIMIT 1;
END$$

-- =============================================
-- SP: Dashboard - Resumen de comprobantes
-- =============================================
DROP PROCEDURE IF EXISTS sp_resumen_comprobantes_empresa$$
CREATE PROCEDURE sp_resumen_comprobantes_empresa(
    IN p_empresa_id INT,
    IN p_fecha_desde DATE,
    IN p_fecha_hasta DATE
)
BEGIN
    SELECT 
        ce.estado,
        CASE ce.estado
            WHEN 'GENERADO' THEN 'Generado'
            WHEN 'FIRMADO' THEN 'Firmado'
            WHEN 'ENVIADO' THEN 'Enviado'
            WHEN 'PROCESANDO' THEN 'Procesando'
            WHEN 'ACEPTADO' THEN 'Aceptado'
            WHEN 'RECHAZADO' THEN 'Rechazado'
            WHEN 'ERROR' THEN 'Error'
            WHEN 'ANULADO' THEN 'Anulado'
            ELSE ce.estado
        END AS estadoDescripcion,
        COUNT(*) AS cantidad,
        COUNT(CASE WHEN DATE(ce.fecha_emision) = CURDATE() THEN 1 END) AS cantidad_hoy,
        MAX(ce.fecha_emision) AS ultima_emision
    FROM comprobante_electronico ce
    WHERE ce.empresa_id = p_empresa_id
      AND DATE(ce.fecha_emision) BETWEEN p_fecha_desde AND p_fecha_hasta
    GROUP BY ce.estado
    ORDER BY cantidad DESC;
END$$

DELIMITER ;

-- =============================================
-- Verificar creación de SPs
-- =============================================
SHOW PROCEDURE STATUS WHERE Db = 'orders_db' AND Name LIKE 'sp_%comprobante%';
