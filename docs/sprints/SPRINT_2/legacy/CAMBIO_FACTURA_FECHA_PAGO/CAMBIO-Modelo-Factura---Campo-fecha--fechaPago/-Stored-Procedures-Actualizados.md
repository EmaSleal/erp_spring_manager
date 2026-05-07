## 📝 Stored Procedures Actualizados

**Nota:** Los SPs fueron actualizados MANUALMENTE por el usuario antes de esta migración.

### **Afectados:**

#### 1. **CrearFactura**
**Antes:**
```sql
INSERT INTO factura (id_cliente, fecha, fecha_entrega, ...)
VALUES (pIdCliente, NOW(), pFechaEntrega, ...);
```

**Después:**
```sql
INSERT INTO factura (id_cliente, fecha_pago, fecha_entrega, ...)
VALUES (pIdCliente, DATE_ADD(pFechaEntrega, INTERVAL 7 DAY), pFechaEntrega, ...);
```

#### 2. **ObtenerFacturaCompleta**
**Antes:**
```sql
SELECT f.fecha AS fechaCreacion, ...
```

**Después:**
```sql
SELECT f.create_date AS fechaCreacion, f.fecha_pago, ...
```

---

