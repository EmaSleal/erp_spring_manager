## 📦 FASE 7: Migración de Datos - Campos Factura

**Estado:** ❌ **PENDIENTE**  
**Prioridad:** 🔴 **CRÍTICA**  
**Tiempo estimado:** 2 horas

### Tareas:

#### 7.1 Crear Script SQL de Migración
- [ ] Crear archivo: `docs/base de datos/MIGRATION_FACTURA_CONDICIONES_VENTA.sql`
- [ ] Agregar campo `condicion_venta` VARCHAR(2) DEFAULT '01'
- [ ] Agregar campo `medio_pago` VARCHAR(2) DEFAULT '01'
- [ ] Agregar campo `plazo_credito` INT NULL
- [ ] Agregar campo `codigo_moneda` VARCHAR(3) DEFAULT 'CRC'
- [ ] Agregar campo `tipo_cambio` DECIMAL(10,5) DEFAULT 1.00000
- [ ] Crear índices

**Script SQL:**
```sql
-- MIGRATION_FACTURA_CONDICIONES_VENTA.sql
ALTER TABLE factura 
ADD COLUMN condicion_venta VARCHAR(2) DEFAULT '01' COMMENT '01=Contado, 02=Crédito, 03=Consignación, 04=Apartado, 99=Otros';

ALTER TABLE factura 
ADD COLUMN medio_pago VARCHAR(2) DEFAULT '01' COMMENT '01=Efectivo, 02=Tarjeta, 03=Cheque, 04=Transferencia, 05=Recaudado por terceros, 99=Otros';

ALTER TABLE factura 
ADD COLUMN plazo_credito INT NULL COMMENT 'Días de crédito (obligatorio si condicion_venta=02)';

ALTER TABLE factura 
ADD COLUMN codigo_moneda VARCHAR(3) DEFAULT 'CRC' COMMENT 'CRC, USD, EUR';

ALTER TABLE factura 
ADD COLUMN tipo_cambio DECIMAL(10,5) DEFAULT 1.00000 COMMENT 'Tipo de cambio respecto a CRC';

-- Índices
CREATE INDEX idx_factura_condicion_venta ON factura(condicion_venta);
CREATE INDEX idx_factura_medio_pago ON factura(medio_pago);
CREATE INDEX idx_factura_moneda ON factura(codigo_moneda);

-- Validar
SHOW CREATE TABLE factura;
```

#### 7.2 Actualizar Entidad Factura
- [ ] Abrir: `modules/facturacion/model/Factura.java`
- [ ] Agregar campo `condicionVenta` (enum)
- [ ] Agregar campo `medioPago` (enum)
- [ ] Agregar campo `plazoCredito` (Integer, nullable)
- [ ] Agregar campo `codigoMoneda`
- [ ] Agregar campo `tipoCambio`

#### 7.3 Crear Enums de Factura
- [ ] Crear `CondicionVenta.java` enum (CONTADO=01, CREDITO=02, etc.)
- [ ] Crear `MedioPago.java` enum (EFECTIVO=01, TARJETA=02, etc.)

#### 7.4 Actualizar Formulario Factura
- [ ] Abrir: `templates/modules/facturacion/add-form.html`
- [ ] Agregar select para `condicionVenta`
- [ ] Agregar select para `medioPago`
- [ ] Agregar input `plazoCredito` (visible solo si condicion=CREDITO)
- [ ] Agregar select `codigoMoneda` (CRC, USD, EUR)
- [ ] Agregar input `tipoCambio` (visible solo si moneda!=CRC)

#### 7.5 Lógica de Validación
- [ ] Validar que `plazoCredito` sea obligatorio si `condicionVenta=02`
- [ ] Validar que `tipoCambio` sea > 0 si moneda != CRC
- [ ] Calcular totales en moneda seleccionada

---

