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
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
/*!50032 DROP TRIGGER IF EXISTS trg_after_insert_usuario */;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017  /*!50003 TRIGGER `trg_after_insert_usuario` AFTER INSERT ON `usuario` FOR EACH ROW BEGIN
    
    IF NEW.activo = true THEN
        
        CALL sp_crear_preferencias_usuario(NEW.id_usuario);
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
/*!50032 DROP TRIGGER IF EXISTS trg_after_update_usuario */;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017  /*!50003 TRIGGER `trg_after_update_usuario` AFTER UPDATE ON `usuario` FOR EACH ROW BEGIN
    DECLARE v_tiene_email_nuevo BOOLEAN;
    DECLARE v_tiene_telefono_nuevo BOOLEAN;
    DECLARE v_tiene_email_viejo BOOLEAN;
    DECLARE v_tiene_telefono_viejo BOOLEAN;
    
    
    SET v_tiene_email_nuevo = (NEW.email IS NOT NULL AND NEW.email != '');
    SET v_tiene_email_viejo = (OLD.email IS NOT NULL AND OLD.email != '');
    
    
    SET v_tiene_telefono_nuevo = (NEW.telefono IS NOT NULL AND NEW.telefono != '');
    SET v_tiene_telefono_viejo = (OLD.telefono IS NOT NULL AND OLD.telefono != '');
    
    
    
    
    IF v_tiene_email_nuevo AND NOT v_tiene_email_viejo THEN
        
        
        INSERT IGNORE INTO preferencia_notificacion (
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
            NEW.id_usuario,
            'FACTURA_VENCIDA',
            'EMAIL',
            true,
            false,
            'INMEDIATA',
            false,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
        
        
        INSERT IGNORE INTO preferencia_notificacion (
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
            NEW.id_usuario,
            'STOCK_BAJO',
            'EMAIL',
            true,
            false,
            'DIARIA',
            false,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
        
    END IF;
    
    
    
    
    IF v_tiene_telefono_nuevo AND NOT v_tiene_telefono_viejo THEN
        
        
        INSERT IGNORE INTO preferencia_notificacion (
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
            NEW.id_usuario,
            'FACTURA_VENCIDA',
            'WHATSAPP',
            true,
            false,
            'INMEDIATA',
            false,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
        
    END IF;
    
END */;;
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

-- Dump completed on 2026-06-08 21:59:57
