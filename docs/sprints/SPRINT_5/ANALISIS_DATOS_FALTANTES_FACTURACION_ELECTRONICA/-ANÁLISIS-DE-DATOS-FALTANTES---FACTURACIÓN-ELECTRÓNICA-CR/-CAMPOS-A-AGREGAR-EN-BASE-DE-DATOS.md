## 🛠️ CAMPOS A AGREGAR EN BASE DE DATOS

### 🏢 Tabla `empresa`

```sql
-- Campos CRÍTICOS para facturación electrónica
ALTER TABLE empresa ADD COLUMN codigo_actividad_economica VARCHAR(6) COMMENT 'Código de actividad económica inscrita en Hacienda';
ALTER TABLE empresa ADD COLUMN tipo_identificacion VARCHAR(2) DEFAULT '02' COMMENT '01=Física, 02=Jurídica, 03=DIMEX, 04=NITE';
ALTER TABLE empresa ADD COLUMN proveedor_sistemas VARCHAR(20) DEFAULT '2100042005' COMMENT 'Cédula del proveedor de sistemas de facturación';

-- Ubicación geográfica CR
ALTER TABLE empresa ADD COLUMN provincia VARCHAR(2) COMMENT 'Código provincia CR (1-7)';
ALTER TABLE empresa ADD COLUMN canton VARCHAR(2) COMMENT 'Código cantón';
ALTER TABLE empresa ADD COLUMN distrito VARCHAR(2) COMMENT 'Código distrito';
ALTER TABLE empresa ADD COLUMN barrio VARCHAR(100) COMMENT 'Nombre del barrio';
ALTER TABLE empresa ADD COLUMN otras_senas VARCHAR(300) COMMENT 'Dirección descriptiva';

-- Índices para búsqueda
CREATE INDEX idx_empresa_codigo_actividad ON empresa(codigo_actividad_economica);
```

### 👥 Tabla `cliente`

```sql
-- Campos CRÍTICOS para facturación electrónica
ALTER TABLE cliente ADD COLUMN tipo_identificacion VARCHAR(2) DEFAULT '01' COMMENT '01=Física, 02=Jurídica, 03=DIMEX, 04=NITE';
ALTER TABLE cliente ADD COLUMN codigo_actividad_economica VARCHAR(6) COMMENT 'Código actividad económica (opcional para créditos/gastos)';

-- Ubicación geográfica CR
ALTER TABLE cliente ADD COLUMN provincia VARCHAR(2) COMMENT 'Código provincia CR';
ALTER TABLE cliente ADD COLUMN canton VARCHAR(2) COMMENT 'Código cantón';
ALTER TABLE cliente ADD COLUMN distrito VARCHAR(2) COMMENT 'Código distrito';
ALTER TABLE cliente ADD COLUMN otras_senas VARCHAR(300) COMMENT 'Dirección descriptiva';

-- Índices
CREATE INDEX idx_cliente_tipo_identificacion ON cliente(tipo_identificacion);
CREATE INDEX idx_cliente_identificacion ON cliente(identificacion);
```

### 🧾 Tabla `factura`

```sql
-- Campos CRÍTICOS para facturación electrónica
ALTER TABLE factura ADD COLUMN condicion_venta VARCHAR(2) DEFAULT '01' COMMENT '01=Contado, 02=Crédito, etc';
ALTER TABLE factura ADD COLUMN medio_pago VARCHAR(2) DEFAULT '01' COMMENT '01=Efectivo, 02=Tarjeta, 03=Cheque, 04=Transferencia';
ALTER TABLE factura ADD COLUMN plazo_credito INT COMMENT 'Días de crédito si condicion_venta=02';
ALTER TABLE factura ADD COLUMN codigo_moneda VARCHAR(3) DEFAULT 'CRC' COMMENT 'CRC, USD, EUR';
ALTER TABLE factura ADD COLUMN tipo_cambio DECIMAL(10,5) DEFAULT 1.00000 COMMENT 'Tipo de cambio respecto a CRC';

-- Índices
CREATE INDEX idx_factura_condicion_venta ON factura(condicion_venta);
CREATE INDEX idx_factura_medio_pago ON factura(medio_pago);
```

### 📦 Tabla `producto`

```sql
-- Campos CRÍTICOS para facturación electrónica
ALTER TABLE producto ADD COLUMN codigo_cabys VARCHAR(13) COMMENT 'Código CABYS de 13 dígitos (obligatorio Hacienda CR)';
ALTER TABLE producto ADD COLUMN unidad_medida VARCHAR(20) DEFAULT 'Unid' COMMENT 'Kg, Unid, Litro, Metro, etc';
ALTER TABLE producto ADD COLUMN codigo_impuesto VARCHAR(2) DEFAULT '01' COMMENT '01=IVA, 02=Selectivo, etc';
ALTER TABLE producto ADD COLUMN codigo_tarifa_iva VARCHAR(2) DEFAULT '08' COMMENT '08=13%, 10=0% (exento)';
ALTER TABLE producto ADD COLUMN tarifa_impuesto DECIMAL(5,2) DEFAULT 13.00 COMMENT 'Porcentaje de impuesto';
ALTER TABLE producto ADD COLUMN tipo_tarifa VARCHAR(20) COMMENT 'Gravado, Exento, Exonerado';

-- Índices
CREATE INDEX idx_producto_cabys ON producto(codigo_cabys);
CREATE INDEX idx_producto_tarifa ON producto(codigo_tarifa_iva);
```

---

