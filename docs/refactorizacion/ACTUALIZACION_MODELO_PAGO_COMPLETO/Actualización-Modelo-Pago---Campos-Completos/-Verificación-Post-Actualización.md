## 🔍 Verificación Post-Actualización

### 1. Compilación
```bash
mvn clean compile
# ✅ SUCCESS
```

### 2. Hibernate DDL Update
Al iniciar la aplicación, Hibernate actualizará la tabla `pagos`:

```sql
-- Se agregarán estas columnas automáticamente:
ALTER TABLE pagos ADD COLUMN numeroPago VARCHAR(20);
ALTER TABLE pagos ADD COLUMN clienteId BIGINT;
ALTER TABLE pagos ADD COLUMN tipoPago VARCHAR(20);
ALTER TABLE pagos ADD COLUMN banco VARCHAR(100);
ALTER TABLE pagos ADD COLUMN cuentaBancaria VARCHAR(50);
ALTER TABLE pagos ADD COLUMN comprobanteUrl VARCHAR(255);
ALTER TABLE pagos ADD COLUMN anuladoPor INT;
ALTER TABLE pagos ADD COLUMN anuladoEn TIMESTAMP;
ALTER TABLE pagos ADD COLUMN motivoAnulacion VARCHAR(1000);

-- Se renombrarán/ajustarán estas columnas:
ALTER TABLE pagos CHANGE referencia referenciaBancaria VARCHAR(100);
ALTER TABLE pagos CHANGE notas observaciones VARCHAR(1000);

-- Se agregarán índices:
CREATE UNIQUE INDEX idx_pago_numero ON pagos(numeroPago);
CREATE INDEX idx_pago_cliente ON pagos(clienteId);
CREATE INDEX idx_pago_tipo ON pagos(tipoPago);

-- Se agregará FK:
ALTER TABLE pagos ADD CONSTRAINT fk_pago_cliente 
    FOREIGN KEY (clienteId) REFERENCES cliente(idCliente);
```

### 3. Verificar en Base de Datos
```sql
-- Después de iniciar la aplicación:
DESCRIBE pagos;
SHOW INDEXES FROM pagos;
SHOW CREATE TABLE pagos;
```

---

