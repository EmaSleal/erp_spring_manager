-- MySQL dump 10.13  Distrib 8.0.34, for Win64 (x86_64)
--
-- Host: 192.168.100.93    Database: facturas_monrachem
-- ------------------------------------------------------
-- Server version	8.0.46-0ubuntu0.24.04.2

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping routines for database 'facturas_monrachem'
--
/*!50003 DROP PROCEDURE IF EXISTS `ActualizarCliente` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `ActualizarCliente`(
    IN pIdCliente INT,
    IN pNombre VARCHAR(255),
    IN pIdUsuario INT,
    IN pUpdateBy INT
)
BEGIN
    UPDATE cliente
    SET 
        nombre = pNombre,
        id_usuario = pIdUsuario,
        update_by = pUpdateBy,
        update_date = CURRENT_TIMESTAMP
    WHERE id_cliente = pIdCliente;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `ActualizarTotalFactura` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `ActualizarTotalFactura`(
    IN pIdFactura INT
)
BEGIN
    UPDATE factura
    SET total = (
        SELECT SUM(subtotal)
        FROM linea_factura
        WHERE id_factura = pIdFactura
    )
    WHERE id_factura = pIdFactura;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `CrearFactura` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `CrearFactura`(
    IN pIdCliente INT,
    IN pFechaEntrega DATE,
    IN pDescripcion VARCHAR(255),
    OUT pIdFactura INT
)
BEGIN
    INSERT INTO factura (id_cliente, create_date, fecha_entrega, entregado, total, descripcion)
    VALUES (pIdCliente, NOW(), pFechaEntrega, 0, 0, pDescripcion);
    
    SET pIdFactura = LAST_INSERT_ID(); 
    SELECT pIdFactura AS pIdFactura;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `InsertarCliente` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `InsertarCliente`(
    IN pNombre VARCHAR(255),
    IN pIdUsuario VARCHAR(50),
    OUT pIdCliente INT
)
BEGIN
    
    SELECT id_cliente INTO pIdCliente
    FROM cliente
    WHERE nombre = pNombre
    LIMIT 1;

    
    IF pIdCliente IS NULL THEN
        INSERT INTO cliente (nombre, id_usuario)
        VALUES (pNombre, pIdUsuario);

        SET pIdCliente = LAST_INSERT_ID(); 
    END IF;

    
    SELECT pIdCliente AS pIdCliente;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `InsertarLineaFactura` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `InsertarLineaFactura`(
    IN pIdFactura INT,
    IN pIdProducto INT,
    IN pCantidad INT,
    IN pPrecioUnitario DECIMAL(10, 2),
    OUT pIdLineaFactura INT
)
BEGIN
    INSERT INTO linea_factura (id_factura, id_producto, cantidad, precio_unitario, subtotal)
    VALUES (pIdFactura, pIdProducto, pCantidad, pPrecioUnitario, pCantidad*pPrecioUnitario);
    
    SET pIdLineaFactura = LAST_INSERT_ID(); 
    
	CALL ActualizarTotalFactura();
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `InsertarPresentacion` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `InsertarPresentacion`(IN pNombre VARCHAR(50), OUT pIdPresentacion INT)
BEGIN
    
    SELECT id_presentacion INTO pIdPresentacion 
    FROM presentacion WHERE nombre = pNombre 
    LIMIT 1;
    
    
    IF pIdPresentacion IS NULL THEN
        INSERT INTO presentacion (nombre) VALUES (pNombre);
        SELECT pIdPresentacion = LAST_INSERT_ID();
    END IF;
    SELECT pIdPresentacion;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `InsertarProducto` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `InsertarProducto`(
    IN pDescripcion VARCHAR(255),
    IN pCodigo VARCHAR(50),
    IN pIdPresentacion INT,
    IN pPrecioInstitucional DECIMAL(10,2),
    IN pPrecioMayorista DECIMAL(10,2),
    OUT pIdProducto INT
)
BEGIN
    DECLARE existingId INT;
    
    
    SELECT id_producto INTO existingId FROM producto WHERE codigo = pCodigo LIMIT 1;
    
    IF existingId IS NOT NULL THEN
        
        UPDATE producto
        SET descripcion = pDescripcion,
            id_presentacion = pIdPresentacion,
            precio_institucional = pPrecioInstitucional,
            precio_mayorista = pPrecioMayorista
        WHERE id_producto = existingId;
        
        SET pIdProducto = existingId; 
    ELSE
        
        INSERT INTO producto (descripcion, codigo, id_presentacion, precio_institucional, precio_mayorista)
        VALUES (pDescripcion, pCodigo, pIdPresentacion, pPrecioInstitucional, pPrecioMayorista);
        
        SET pIdProducto = LAST_INSERT_ID(); 
    END IF;
    
    
    SELECT pIdProducto AS pIdProducto;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `InsertarUsuario` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `InsertarUsuario`(
    IN pNombre VARCHAR(255),
    IN pTelefono VARCHAR(15),
    OUT pIdUsuario INT
)
BEGIN
    INSERT INTO usuario (nombre, telefono)
    VALUES (pNombre, pTelefono);
    
    SET pIdUsuario = LAST_INSERT_ID(); 
    
    SELECT pIdUsuario AS pIdUsuario;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `ObtenerClientes` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `ObtenerClientes`()
BEGIN
    SELECT id_cliente AS idCliente, nombre FROM cliente;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `ObtenerFacturaCompleta` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `ObtenerFacturaCompleta`(
    IN pIdFactura INT
)
BEGIN
    
	SELECT 
        f.id_Factura AS idFactura,
        c.id_cliente AS idCliente,
        c.nombre AS nombreCliente,
        c.id_usuario AS telefono,
        f.create_date AS fechaCreacion,
        f.fecha_entrega AS fechaEntrega,
        f.descripcion AS tipoFactura,
        c.nombre AS NombreCliente
    FROM factura f
    INNER JOIN cliente c ON f.id_cliente = c.id_cliente
    WHERE f.id_factura = pIdFactura;

    
    SELECT 
        lf.id_linea_factura,
        p.id_producto,
        CONCAT(p.descripcion, " (", pres.nombre, ")") AS nombreProducto,
        lf.cantidad,
        lf.precio_unitario,
        lf.subtotal
    FROM linea_factura lf
    INNER JOIN producto p ON lf.id_producto = p.id_producto
    INNER JOIN presentacion pres ON p.id_presentacion = pres.id_presentacion
    WHERE lf.id_factura = pIdFactura;

    
    SELECT 
        f.id_factura,
        f.total AS totalFactura
    FROM factura f
    WHERE f.id_factura = pIdFactura;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `ObtenerHistorialMensajes` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `ObtenerHistorialMensajes`(
    IN pPhoneNumber VARCHAR(50),
    IN pLimite INT
)
BEGIN
    SELECT message_body
    FROM webhooklogs
    WHERE phone_number = pPhoneNumber
    ORDER BY timestamp DESC
    LIMIT pLimite;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `ObtenerProductos` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `ObtenerProductos`()
BEGIN
    SELECT PRO.id_producto, CONCAT(PRO.descripcion," (",PRES.nombre,")")  AS nombre, PRO.precio_institucional, PRO.precio_mayorista FROM producto PRO
    INNER JOIN presentacion PRES ON PRES.id_presentacion = PRO.id_presentacion
    ORDER BY PRO.descripcion;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `ObtenerReportePorArticulo` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `ObtenerReportePorArticulo`(
    IN pFechaInicio DATE,
    IN pFechaFin DATE
)
BEGIN
    SELECT 
        p.id_producto,
        CONCAT(p.descripcion, " (", pres.nombre, ")") AS nombreProducto,
        SUM(lf.cantidad) AS cantidadTotal
    FROM linea_factura lf
    INNER JOIN factura f ON lf.id_factura = f.id_factura
    INNER JOIN producto p ON lf.id_producto = p.id_producto
    INNER JOIN presentacion pres ON p.id_presentacion = pres.id_presentacion
    WHERE f.fecha_entrega BETWEEN pFechaInicio AND pFechaFin
    GROUP BY p.id_producto, nombreProducto
    ORDER BY cantidadTotal DESC;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `RegistrarWebhook` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `RegistrarWebhook`(
    IN pMessageId VARCHAR(255),
    IN pPhoneNumber VARCHAR(50),
    IN pMessageBody TEXT,
    IN pWholeMessage JSON,
    OUT pExiste INT
)
BEGIN
    
    SELECT COUNT(*) INTO pExiste FROM webhooklogs WHERE message_id = pMessageId;

    IF pExiste = 0 THEN
        
        INSERT INTO webhooklogs (message_id, timestamp ,phone_number, message_body, whole_message)
        VALUES (pMessageId, CURDATE(),pPhoneNumber, pMessageBody, pWholeMessage);
    END IF;
    
    SELECT pExiste;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `sp_actualizar_linea_factura` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `sp_actualizar_linea_factura`(
    IN p_id_linea_factura INT,
    IN p_numero_linea INT,
    IN p_id_factura INT, 
    IN p_id_producto INT,
    IN p_cantidad INT,
    IN p_precioUnitario DECIMAL(10,2),
    IN p_subtotal DECIMAL(10,2),
    IN p_update_by INT
)
BEGIN
    DECLARE v_exists INT;

    SELECT COUNT(*) INTO v_exists
    FROM linea_factura
    WHERE id_linea_factura = p_id_linea_factura;

    IF v_exists > 0 THEN
        
        UPDATE linea_factura
        SET
            numero_linea = p_numero_linea,
            id_producto = p_id_producto,
            cantidad = p_cantidad,
            precio_unitario = p_precioUnitario,
            subtotal = p_subtotal,
            update_by = p_update_by,
            update_date = CURRENT_TIMESTAMP
        WHERE id_linea_factura = p_id_linea_factura;
        
        CALL ActualizarTotalFactura(p_id_factura);
    ELSE 
        
        INSERT INTO linea_factura (
            numero_linea,
            id_factura,
            id_producto,
            cantidad,
            precio_unitario,
            subtotal,
            create_by,
            update_by,
            create_date,
            update_date
        ) VALUES (
            p_numero_linea,
            p_id_factura,
            p_id_producto,
            p_cantidad,
            p_precioUnitario,
            p_subtotal,
            p_update_by,
            p_update_by,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
        CALL ActualizarTotalFactura(p_id_factura);
    END IF;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `SP_CLIENTES_NUEVOS_POR_MES` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `SP_CLIENTES_NUEVOS_POR_MES`(IN p_meses INT)
BEGIN
    DECLARE v_fecha_inicio DATE;
    
    
    SET v_fecha_inicio = DATE_SUB(CURDATE(), INTERVAL p_meses MONTH);
    
    
    SELECT 
        DATE_FORMAT(c.create_date, '%b %Y') AS mes_anio,
        COUNT(*) AS cantidad_clientes
    FROM 
        cliente c
    WHERE 
        c.create_date >= v_fecha_inicio
        AND c.create_date <= CURDATE()
    GROUP BY 
        YEAR(c.create_date), 
        MONTH(c.create_date)
    ORDER BY 
        YEAR(c.create_date), 
        MONTH(c.create_date);
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `sp_contar_comprobantes_por_estado` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `sp_contar_comprobantes_por_estado`(
    IN p_empresa_id INT,
    IN p_estado VARCHAR(20)
)
BEGIN
    SELECT COUNT(*) AS total
    FROM comprobante_electronico
    WHERE empresa_id = p_empresa_id 
      AND estado = p_estado;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `sp_crear_preferencias_usuario` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `sp_crear_preferencias_usuario`(
    IN p_id_usuario INT
)
BEGIN
    DECLARE v_email_disponible BOOLEAN;
    DECLARE v_telefono_disponible BOOLEAN;
    
    
    SELECT 
        (email IS NOT NULL AND email != '') INTO v_email_disponible
    FROM usuario 
    WHERE id_usuario = p_id_usuario;
    
    
    SELECT 
        (telefono IS NOT NULL AND telefono != '') INTO v_telefono_disponible
    FROM usuario 
    WHERE id_usuario = p_id_usuario;
    
    
    
    
    INSERT INTO preferencia_notificacion (
        id_usuario,
        tipo_notificacion,
        canal,
        activa,
        notificaciones_desactivadas_global,
        frecuencia,
        solo_horario_laboral,
        create_date,
        update_date
    ) VALUES (
        p_id_usuario,
        NULL,                   
        NULL,                   
        true,                   
        false,                  
        'INMEDIATA',           
        false,                 
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );
    
    
    
    
    INSERT INTO preferencia_notificacion (
        id_usuario,
        tipo_notificacion,
        canal,
        activa,
        notificaciones_desactivadas_global,
        frecuencia,
        solo_horario_laboral,
        create_date,
        update_date
    ) VALUES (
        p_id_usuario,
        'FACTURA_VENCIDA',
        'WEB',
        true,
        false,
        'INMEDIATA',
        false,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );
    
    
    
    
    IF v_email_disponible THEN
        INSERT INTO preferencia_notificacion (
            id_usuario,
            tipo_notificacion,
            canal,
            activa,
            notificaciones_desactivadas_global,
            frecuencia,
            solo_horario_laboral,
            create_date,
            update_date
        ) VALUES (
            p_id_usuario,
            'FACTURA_VENCIDA',
            'EMAIL',
            true,
            false,
            'INMEDIATA',
            false,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
    END IF;
    
    
    
    
    INSERT INTO preferencia_notificacion (
        id_usuario,
        tipo_notificacion,
        canal,
        activa,
        notificaciones_desactivadas_global,
        frecuencia,
        solo_horario_laboral,
        create_date,
        update_date
    ) VALUES (
        p_id_usuario,
        'FACTURA_PROXIMA_VENCER',
        'WEB',
        true,
        false,
        'INMEDIATA',
        false,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );
    
    
    
    
    INSERT INTO preferencia_notificacion (
        id_usuario,
        tipo_notificacion,
        canal,
        activa,
        notificaciones_desactivadas_global,
        frecuencia,
        solo_horario_laboral,
        create_date,
        update_date
    ) VALUES (
        p_id_usuario,
        'FACTURA_CREADA',      
        'WEB',
        true,
        false,
        'INMEDIATA',
        false,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );
    
    
    
    
    INSERT INTO preferencia_notificacion (
        id_usuario,
        tipo_notificacion,
        canal,
        activa,
        notificaciones_desactivadas_global,
        frecuencia,
        solo_horario_laboral,
        create_date,
        update_date
    ) VALUES (
        p_id_usuario,
        'STOCK_BAJO',
        'WEB',
        true,
        false,
        'INMEDIATA',
        false,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );
    
    
    
    
    INSERT INTO preferencia_notificacion (
        id_usuario,
        tipo_notificacion,
        canal,
        activa,
        notificaciones_desactivadas_global,
        frecuencia,
        solo_horario_laboral,
        create_date,
        update_date
    ) VALUES (
        p_id_usuario,
        'PAGO_RECIBIDO',
        'WEB',
        true,
        false,
        'INMEDIATA',
        false,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );
    
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `sp_desactivar_producto` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `sp_desactivar_producto`(
	IN p_id_producto INT
)
BEGIN
    UPDATE producto
    SET active = false
    WHERE (id_producto = p_id_producto);
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `SP_ESTADISTICAS_DASHBOARD` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `SP_ESTADISTICAS_DASHBOARD`()
BEGIN
    SELECT 
        
        (SELECT COUNT(*) FROM factura) AS total_facturas,
        
        
        (SELECT COUNT(*) FROM cliente) AS total_clientes,
        
        
        (SELECT COUNT(*) FROM producto WHERE active = TRUE) AS total_productos,
        
        
        (SELECT COUNT(*) FROM usuario WHERE activo = TRUE) AS total_usuarios,
        
        
        (SELECT COALESCE(SUM(total), 0) 
         FROM factura 
         WHERE MONTH(create_date) = MONTH(CURDATE()) 
         AND YEAR(create_date) = YEAR(CURDATE())) AS ventas_mes_actual,
        
        
        (SELECT COUNT(*) 
         FROM factura 
         WHERE tipo_factura = 'PENDIENTE') AS facturas_pendientes,
        
        
        (SELECT COUNT(*) 
         FROM cliente 
         WHERE MONTH(create_date) = MONTH(CURDATE()) 
         AND YEAR(create_date) = YEAR(CURDATE())) AS clientes_nuevos_mes;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `sp_get_lineas_factura` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `sp_get_lineas_factura`(
	IN idFactura INT
)
BEGIN
	SELECT  lf.id_linea_factura, lf.numero_linea, lf.id_producto, lf.id_factura,CONCAT(pr.descripcion,' (',PRE.nombre,')'), lf.cantidad, lf.precio_unitario AS precioUnitario, lf.subtotal, lf.create_by, lf.update_by, lf.create_date, lf.update_date 
    FROM linea_factura AS lf
    INNER JOIN producto AS pr ON pr.id_producto = lf.id_producto
    INNER JOIN presentacion AS PRE ON PRE.id_presentacion = pr.id_presentacion
    WHERE id_factura = idFactura;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `sp_listar_comprobantes_pendientes` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `sp_listar_comprobantes_pendientes`()
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
    LIMIT 100; 
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `sp_listar_comprobantes_por_empresa` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `sp_listar_comprobantes_por_empresa`(
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
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `sp_listar_comprobantes_por_estado` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `sp_listar_comprobantes_por_estado`(
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
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `sp_listar_comprobantes_por_fechas` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `sp_listar_comprobantes_por_fechas`(
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
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `sp_obtener_clientes_nuevos_por_mes` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `sp_obtener_clientes_nuevos_por_mes`(IN p_meses INT)
BEGIN
    DECLARE v_fecha_inicio DATE;
    SET v_fecha_inicio = DATE_SUB(CURDATE(), INTERVAL p_meses MONTH);
    
    SELECT 
        DATE_FORMAT(c.create_date, '%b %Y') AS mes,
        COUNT(*) AS cantidad_clientes
    FROM cliente c
    WHERE c.create_date >= v_fecha_inicio
        AND c.create_date <= CURDATE()
    GROUP BY DATE_FORMAT(c.create_date, '%Y-%m'),
             DATE_FORMAT(c.create_date, '%b %Y')
    ORDER BY DATE_FORMAT(c.create_date, '%Y-%m') ASC;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `sp_obtener_comprobante_por_clave` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `sp_obtener_comprobante_por_clave`(
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
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `sp_obtener_comprobante_por_factura` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `sp_obtener_comprobante_por_factura`(
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
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `sp_obtener_productos_mas_vendidos` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `sp_obtener_productos_mas_vendidos`(IN p_limite INT)
BEGIN
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
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `sp_obtener_ventas_por_mes` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `sp_obtener_ventas_por_mes`(IN p_meses INT)
BEGIN
    DECLARE v_fecha_inicio DATE;
    SET v_fecha_inicio = DATE_SUB(CURDATE(), INTERVAL p_meses MONTH);
    
    SELECT 
        DATE_FORMAT(f.create_date, '%b %Y') AS mes,
        COALESCE(SUM(f.total), 0) AS total_ventas
    FROM factura f
    WHERE f.create_date >= v_fecha_inicio
        AND f.create_date <= CURDATE()
    GROUP BY DATE_FORMAT(f.create_date, '%Y-%m'),
             DATE_FORMAT(f.create_date, '%b %Y')
    ORDER BY DATE_FORMAT(f.create_date, '%Y-%m') ASC;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `SP_PRODUCTOS_MAS_VENDIDOS` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `SP_PRODUCTOS_MAS_VENDIDOS`(IN p_limite INT)
BEGIN
    
    SELECT 
        p.descripcion AS nombre_producto,
        COALESCE(SUM(lf.cantidad), 0) AS cantidad_vendida
    FROM 
        producto p
    LEFT JOIN 
        linea_factura lf ON p.id_producto = lf.producto_id
    LEFT JOIN
        factura f ON lf.factura_id = f.id_factura
    WHERE
        p.active = TRUE
    GROUP BY 
        p.id_producto, 
        p.descripcion
    HAVING 
        cantidad_vendida > 0
    ORDER BY 
        cantidad_vendida DESC
    LIMIT p_limite;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `sp_resumen_comprobantes_empresa` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `sp_resumen_comprobantes_empresa`(
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
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `SP_VENTAS_POR_CLIENTE_TOP` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE  PROCEDURE `SP_VENTAS_POR_CLIENTE_TOP`(IN p_limite INT)
BEGIN
    
    SELECT 
        c.nombre AS nombre_cliente,
        COUNT(f.id_factura) AS total_facturas,
        COALESCE(SUM(f.total), 0) AS total_ventas
    FROM 
        cliente c
    INNER JOIN 
        factura f ON c.id_cliente = f.id_cliente


    GROUP BY 
        c.id_cliente, 
        c.nombre
    HAVING 
        total_facturas > 0
    ORDER BY 
        total_ventas DESC
    LIMIT p_limite;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-08 21:59:55
