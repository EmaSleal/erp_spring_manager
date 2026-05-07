## 🧪 Testing Necesario

### **Después de Ejecutar la Migración SQL:**

#### 1. **Verificar estructura de tabla**
```sql
DESCRIBE factura;
-- Debe mostrar: fecha_pago (DATE)
-- NO debe mostrar: fecha
```

#### 2. **Verificar datos migrados**
```sql
SELECT 
    id_factura,
    numero_factura,
    fecha_entrega,
    fecha_pago,
    DATEDIFF(fecha_pago, fecha_entrega) AS dias_credito
FROM factura
LIMIT 20;
-- Esperado: dias_credito = 7 para la mayoría
```

#### 3. **Probar creación de factura**
- Crear nueva factura con fecha_entrega = 2025-10-20
- Verificar que fecha_pago = 2025-10-27 (automático)

#### 4. **Probar edición de factura**
- Editar fecha_entrega
- Verificar que fecha_pago se recalcula

---

