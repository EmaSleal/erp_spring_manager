# CAMBIO: Modelo Factura - Campo `fecha` → `fechaPago`

## 📋 Información del Cambio

**Fecha:** 13 de octubre de 2025  
**Sprint:** Sprint 2 - Fase 5 (Notificaciones)  
**Punto:** 5.3.3 (Recordatorio de pago)  
**Tipo:** Refactorización + Nueva funcionalidad

---

## 🔄 Cambios Realizados

### **Modelo Factura.java**

#### Campo ELIMINADO ❌
```java
private Timestamp fecha;  // Fecha de emisión (redundante con createDate)
```

**Razón de eliminación:**
- Redundante con `createDate` (auditoría)
- Los stored procedures fueron actualizados manualmente
- No aportaba valor de negocio único

#### Campo AGREGADO ✅
```java
@Column(name = "fechaPago")
private Date fechaPago;  // Fecha límite de pago
```

**Propósito:**
- Define cuándo debe pagar el cliente
- Base para recordatorios de pago (Punto 5.3.3)
- Cálculo: `fechaEntrega + días de crédito` (ej: 7 días)

---

## 📊 Comparación Antes vs Después

### **Antes**
```java
@Entity
public class Factura {
    private Timestamp fecha;           // ❌ Redundante
    private Date fechaEntrega;         // ✅ Cuándo se entrega
    private Timestamp createDate;      // ✅ Auditoría
}
```

### **Después**
```java
@Entity
public class Factura {
    private Date fechaEntrega;         // ✅ Cuándo se entrega
    private Date fechaPago;            // ✅ Cuándo debe pagar (NUEVO)
    private Timestamp createDate;      // ✅ Auditoría
}
```

---

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

## 🎯 Impacto en el Código

### **Archivos Modificados**

#### 1. **Factura.java** ✅
```java
// Antes
private Timestamp fecha;

// Después
@Column(name = "fechaPago")
private Date fechaPago;
```

#### 2. **Vistas HTML** (Pendiente de actualizar)
- `facturas/facturas.html` - Tabla de facturas
- `facturas/form.html` - Formulario crear/editar
- `facturas/detalle.html` - Vista detallada

**Cambios necesarios:**
- Agregar columna "Fecha de Pago" en tabla
- Campo `fechaPago` en formulario (auto-calculado o editable)
- Mostrar fecha de pago en detalle

#### 3. **JavaScript** (Pendiente de actualizar)
- `facturas.js` - Lógica del formulario

**Cambios necesarios:**
- Calcular `fechaPago` automáticamente al seleccionar `fechaEntrega`
- Validar que `fechaPago >= fechaEntrega`

---

## 🔧 Implementación Futura (Punto 5.3.3)

### **Lógica de Recordatorios**

```java
@Scheduled(cron = "0 0 9 * * ?")  // Cada día a las 9 AM
public void enviarRecordatoriosPago() {
    LocalDate hoy = LocalDate.now();
    
    // Buscar facturas vencidas:
    // - tipo_factura = PENDIENTE
    // - fecha_pago < hoy
    // - entregado = true
    // - cliente.email != null
    
    List<Factura> facturasVencidas = facturaRepository
        .findFacturasConPagoVencido(Date.valueOf(hoy));
    
    for (Factura factura : facturasVencidas) {
        emailService.enviarRecordatorioPago(factura);
    }
}
```

### **Query en FacturaRepository**

```java
@Query("SELECT f FROM Factura f " +
       "WHERE f.tipoFactura = 'PENDIENTE' " +
       "AND f.fechaPago < :fechaHoy " +
       "AND f.entregado = true " +
       "AND f.cliente.email IS NOT NULL")
List<Factura> findFacturasConPagoVencido(@Param("fechaHoy") Date fechaHoy);
```

---

## ✅ Compilación

```bash
mvn clean compile
```

**Resultado:** ✅ BUILD SUCCESS
- 59 archivos compilados sin errores
- Tiempo: 5.081s

---

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

## 📦 Archivos Creados/Modificados

### **Modificados:**
1. ✅ `src/main/java/api/astro/whats_orders_manager/models/Factura.java`

### **Creados:**
1. ✅ `docs/base de datos/MIGRATION_FACTURA_FECHA_PAGO.sql`
2. ✅ `docs/sprints/SPRINT_2/CAMBIO_FACTURA_FECHA_PAGO.md` (este archivo)

### **Pendientes de Modificar:**
1. ⏳ `templates/facturas/facturas.html` - Agregar columna fecha_pago
2. ⏳ `templates/facturas/form.html` - Campo fecha_pago
3. ⏳ `static/js/facturas.js` - Cálculo automático
4. ⏳ `FacturaController.java` - Setear fecha_pago al crear
5. ⏳ `FacturaServiceImpl.java` - Lógica de cálculo

---

## 🎯 Próximos Pasos

### **Inmediato:**
1. ✅ Modelo actualizado
2. ✅ Script SQL creado
3. ⏳ **PENDIENTE:** Ejecutar migración SQL
4. ⏳ **PENDIENTE:** Reiniciar aplicación

### **Corto Plazo:**
1. Actualizar vistas HTML para mostrar fecha_pago
2. Actualizar formulario para calcular fecha_pago
3. Actualizar controller para setear fecha_pago

### **Sprint 2 - Punto 5.3.3:**
1. Crear RecordatorioPagoScheduler.java
2. Implementar query findFacturasConPagoVencido()
3. Integrar con EmailService
4. Testing de recordatorios

---

## 💡 Beneficios del Cambio

### **1. Claridad de Datos**
- ✅ `fechaPago` tiene propósito específico de negocio
- ✅ No confusión con `createDate` (auditoría)
- ✅ Nomenclatura descriptiva

### **2. Funcionalidad Mejorada**
- ✅ Base para recordatorios automáticos
- ✅ Cálculo flexible de días de crédito
- ✅ Control sobre plazos de pago

### **3. Mantenibilidad**
- ✅ Menos campos redundantes
- ✅ Código más limpio
- ✅ Queries más eficientes

---

## ⚠️ Consideraciones Importantes

### **1. Datos Legacy**
- Facturas antiguas tendrán `fecha_pago` calculada automáticamente
- Revisar si las fechas calculadas son correctas
- Puede requerir ajuste manual para casos especiales

### **2. Días de Crédito**
- Actualmente hardcoded: 7 días
- Considerar hacer configurable por cliente o tipo de factura
- Futuro: agregar campo `diasCredito` en Cliente o ConfiguracionFacturacion

### **3. Validaciones**
- `fecha_pago` debe ser >= `fecha_entrega`
- Si se edita `fecha_entrega`, recalcular `fecha_pago`
- Alertar si `fecha_pago` < `fecha_actual` (factura vencida)

---

**Autor:** GitHub Copilot  
**Fecha:** 13 de octubre de 2025  
**Sprint:** Sprint 2 - Fase 5 (Notificaciones)  
**Estado:** ✅ MODELO ACTUALIZADO - Pendiente migración SQL
