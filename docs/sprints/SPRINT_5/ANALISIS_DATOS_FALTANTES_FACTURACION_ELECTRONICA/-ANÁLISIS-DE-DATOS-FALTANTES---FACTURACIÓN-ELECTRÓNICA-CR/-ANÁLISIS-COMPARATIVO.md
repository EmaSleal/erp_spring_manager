## 🔍 ANÁLISIS COMPARATIVO

### 1️⃣ DATOS DEL EMISOR (Empresa)

#### ✅ Datos que YA TENEMOS:
```java
// Entidad Empresa actual
- nombreEmpresa         → <Emisor><Nombre>
- nombreComercial       → <Emisor><NombreComercial>
- ruc                   → <Emisor><Identificacion><Numero>
- direccion             → <Emisor><Ubicacion><OtrasSenas>
- telefono              → <Emisor><Telefono> (si se agrega campo)
- email                 → <Emisor><CorreoElectronico>
```

#### ❌ Datos que NOS FALTAN:

| Campo Requerido | XML Ejemplo | Estado | Prioridad |
|----------------|-------------|---------|-----------|
| **Código Actividad Económica** | `<CodigoActividadEmisor>4773.0</CodigoActividadEmisor>` | ❌ FALTA | 🔴 CRÍTICO |
| **Tipo de Identificación** | `<Tipo>01</Tipo>` (01=Física, 02=Jurídica) | ❌ FALTA | 🔴 CRÍTICO |
| **Proveedor de Sistemas** | `<ProveedorSistemas>2100042005</ProveedorSistemas>` | ❌ FALTA | 🔴 CRÍTICO |
| **Ubicación detallada:** |  |  |  |
| - Provincia | `<Provincia>2</Provincia>` | ❌ FALTA | 🟠 ALTA |
| - Cantón | `<Canton>01</Canton>` | ❌ FALTA | 🟠 ALTA |
| - Distrito | `<Distrito>04</Distrito>` | ❌ FALTA | 🟠 ALTA |
| - Barrio | `<Barrio>EL ROBLE</Barrio>` | ❌ FALTA | 🟡 MEDIA |

**Tipos de Identificación (según XSD):**
- `01` = Cédula Física
- `02` = Cédula Jurídica
- `03` = DIMEX (Extranjeros)
- `04` = NITE (Extranjeros sin cédula)

---

### 2️⃣ DATOS DEL RECEPTOR (Cliente)

#### ✅ Datos que YA TENEMOS:
```java
// Entidad Cliente actual
- nombre                 → <Receptor><Nombre>
- email                  → <Receptor><CorreoElectronico>
- identificacion         → <Receptor><Identificacion><Numero>
```

#### ❌ Datos que NOS FALTAN:

| Campo Requerido | XML Ejemplo | Estado | Prioridad |
|----------------|-------------|---------|-----------|
| **Tipo de Identificación** | `<Tipo>02</Tipo>` | ❌ FALTA | 🔴 CRÍTICO |
| **Código Actividad Económica** | `<CodigoActividadReceptor>1080.0</CodigoActividadReceptor>` | ⚠️ OPCIONAL | 🟢 BAJA |
| **Ubicación detallada:** | | | |
| - Provincia | `<Provincia>2</Provincia>` | ❌ FALTA | 🟡 MEDIA |
| - Cantón | `<Canton>01</Canton>` | ❌ FALTA | 🟡 MEDIA |
| - Distrito | `<Distrito>13</Distrito>` | ❌ FALTA | 🟡 MEDIA |
| - Otras Señas | `<OtrasSenas>ALAJUELA</OtrasSenas>` | ❌ FALTA | 🟡 MEDIA |

---

### 3️⃣ DATOS DE LA FACTURA

#### ✅ Datos que YA TENEMOS:
```java
// Entidad Factura actual
- numeroFactura         → <NumeroConsecutivo>
- fechaEmision (createDate) → <FechaEmision>
- subtotal              → <SubTotal>
- igv                   → <TotalImpuesto>
- total                 → <TotalComprobante>
```

#### ❌ Datos que NOS FALTAN:

| Campo Requerido | XML Ejemplo | Estado | Prioridad |
|----------------|-------------|---------|-----------|
| **Condición de Venta** | `<CondicionVenta>01</CondicionVenta>` | ❌ FALTA | 🔴 CRÍTICO |
| **Medio de Pago** | `<TipoMedioPago>01</TipoMedioPago>` | ❌ FALTA | 🔴 CRÍTICO |
| **Plazo de Crédito** | Si condición = 02 (crédito) | ⚠️ CONDICIONAL | 🟠 ALTA |
| **Código Moneda** | `<CodigoMoneda>CRC</CodigoMoneda>` | ❌ FALTA | 🟠 ALTA |
| **Tipo de Cambio** | `<TipoCambio>1.00000</TipoCambio>` | ❌ FALTA | 🟠 ALTA |

**Condiciones de Venta (según XSD):**
- `01` = Contado
- `02` = Crédito
- `03` = Consignación
- `04` = Apartado
- `05` = Arrendamiento con opción de compra
- `06` = Arrendamiento en función financiera
- `99` = Otros

**Medios de Pago:**
- `01` = Efectivo
- `02` = Tarjeta
- `03` = Cheque
- `04` = Transferencia
- `05` = Recaudado por terceros
- `99` = Otros

---

### 4️⃣ DATOS DE LÍNEAS DE DETALLE (Productos)

#### ✅ Datos que YA TENEMOS:
```java
// Entidad LineaFactura + Producto actual
- numeroLinea           → <NumeroLinea>
- cantidad              → <Cantidad>
- precioUnitario        → <PrecioUnitario>
- subtotal              → <MontoTotal>
- producto.descripcion  → <Detalle>
```

#### ❌ Datos que NOS FALTAN:

| Campo Requerido | XML Ejemplo | Estado | Prioridad |
|----------------|-------------|---------|-----------|
| **Código CABYS** | `<CodigoCABYS>3532201060000</CodigoCABYS>` | ❌ FALTA | 🔴 CRÍTICO |
| **Unidad de Medida** | `<UnidadMedida>Kg</UnidadMedida>` | ❌ FALTA | 🔴 CRÍTICO |
| **Código de Impuesto** | `<Codigo>01</Codigo>` (01=IVA) | ❌ FALTA | 🔴 CRÍTICO |
| **Código Tarifa IVA** | `<CodigoTarifaIVA>10</CodigoTarifaIVA>` | ❌ FALTA | 🔴 CRÍTICO |
| **Tarifa (%)** | `<Tarifa>0</Tarifa>` o 13 | ❌ FALTA | 🔴 CRÍTICO |
| **Naturaleza del Descuento** | Si aplica descuento | ⚠️ CONDICIONAL | 🟡 MEDIA |

**Códigos Tarifa IVA importantes:**
- `10` = 0% (Exento/Productos Básicos)
- `08` = 13% (Tarifa general CR)
- `01` = 1%
- `02` = 2%
- `04` = 4%

---

