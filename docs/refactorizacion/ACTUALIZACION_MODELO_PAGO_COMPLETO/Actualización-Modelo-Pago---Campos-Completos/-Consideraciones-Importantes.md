## ⚠️ Consideraciones Importantes

### 1. **Migración de Datos Existentes**

Si ya existen pagos en la BD, necesitan actualización:

```sql
-- Generar números de pago para registros existentes
SET @counter = 0;
UPDATE pagos 
SET numeroPago = CONCAT(
    'PAG-', 
    DATE_FORMAT(fechaPago, '%Y%m%d'), 
    '-',
    LPAD(@counter := @counter + 1, 4, '0')
)
WHERE numeroPago IS NULL
ORDER BY fechaPago, idPago;

-- Asignar cliente desde factura
UPDATE pagos p
JOIN factura f ON p.idFactura = f.id_factura
SET p.clienteId = f.idCliente
WHERE p.clienteId IS NULL;

-- Asignar tipo de pago (por defecto TOTAL)
UPDATE pagos SET tipoPago = 'TOTAL' WHERE tipoPago IS NULL;
```

### 2. **Restricciones de FK**

La columna `clienteId` es `NOT NULL` pero se agregará después de datos existentes. Asegurar que:

1. Todos los pagos existentes tengan `idFactura`
2. Se ejecute el UPDATE de migración antes que Hibernate aplique `NOT NULL`

### 3. **Renombrado de Columnas**

Hibernate NO renombra columnas automáticamente. Si hay datos en producción:

**Opción A**: Migración manual
```sql
ALTER TABLE pagos CHANGE referencia referenciaBancaria VARCHAR(100);
ALTER TABLE pagos CHANGE notas observaciones VARCHAR(1000);
```

**Opción B**: Mantener nombres antiguos en mapeo (no recomendado)

---

