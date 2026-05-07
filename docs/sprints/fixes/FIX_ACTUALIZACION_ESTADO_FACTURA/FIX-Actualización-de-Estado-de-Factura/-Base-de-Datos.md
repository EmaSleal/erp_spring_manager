## 🗄️ Base de Datos

### Stored Procedures utilizados:

#### `sp_actualizar_linea_factura`
```sql
DELIMITER $$
CREATE DEFINER=`m4n0`@`%` PROCEDURE `sp_actualizar_linea_factura`(
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
    -- Actualiza línea
    UPDATE linea_factura SET ...
    
    -- Llama a ActualizarTotalFactura
    CALL ActualizarTotalFactura(p_id_factura);
END$$
DELIMITER ;
```

#### `ActualizarTotalFactura`
```sql
DELIMITER $$
CREATE DEFINER=`m4n0`@`localhost` PROCEDURE `ActualizarTotalFactura`(
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
END$$
DELIMITER ;
```

**Nota:** Ninguno de estos SP actualiza el campo `entregado`, por eso se necesitó el endpoint adicional.

---

