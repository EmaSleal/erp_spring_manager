## 📊 Campos Agregados

### 1. Serie (`serie`)

**Descripción:** Prefijo o serie de la factura  
**Tipo:** VARCHAR(10)  
**Requerido:** No (opcional)  
**Ejemplo:** "F001", "B002", "FA01"  
**Comportamiento:**
- Si el usuario lo deja vacío, se genera automáticamente desde configuración
- Si el usuario lo ingresa, se respeta el valor manual

**Ubicación en UI:**
- ✅ Formulario: Paso 1, fila 2 (columna izquierda)
- ✅ Tabla listado: Columna "N° Factura" (subtítulo)
- ✅ Modal detalle: Sección "Información General"

---

### 2. Número de Factura (`numeroFactura`)

**Descripción:** Número único e identificador de la factura  
**Tipo:** VARCHAR(50)  
**Requerido:** No (se auto-genera si está vacío)  
**Ejemplo:** "FA01-00123", "001-2025-00456"  
**Comportamiento:**
- Si se deja vacío, el sistema lo genera automáticamente
- Formato: `{serie}-{numero_secuencial}`
- Ejemplo: "FA01-00001", "FA01-00002", etc.

**Ubicación en UI:**
- ✅ Formulario: Paso 1, fila 2 (columna derecha)
- ✅ Tabla listado: **Nueva columna** destacada en azul
- ✅ Modal detalle: Sección "Información General" (bold)

---

### 3. Fecha de Pago (`fechaPago`)

**Descripción:** Fecha límite para que el cliente pague la factura  
**Tipo:** DATE  
**Requerido:** No  
**Ejemplo:** "2025-10-27"  
**Comportamiento:**
- Se calcula automáticamente: `fecha_entrega + 7 días`
- El usuario puede modificarla manualmente si lo desea
- Útil para recordatorios de pago

**Ubicación en UI:**
- ✅ Formulario: Paso 1, fila 3 (columna derecha)
- ✅ Modal detalle: Sección "Información General"

---

