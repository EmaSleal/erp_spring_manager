## 🧾 Configuración de Facturación

### Información General

Configure los parámetros de facturación que se aplicarán automáticamente a todas las facturas generadas en el sistema.

### Campos Disponibles

#### 1. Numeración de Facturas

| Campo | Descripción | Obligatorio | Ejemplo |
|-------|-------------|-------------|---------|
| **Serie de Factura** | Prefijo de las facturas | ✅ Sí | "F001" |
| **Número Actual** | Último número usado | ✅ Sí | "125" |
| **Prefijo** | Texto antes del número | ❌ No | "FAC-" |
| **Sufijo** | Texto después del número | ❌ No | "-2026" |

**Ejemplo de numeración completa:**
```
Prefijo + Serie + Número + Sufijo
"FAC-" + "F001" + "00125" + "-2026"
= FAC-F001-00125-2026
```

#### 2. Impuestos

| Campo | Descripción | Obligatorio | Ejemplo |
|-------|-------------|-------------|---------|
| **IGV/IVA (%)** | Porcentaje de impuesto | ✅ Sí | "18.00" (Perú) |
| **Incluir IGV** | Mostrar IGV en facturas | ✅ Sí | Sí / No |

**Cálculo automático:**
```
Subtotal:  S/ 1,000.00
IGV (18%):  S/   180.00
────────────────────────
Total:     S/ 1,180.00
```

#### 3. Términos y Condiciones

| Campo | Descripción | Obligatorio | Ejemplo |
|-------|-------------|-------------|---------|
| **Términos** | Condiciones de venta | ❌ No | "Pago a 30 días..." |
| **Pie de Página** | Texto al final | ❌ No | "Gracias por su compra" |

#### 4. Información de Pago

| Campo | Descripción | Obligatorio | Ejemplo |
|-------|-------------|-------------|---------|
| **Cuenta Bancaria** | Número de cuenta | ❌ No | "0011-0123-456789" |
| **Banco** | Nombre del banco | ❌ No | "Banco de Crédito BCP" |
| **CCI** | Código interbancario | ❌ No | "002-011-001234567890-12" |

### Procedimiento: Configurar Facturación

#### Paso 1: Acceder a la Pestaña
1. Haga clic en la pestaña **"Facturación"**
2. Se mostrarán los parámetros actuales

#### Paso 2: Configurar Numeración

1. **Serie de Factura:**
   - Ingrese el código de serie (ej: "F001", "B001")
   - Formato común: Letra + 3 dígitos

2. **Número Actual:**
   - Ingrese el último número de factura emitida
   - El sistema incrementará automáticamente

3. **Prefijo y Sufijo (opcional):**
   - Agregue texto personalizado
   - Se añadirá antes/después del número

**Vista Previa:**
```
┌──────────────────────────────────────┐
│  Vista Previa del Número:           │
│  FAC-F001-00126-2026                │
└──────────────────────────────────────┘
```

#### Paso 3: Configurar Impuestos

1. **IGV/IVA:**
   - Ingrese el porcentaje (ej: 18.00 para Perú)
   - Use punto decimal (no coma)
   - Rango válido: 0.00 - 100.00

2. **Incluir IGV:**
   - ✅ Marque para mostrar en facturas
   - ❌ Desmarque para ocultar

#### Paso 4: Términos y Condiciones

**Recomendaciones:**
- Sea claro y conciso
- Incluya políticas de pago
- Especifique garantías o devoluciones
- Mencione penalidades por mora (si aplica)

**Ejemplo de términos:**
```
- Pago a 30 días desde la fecha de emisión
- Intereses moratorios: 2% mensual
- No se aceptan devoluciones después de 7 días
- Productos sujetos a disponibilidad
```

#### Paso 5: Información Bancaria

Complete **solo si desea** que aparezca en las facturas:

1. Número de cuenta bancaria
2. Nombre del banco
3. CCI (Código de Cuenta Interbancario)

#### Paso 6: Guardar Configuración

1. Revise todos los parámetros
2. Haga clic en **"Guardar Configuración"**
3. Espere el mensaje de confirmación

### Mensajes del Sistema

#### ✅ Mensajes de Éxito
- **"Configuración de facturación actualizada exitosamente"**
- **"Configuración de facturación creada exitosamente"**

#### ❌ Mensajes de Error
- **"El porcentaje de IGV debe estar entre 0 y 100"**
- **"La serie de factura es obligatoria"**
- **"El número actual debe ser mayor a 0"**

### Ejemplo de Factura Generada

Con esta configuración:
```yaml
Serie: F001
Número Actual: 125
Prefijo: FAC-
Sufijo: -2026
IGV: 18%
```

Se generará:
```
┌─────────────────────────────────────────────────┐
│  FACTURA: FAC-F001-00126-2026                  │
├─────────────────────────────────────────────────┤
│  Comercial ABC S.A.C.                          │
│  RUC: 20123456789                              │
│                                                 │
│  Subtotal:        S/ 1,000.00                  │
│  IGV (18%):       S/   180.00                  │
│  ─────────────────────────────                  │
│  Total:           S/ 1,180.00                  │
│                                                 │
│  Pago a 30 días desde la fecha de emisión     │
│                                                 │
│  Banco: Banco de Crédito BCP                   │
│  Cuenta: 0011-0123-456789                      │
└─────────────────────────────────────────────────┘
```

---

