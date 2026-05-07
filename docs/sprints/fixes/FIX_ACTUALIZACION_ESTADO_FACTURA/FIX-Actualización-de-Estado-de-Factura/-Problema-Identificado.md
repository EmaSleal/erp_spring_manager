## 🔴 Problema Identificado

Cuando se editaba una factura y se cambiaba el estado de "Pendiente" a "Entregado" (o viceversa), el sistema mostraba el mensaje de éxito pero **el estado no se actualizaba en la base de datos**.

### Causa Raíz

El flujo de actualización de factura solo actualizaba:
1. ✅ Las líneas de productos (cantidad, precio, subtotal)
2. ✅ El total de la factura (calculado por `ActualizarTotalFactura` SP)
3. ❌ **NO actualizaba el campo `entregado` de la tabla `factura`**

El stored procedure `sp_actualizar_linea_factura` solo modifica la tabla `linea_factura` y llama a `ActualizarTotalFactura`, pero ninguno de estos SP modifica el campo `entregado`.

---

