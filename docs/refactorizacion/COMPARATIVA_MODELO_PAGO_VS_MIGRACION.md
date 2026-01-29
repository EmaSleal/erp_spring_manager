# Comparativa: Modelo Pago vs Migración SQL

**Fecha**: 18 de enero de 2026  
**Sprint**: 5 - Fase 1  
**Estado**: ⚠️ DISCREPANCIAS IDENTIFICADAS

---

## 🔍 Resumen Ejecutivo

El modelo JPA `Pago.java` y la migración SQL `MIGRATION_PAGOS.sql` tienen **diferencias significativas** en estructura de datos. El modelo actual es más simple y orientado a validación automática por Hibernate, mientras que la migración SQL incluye campos adicionales para trazabilidad completa.

---

## 📊 Comparativa de Campos

### ✅ Campos Presentes en Ambos

| Campo Modelo (JPA) | Campo SQL | Tipo Modelo | Tipo SQL | Coincide |
|-------------------|-----------|-------------|----------|----------|
| `idPago` | `id_pago` | `Long` | `BIGINT` | ✅ |
| `factura` (FK) | `factura_id` | `ManyToOne` | `INT` | ✅ |
| `monto` | `monto` | `BigDecimal(12,2)` | `DECIMAL(12,2)` | ✅ |
| `metodoPago` | `metodo_pago` | `Enum` | `VARCHAR(30)` | ✅ |
| `fechaPago` | `fecha_pago` | `LocalDateTime` | `DATE` | ⚠️ Tipo diferente |
| `estado` | `estado` | `Enum` | `VARCHAR(20)` | ✅ |
| `referencia` | `referencia_bancaria` | `String(100)` | `VARCHAR(100)` | ⚠️ Nombre diferente |
| `notas` | `observaciones` | `String(500)` | `TEXT` | ⚠️ Nombre y tipo diferente |
| `conciliado` | - | `Boolean` | - | ❌ Solo en modelo |
| `fechaConciliacion` | - | `LocalDateTime` | - | ❌ Solo en modelo |
| `creadoPor` | `creado_por` | `Integer` | `VARCHAR(100)` | ⚠️ Tipo diferente |
| `fechaCreacion` | `creado_en` | `LocalDateTime` | `TIMESTAMP` | ✅ |
| `modificadoPor` | `modificado_por` | `Integer` | `VARCHAR(100)` | ⚠️ Tipo diferente |
| `fechaModificacion` | `modificado_en` | `LocalDateTime` | `TIMESTAMP` | ✅ |

---

### ❌ Campos SOLO en Migración SQL (Faltantes en Modelo)

| Campo SQL | Tipo | Descripción | ¿Necesario? |
|-----------|------|-------------|-------------|
| `numero_pago` | `VARCHAR(20) UNIQUE` | Consecutivo PAG-YYYYMMDD-0001 | **⚠️ CRÍTICO** |
| `cliente_id` | `BIGINT FK` | Relación directa a cliente | **⚠️ CRÍTICO** |
| `tipo_pago` | `VARCHAR(20)` | TOTAL/PARCIAL/ADELANTO/NOTA_CREDITO | **⚠️ IMPORTANTE** |
| `banco` | `VARCHAR(100)` | Nombre del banco | ℹ️ Útil |
| `cuenta_bancaria` | `VARCHAR(50)` | Últimos dígitos de cuenta | ℹ️ Útil |
| `comprobante_url` | `VARCHAR(255)` | URL del comprobante digitalizado | ℹ️ Útil |
| `anulado_por` | `VARCHAR(100)` | Usuario que anula | **⚠️ IMPORTANTE** |
| `anulado_en` | `TIMESTAMP` | Fecha de anulación | **⚠️ IMPORTANTE** |
| `motivo_anulacion` | `TEXT` | Motivo de anulación | **⚠️ IMPORTANTE** |

---

### ❌ Campos SOLO en Modelo JPA (Faltantes en SQL)

| Campo Modelo | Tipo | Descripción | ¿Necesario en SQL? |
|--------------|------|-------------|-------------------|
| `conciliado` | `Boolean` | Indica si está conciliado | ℹ️ Ya está en `estado` |
| `fechaConciliacion` | `LocalDateTime` | Fecha de conciliación | ℹ️ Útil tener explícito |

---

## 🔧 Discrepancias de Tipo de Dato

### 1. **fechaPago**
- **Modelo JPA**: `LocalDateTime` (fecha + hora exacta)
- **SQL**: `DATE` (solo fecha sin hora)
- **Impacto**: El modelo permite timestamp completo, la BD solo almacena fecha
- **Recomendación**: Cambiar SQL a `TIMESTAMP` o modelo a `LocalDate`

### 2. **creadoPor / modificadoPor**
- **Modelo JPA**: `Integer` (ID numérico del usuario)
- **SQL**: `VARCHAR(100)` (nombre/username del usuario)
- **Impacto**: Inconsistencia en tipo de referencia de auditoría
- **Recomendación**: Decidir si usar ID o username (proyecto usa `Integer` con `@CreatedBy`)

### 3. **notas vs observaciones**
- **Modelo JPA**: `notas` (`String(500)`)
- **SQL**: `observaciones` (`TEXT`)
- **Impacto**: Diferente nombre de columna causará error Hibernate
- **Recomendación**: Unificar nombre a `notas` o agregar `@Column(name = "observaciones")`

### 4. **referencia vs referencia_bancaria**
- **Modelo JPA**: `referencia`
- **SQL**: `referencia_bancaria`
- **Impacto**: Hibernate buscará columna `referencia`, no existe en SQL
- **Recomendación**: Agregar `@Column(name = "referencia_bancaria")` al modelo

---

## 🚨 Problemas Críticos Identificados

### 1. **Falta `numero_pago` (CRÍTICO)**
```java
// ❌ NO EXISTE en modelo actual
@Column(unique = true, length = 20)
private String numeroPago; // PAG-YYYYMMDD-0001
```
**Consecuencias**:
- No se puede generar número consecutivo de pago
- No hay identificador de negocio único
- Dificulta auditoría y búsqueda de pagos

---

### 2. **Falta relación directa `Cliente` (CRÍTICO)**
```java
// ❌ NO EXISTE en modelo actual
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "cliente_id")
private Cliente cliente;
```
**Consecuencias**:
- La migración SQL tiene FK a `clientes.id_cliente`
- El modelo solo tiene relación vía `Factura.cliente`
- Para adelantos/pagos sin factura asignada, se necesita cliente directo

---

### 3. **Falta `tipo_pago` (IMPORTANTE)**
```java
// ❌ NO EXISTE en modelo actual
public enum TipoPago {
    TOTAL("Pago total de factura"),
    PARCIAL("Pago parcial"),
    ADELANTO("Adelanto de cliente"),
    NOTA_CREDITO("Aplicación de nota de crédito");
}

@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private TipoPago tipoPago;
```
**Consecuencias**:
- No se diferencia entre tipos de pago
- Lógica de aplicación de pagos puede fallar

---

### 4. **Campos de Anulación (IMPORTANTE)**
```java
// ❌ NO EXISTEN en modelo actual
@Column
private Integer anuladoPor;

@Column
private LocalDateTime anuladoEn;

@Column(length = 1000)
private String motivoAnulacion;
```
**Consecuencias**:
- No hay trazabilidad de anulaciones
- No se registra quién y cuándo anuló
- Falta justificación de anulación (auditoría)

---

## 🎯 Recomendaciones de Acción

### Opción 1: Actualizar Modelo JPA (RECOMENDADO)
Agregar los campos faltantes al modelo `Pago.java` para que coincida con la migración SQL completa.

### Opción 2: Simplificar Migración SQL
Eliminar campos de la migración SQL que no están en el modelo JPA.

### Opción 3: Enfoque Híbrido
- Mantener campos críticos (`numero_pago`, `cliente_id`, `tipo_pago`, anulación)
- Eliminar campos opcionales (`banco`, `cuenta_bancaria`, `comprobante_url`)

---

## ✅ Campos que Sí Deben Agregarse al Modelo

### Alta Prioridad (Funcionalidad Core)
1. ✅ `numeroPago` (String, único)
2. ✅ `cliente` (ManyToOne a Cliente)
3. ✅ `tipoPago` (Enum)
4. ✅ `anuladoPor`, `anuladoEn`, `motivoAnulacion`

### Media Prioridad (Trazabilidad)
5. ⚠️ `banco` (String)
6. ⚠️ `cuentaBancaria` (String)

### Baja Prioridad (Features Avanzadas)
7. ℹ️ `comprobanteUrl` (String)

---

## 📝 Mapeo de Columnas a Corregir

```java
// En Pago.java, agregar mapeos explícitos:

@Column(name = "referencia_bancaria", length = 100)
private String referencia;

@Column(name = "observaciones", length = 1000)
private String notas;
```

---

## 🔄 Estado Actual vs Esperado

| Componente | Estado Actual | Estado Esperado |
|------------|---------------|-----------------|
| Modelo JPA | 15 campos | 24 campos (+9) |
| Migración SQL | 21 campos | Sin cambios |
| Coincidencia | ~60% | 100% |
| Hibernate Auto-Creation | ❌ Creará tabla incompleta | ✅ Tabla completa |
| Funcionalidad Negocio | ⚠️ Limitada | ✅ Completa |

---

## 🚀 Próximos Pasos

1. **Inmediato**: Corregir mapeo de columnas existentes (`@Column(name = "...")"`)
2. **Corto Plazo**: Agregar campos críticos (`numeroPago`, `cliente`, `tipoPago`)
3. **Mediano Plazo**: Agregar campos de anulación
4. **Largo Plazo**: Decidir sobre campos opcionales (banco, comprobante, etc.)

---

## 📌 Notas Finales

- La discrepancia surgió porque la migración SQL fue diseñada con requisitos completos de auditoría y trazabilidad
- El modelo JPA existente fue creado con enfoque minimalista
- Hibernate con `ddl-auto: update` creará tabla basada en el modelo, **ignorando** la migración SQL
- Si se levanta la app sin corregir, se creará tabla `pagos` sin los campos críticos
- **Decisión requerida**: ¿Actualizar modelo o simplificar migración?

---

**Generado**: 18 de enero de 2026, 23:54  
**Versión**: 1.0  
**Autor**: GitHub Copilot
