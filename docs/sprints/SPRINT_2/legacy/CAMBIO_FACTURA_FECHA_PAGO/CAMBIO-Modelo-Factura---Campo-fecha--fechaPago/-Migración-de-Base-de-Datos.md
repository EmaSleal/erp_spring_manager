## 🗄️ Migración de Base de Datos

### **Script SQL Creado**
📄 **Archivo:** `docs/base de datos/MIGRATION_FACTURA_FECHA_PAGO.sql`

### **Pasos de la Migración**

#### 1. Agregar campo `fecha_pago`
```sql
ALTER TABLE factura
ADD COLUMN fecha_pago DATE NULL 
AFTER fecha_entrega;
```

#### 2. Migrar datos existentes
```sql
-- Facturas con fecha_entrega: fecha_pago = fecha_entrega + 7 días
UPDATE factura
SET fecha_pago = DATE_ADD(fecha_entrega, INTERVAL 7 DAY)
WHERE fecha_entrega IS NOT NULL;

-- Facturas sin fecha_entrega: fecha_pago = create_date + 7 días
UPDATE factura
SET fecha_pago = DATE_ADD(DATE(create_date), INTERVAL 7 DAY)
WHERE fecha_entrega IS NULL AND create_date IS NOT NULL;
```

#### 3. Eliminar campo antiguo
```sql
ALTER TABLE factura
DROP COLUMN fecha;
```

#### 4. Crear índices (opcional, recomendado)
```sql
-- Para búsquedas de facturas vencidas:
CREATE INDEX idx_factura_pago_vencido 
ON factura(tipo_factura, fecha_pago, entregado);

-- Para búsquedas por fecha de pago:
CREATE INDEX idx_factura_fecha_pago 
ON factura(fecha_pago);
```

---

