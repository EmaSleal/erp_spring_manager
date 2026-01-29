# Actualización Modelo Pago - Campos Completos

**Fecha**: 19 de enero de 2026  
**Sprint**: 5 - Fase 1  
**Estado**: ✅ COMPLETADO

---

## 📋 Resumen Ejecutivo

Se ha actualizado completamente el modelo `Pago` para incluir **todos los campos** definidos en la migración SQL `MIGRATION_PAGOS.sql`. El modelo ahora soporta:

✅ Generación automática de números consecutivos  
✅ Relación directa con clientes (para adelantos)  
✅ Tipos de pago (TOTAL, PARCIAL, ADELANTO, NOTA_CREDITO)  
✅ Información bancaria completa  
✅ Trazabilidad de anulaciones  
✅ Comprobantes digitalizados  

---

## 🎯 Archivos Modificados

### 1. **Nuevo Archivo**: `TipoPago.java` (Enum)
**Ubicación**: `modules/facturacion/enums/TipoPago.java`  
**Líneas**: 65  
**Propósito**: Define los tipos de pago según su aplicación

```java
public enum TipoPago {
    TOTAL,      // Pago total de factura
    PARCIAL,    // Pago parcial
    ADELANTO,   // Adelanto sin factura asignada
    NOTA_CREDITO // Aplicación de nota de crédito
}
```

**Métodos de negocio**:
- `requiereFactura()` - Valida si necesita factura asignada
- `esAdelanto()` - Identifica adelantos
- `permiteAplicarAFactura()` - Verifica si se puede aplicar a factura existente

---

### 2. **Actualizado**: `Pago.java` (Modelo)
**Ubicación**: `modules/facturacion/model/Pago.java`  
**Cambios**: +9 campos nuevos, +2 métodos de negocio  
**Estado**: ✅ Sin errores de compilación

#### Campos Agregados:

| Campo | Tipo | Mapeo SQL | Descripción |
|-------|------|-----------|-------------|
| `numeroPago` | `String(20)` | `numeroPago` | Consecutivo único PAG-YYYYMMDD-NNNN |
| `cliente` | `ManyToOne` | `clienteId` | Relación directa a Cliente |
| `tipoPago` | `TipoPago` | `tipoPago` | Tipo de pago (TOTAL/PARCIAL/etc) |
| `banco` | `String(100)` | `banco` | Nombre del banco |
| `cuentaBancaria` | `String(50)` | `cuentaBancaria` | Últimos dígitos de cuenta |
| `comprobanteUrl` | `String(255)` | `comprobanteUrl` | URL del comprobante digitalizado |
| `anuladoPor` | `Integer` | `anuladoPor` | Usuario que anuló el pago |
| `anuladoEn` | `LocalDateTime` | `anuladoEn` | Fecha de anulación |
| `motivoAnulacion` | `String(1000)` | `motivoAnulacion` | Justificación de anulación |

#### Mapeos Corregidos:

| Campo Java | Mapeo Anterior | Mapeo Actual |
|------------|----------------|--------------|
| `referencia` | `@Column` | `@Column(name = "referenciaBancaria")` |
| `notas` | `@Column` | `@Column(name = "observaciones")` |
| `factura` | `optional = false` | `optional = true` (permite adelantos) |

#### Nuevos Métodos de Negocio:

```java
// Método para anular pagos con trazabilidad
public void anular(String motivo, Integer usuarioId) {
    this.estado = EstadoPago.ANULADO;
    this.anuladoPor = usuarioId;
    this.anuladoEn = LocalDateTime.now();
    this.motivoAnulacion = motivo;
}

// Validación de factura requerida según tipo de pago
public void validarFacturaRequerida() {
    if (tipoPago.requiereFactura() && factura == null) {
        throw new IllegalStateException("Requiere factura asignada");
    }
}

// Validación mejorada de monto (considera adelantos)
public void validarMonto() {
    if (tipoPago.esAdelanto()) return; // No valida en adelantos
    // ... validación normal
}
```

#### Índices Actualizados:

Se agregaron 3 nuevos índices a la tabla:

```java
@Index(name = "idx_pago_numero", columnList = "numeroPago", unique = true)
@Index(name = "idx_pago_cliente", columnList = "clienteId")
@Index(name = "idx_pago_tipo", columnList = "tipoPago")
```

---

### 3. **Actualizado**: `PagoDTO.java`
**Ubicación**: `modules/facturacion/dto/PagoDTO.java`  
**Cambios**: +9 campos nuevos, +1 método

#### Campos Agregados:

```java
private String numeroPago;
private Integer idCliente;      // Cambio: idFactura ahora es opcional
private TipoPago tipoPago;
private String tipoPagoDescripcion;  // Para vistas
private String banco;
private String cuentaBancaria;
private String comprobanteUrl;
private Integer anuladoPor;
private LocalDateTime anuladoEn;
private String motivoAnulacion;
```

#### Validaciones Actualizadas:

```java
// ANTES: idFactura era @NotNull
@NotNull(message = "El ID de la factura es obligatorio")
private Integer idFactura;

// AHORA: idCliente es obligatorio, idFactura es opcional
@NotNull(message = "El ID del cliente es obligatorio")
private Integer idCliente;
private Integer idFactura;  // Opcional para adelantos
```

#### Nuevo Método:

```java
public boolean estaAnulado() {
    return estado == EstadoPago.ANULADO;
}
```

#### Métodos Actualizados:

```java
// Ahora considera anulados además de conciliados
public boolean puedeEditarse() {
    return estado != EstadoPago.CONCILIADO && estado != EstadoPago.ANULADO;
}

public boolean puedeEliminarse() {
    return estado != EstadoPago.CONCILIADO && estado != EstadoPago.ANULADO;
}
```

---

### 4. **Actualizado**: `PagoMapper.java`
**Ubicación**: `modules/facturacion/dto/mapper/PagoMapper.java`  
**Cambios**: Mapeo completo de todos los nuevos campos

#### Método `toDTO()` Actualizado:

```java
PagoDTO dto = PagoDTO.builder()
    .numeroPago(pago.getNumeroPago())           // NUEVO
    .idCliente(pago.getCliente().getIdCliente())  // NUEVO
    .tipoPago(pago.getTipoPago())               // NUEVO
    .banco(pago.getBanco())                     // NUEVO
    .cuentaBancaria(pago.getCuentaBancaria())   // NUEVO
    .comprobanteUrl(pago.getComprobanteUrl())   // NUEVO
    .anuladoPor(pago.getAnuladoPor())           // NUEVO
    .anuladoEn(pago.getAnuladoEn())             // NUEVO
    .motivoAnulacion(pago.getMotivoAnulacion()) // NUEVO
    // ... campos existentes
    .build();

// Agrega descripción del tipo de pago
if (pago.getTipoPago() != null) {
    dto.setTipoPagoDescripcion(pago.getTipoPago().getDescripcion());
}

// Ahora obtiene nombre del cliente desde la relación directa
if (pago.getCliente() != null) {
    dto.setNombreCliente(pago.getCliente().getNombre());
}
```

#### Método `toEntity()` Actualizado:

```java
Pago pago = new Pago();
pago.setNumeroPago(dto.getNumeroPago());         // NUEVO
pago.setTipoPago(dto.getTipoPago());             // NUEVO
pago.setBanco(dto.getBanco());                   // NUEVO
pago.setCuentaBancaria(dto.getCuentaBancaria()); // NUEVO
pago.setComprobanteUrl(dto.getComprobanteUrl()); // NUEVO
pago.setAnuladoPor(dto.getAnuladoPor());         // NUEVO
pago.setAnuladoEn(dto.getAnuladoEn());           // NUEVO
pago.setMotivoAnulacion(dto.getMotivoAnulacion());// NUEVO
// ... campos existentes

// Nota: Cliente y factura se asignan en el servicio
```

#### Método `updateEntityFromDTO()` Actualizado:

```java
public static void updateEntityFromDTO(Pago pago, PagoDTO dto) {
    // No actualizar numeroPago (es único e inmutable)
    pago.setTipoPago(dto.getTipoPago());
    pago.setBanco(dto.getBanco());
    pago.setCuentaBancaria(dto.getCuentaBancaria());
    pago.setComprobanteUrl(dto.getComprobanteUrl());
    // ... otros campos actualizables
    // No actualizar campos de anulación (se usan métodos de negocio)
}
```

---

### 5. **Actualizado**: `PagoRepository.java`
**Ubicación**: `modules/facturacion/repository/PagoRepository.java`  
**Cambios**: +3 métodos nuevos

#### Métodos Agregados:

```java
/**
 * Busca el último número de pago que coincida con un patrón.
 * Útil para generar números consecutivos.
 */
@Query(value = "SELECT numero_pago FROM pagos WHERE numero_pago LIKE :patron " +
               "ORDER BY numero_pago DESC LIMIT 1", nativeQuery = true)
String findUltimoNumeroPagoPorPatron(@Param("patron") String patron);

/**
 * Busca un pago por su número único.
 */
@Query("SELECT p FROM Pago p WHERE p.numeroPago = :numeroPago")
Optional<Pago> findByNumeroPago(@Param("numeroPago") String numeroPago);

/**
 * Verifica si existe un pago con el número especificado.
 */
@Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
       "FROM Pago p WHERE p.numeroPago = :numeroPago")
boolean existsByNumeroPago(@Param("numeroPago") String numeroPago);
```

---

### 6. **Nuevo Archivo**: `NumeroPagoGeneratorService.java`
**Ubicación**: `modules/facturacion/service/NumeroPagoGeneratorService.java`  
**Líneas**: 140  
**Propósito**: Genera números consecutivos automáticos

#### Funcionalidades:

**Generación de Números**:
```java
// Genera: PAG-20260119-0001, PAG-20260119-0002, etc.
String numero = numeroPagoGeneratorService.generarNumeroPago();
```

**Formato**: `PAG-YYYYMMDD-NNNN`
- `PAG`: Prefijo fijo
- `YYYYMMDD`: Fecha (ej: 20260119)
- `NNNN`: Consecutivo del día con 4 dígitos (0001-9999)

**Características**:
- ✅ Thread-safe (`synchronized`)
- ✅ Transaccional (`@Transactional`)
- ✅ Validación de formato
- ✅ Extracción de fecha y consecutivo
- ✅ Logging con SLF4J

#### Métodos Principales:

```java
// Genera siguiente número para hoy
String generarNumeroPago()

// Genera siguiente número para fecha específica
String generarNumeroPago(LocalDate fecha)

// Valida formato PAG-YYYYMMDD-NNNN
boolean validarFormato(String numeroPago)

// Extrae la fecha del número
LocalDate extraerFecha(String numeroPago)

// Extrae el consecutivo
int extraerConsecutivo(String numeroPago)
```

#### Ejemplo de Uso:

```java
@Service
public class PagoServiceImpl {
    private final NumeroPagoGeneratorService numeroPagoGenerator;
    
    public Pago registrarPago(PagoDTO dto) {
        Pago pago = PagoMapper.toEntity(dto);
        
        // Generar número automáticamente
        String numero = numeroPagoGenerator.generarNumeroPago();
        pago.setNumeroPago(numero);
        
        return pagoRepository.save(pago);
    }
}
```

---

## 📊 Comparativa: Antes vs Después

| Aspecto | Antes | Después | Mejora |
|---------|-------|---------|--------|
| **Campos en Modelo** | 15 | 24 | +60% |
| **Campos en DTO** | 15 | 24 | +60% |
| **Métodos Repositorio** | 14 | 17 | +21% |
| **Enums** | 2 | 3 | +50% |
| **Servicios de Soporte** | 0 | 1 | ✅ Generador de números |
| **Coincidencia con SQL** | ~60% | 100% | ✅ Total |
| **Soporte Adelantos** | ❌ | ✅ | ✅ |
| **Trazabilidad Anulación** | ❌ | ✅ | ✅ |
| **Número Consecutivo** | ❌ | ✅ | ✅ |
| **Info Bancaria** | Parcial | Completa | ✅ |

---

## 🔧 Cambios en Lógica de Negocio

### 1. **Factura Ahora es Opcional**

**Antes**:
```java
@ManyToOne(optional = false)  // Siempre requerida
private Factura factura;
```

**Después**:
```java
@ManyToOne  // Opcional para adelantos
private Factura factura;
```

**Impacto**: Permite registrar adelantos de clientes sin factura asignada.

---

### 2. **Nueva Relación Directa con Cliente**

**Agregado**:
```java
@ManyToOne(optional = false)  // Siempre requerido
@JoinColumn(name = "clienteId")
private Cliente cliente;
```

**Ventajas**:
- Adelantos no necesitan factura ficticia
- Consultas directas de pagos por cliente
- Mejor integridad de datos

---

### 3. **Validación Mejorada de Factura**

**Antes**: Factura siempre obligatoria  
**Después**: Depende del tipo de pago

```java
// Se valida automáticamente en @PrePersist
if (tipoPago.requiereFactura() && factura == null) {
    throw new IllegalStateException("Requiere factura");
}
```

Tipos que **requieren** factura:
- ✅ TOTAL
- ✅ PARCIAL
- ✅ NOTA_CREDITO

Tipos que **NO requieren** factura:
- ❌ ADELANTO

---

### 4. **Anulación con Trazabilidad**

**Antes**: Solo cambiar estado
```java
pago.setEstado(EstadoPago.ANULADO);
```

**Después**: Método de negocio completo
```java
pago.anular("Duplicado", usuarioId);
// Registra: quién, cuándo, por qué
```

---

## 🚀 Próximos Pasos Requeridos

### 1. **Actualizar `PagoServiceImpl`** (CRÍTICO)

Necesita integrar el generador de números:

```java
@Service
public class PagoServiceImpl implements PagoService {
    
    private final NumeroPagoGeneratorService numeroPagoGenerator;
    private final ClienteRepository clienteRepository;  // NUEVO
    
    public Pago registrarPago(PagoDTO dto) {
        // Validar cliente existe
        Cliente cliente = clienteRepository.findById(dto.getIdCliente())
            .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));
        
        // Generar número automáticamente
        String numeroPago = numeroPagoGenerator.generarNumeroPago();
        
        Pago pago = PagoMapper.toEntity(dto);
        pago.setNumeroPago(numeroPago);
        pago.setCliente(cliente);
        
        // Asignar factura si existe (opcional para adelantos)
        if (dto.getIdFactura() != null) {
            Factura factura = facturaRepository.findById(dto.getIdFactura())
                .orElseThrow(() -> new EntityNotFoundException("Factura no encontrada"));
            pago.setFactura(factura);
        }
        
        return pagoRepository.save(pago);
    }
    
    public void anularPago(Long idPago, String motivo, Integer usuarioId) {
        Pago pago = pagoRepository.findById(idPago)
            .orElseThrow(() -> new EntityNotFoundException("Pago no encontrado"));
        
        // Usar método de negocio
        pago.anular(motivo, usuarioId);
        pagoRepository.save(pago);
    }
}
```

---

### 2. **Actualizar `PagoController`** (IMPORTANTE)

Ajustar formularios y validaciones:

```java
@Controller
@RequestMapping("/pagos")
public class PagoController {
    
    @GetMapping("/nuevo")
    public String nuevoPago(
        @RequestParam(required = false) Integer clienteId,  // NUEVO: permitir sin factura
        @RequestParam(required = false) Integer facturaId,
        Model model
    ) {
        PagoDTO pagoDTO = new PagoDTO();
        pagoDTO.setTipoPago(TipoPago.TOTAL);  // NUEVO: tipo por defecto
        
        if (clienteId != null) {
            pagoDTO.setIdCliente(clienteId);
        }
        
        if (facturaId != null) {
            pagoDTO.setIdFactura(facturaId);
            // Cargar factura...
        }
        
        model.addAttribute("pago", pagoDTO);
        model.addAttribute("tiposPago", TipoPago.values());  // NUEVO
        return "modules/facturacion/pagos/form";
    }
    
    @PostMapping("/anular/{id}")
    @PreAuthorize("hasPermission(null, 'PAGO_ANULAR')")
    public String anularPago(
        @PathVariable Long id,
        @RequestParam String motivo,
        Authentication auth
    ) {
        Integer usuarioId = obtenerUsuarioId(auth);
        pagoService.anularPago(id, motivo, usuarioId);
        return "redirect:/pagos";
    }
}
```

---

### 3. **Actualizar Vistas HTML** (IMPORTANTE)

#### `pagos/form.html`:

```html
<!-- Nuevo campo: Cliente (siempre visible) -->
<div class="mb-3">
    <label>Cliente *</label>
    <select th:field="*{idCliente}" class="form-select" required>
        <option value="">Seleccione...</option>
        <option th:each="c : ${clientes}" th:value="${c.idCliente}" 
                th:text="${c.nombre}"></option>
    </select>
</div>

<!-- Nuevo campo: Tipo de Pago -->
<div class="mb-3">
    <label>Tipo de Pago *</label>
    <select th:field="*{tipoPago}" class="form-select" required 
            onchange="toggleFacturaField()">
        <option th:each="tipo : ${tiposPago}" th:value="${tipo}" 
                th:text="${tipo.descripcion}"></option>
    </select>
</div>

<!-- Factura (opcional si es ADELANTO) -->
<div class="mb-3" id="facturaField">
    <label>Factura</label>
    <select th:field="*{idFactura}" class="form-select">
        <option value="">Sin asignar (Adelanto)</option>
        <!-- ... facturas del cliente ... -->
    </select>
</div>

<!-- Nuevos campos bancarios -->
<div class="mb-3">
    <label>Banco</label>
    <input type="text" th:field="*{banco}" class="form-control">
</div>

<div class="mb-3">
    <label>Cuenta Bancaria</label>
    <input type="text" th:field="*{cuentaBancaria}" class="form-control" 
           placeholder="Últimos 4 dígitos">
</div>

<!-- Comprobante digitalizado -->
<div class="mb-3">
    <label>Comprobante (URL)</label>
    <input type="url" th:field="*{comprobanteUrl}" class="form-control">
</div>
```

#### `pagos/detalle.html`:

```html
<!-- Mostrar número de pago -->
<p><strong>Número:</strong> <span th:text="${pago.numeroPago}"></span></p>

<!-- Mostrar tipo de pago -->
<p><strong>Tipo:</strong> 
    <span class="badge bg-info" th:text="${pago.tipoPagoDescripcion}"></span>
</p>

<!-- Mostrar datos de anulación si aplica -->
<div th:if="${pago.estaAnulado()}" class="alert alert-danger">
    <h5>Pago Anulado</h5>
    <p><strong>Anulado por:</strong> <span th:text="${pago.anuladoPor}"></span></p>
    <p><strong>Fecha:</strong> <span th:text="${#temporals.format(pago.anuladoEn, 'dd/MM/yyyy HH:mm')}"></span></p>
    <p><strong>Motivo:</strong> <span th:text="${pago.motivoAnulacion}"></span></p>
</div>

<!-- Mostrar info bancaria -->
<div th:if="${pago.banco != null}">
    <p><strong>Banco:</strong> <span th:text="${pago.banco}"></span></p>
    <p><strong>Cuenta:</strong> <span th:text="${pago.cuentaBancaria}"></span></p>
</div>

<!-- Enlace a comprobante si existe -->
<div th:if="${pago.comprobanteUrl != null}">
    <a th:href="${pago.comprobanteUrl}" target="_blank" class="btn btn-sm btn-outline-primary">
        <i class="bi bi-file-earmark-pdf"></i> Ver Comprobante
    </a>
</div>
```

---

### 4. **Agregar Queries para Cliente** (ÚTIL)

En `PagoRepository`:

```java
/**
 * Encuentra todos los pagos de un cliente.
 */
@Query("SELECT p FROM Pago p WHERE p.cliente.idCliente = :idCliente ORDER BY p.fechaPago DESC")
List<Pago> findByClienteId(@Param("idCliente") Integer idCliente);

/**
 * Encuentra adelantos sin factura asignada de un cliente.
 */
@Query("SELECT p FROM Pago p WHERE p.cliente.idCliente = :idCliente " +
       "AND p.tipoPago = 'ADELANTO' AND p.factura IS NULL " +
       "AND p.estado = 'CONFIRMADO' ORDER BY p.fechaPago")
List<Pago> findAdelantosDisponiblesPorCliente(@Param("idCliente") Integer idCliente);

/**
 * Calcula total de adelantos disponibles de un cliente.
 */
@Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p " +
       "WHERE p.cliente.idCliente = :idCliente " +
       "AND p.tipoPago = 'ADELANTO' AND p.factura IS NULL " +
       "AND p.estado = 'CONFIRMADO'")
BigDecimal sumAdelantosDisponiblesPorCliente(@Param("idCliente") Integer idCliente);
```

---

## ✅ Checklist de Integración

- [x] Crear enum `TipoPago`
- [x] Actualizar modelo `Pago` con 9 campos nuevos
- [x] Corregir mapeos de columnas (`referenciaBancaria`, `observaciones`)
- [x] Actualizar `PagoDTO` con nuevos campos
- [x] Actualizar `PagoMapper` (toDTO, toEntity, updateEntityFromDTO)
- [x] Agregar métodos en `PagoRepository`
- [x] Crear `NumeroPagoGeneratorService`
- [ ] **Actualizar `PagoServiceImpl`** (integrar generador + cliente)
- [ ] **Actualizar `PagoController`** (formularios + anulación)
- [ ] **Actualizar vistas HTML** (form.html, detalle.html, listar.html)
- [ ] **Agregar validaciones JavaScript** (tipo de pago → factura opcional)
- [ ] **Actualizar tests unitarios**
- [ ] **Probar flujo completo** (registrar, editar, anular)

---

## 🎓 Cambios en Flujos de Negocio

### Flujo 1: Registrar Pago Normal

1. Usuario selecciona **cliente** (nuevo campo obligatorio)
2. Usuario selecciona **factura** del cliente
3. Sistema carga `saldoPendiente` de la factura
4. Usuario ingresa **monto** ≤ saldoPendiente
5. Usuario selecciona **tipo de pago**: TOTAL o PARCIAL
6. Usuario selecciona **método de pago**
7. Si método requiere referencia → solicitar referencia
8. Sistema **genera número** automático: `PAG-20260119-0001`
9. Sistema guarda pago con estado `CONFIRMADO`
10. Sistema actualiza saldo de factura

---

### Flujo 2: Registrar Adelanto (NUEVO)

1. Usuario selecciona **cliente**
2. Usuario selecciona **tipo de pago**: ADELANTO
3. Sistema **oculta** campo factura (no requerido)
4. Usuario ingresa **monto** del adelanto
5. Usuario selecciona **método de pago**
6. Sistema **genera número**: `PAG-20260119-0002`
7. Sistema guarda pago:
   - `cliente` = Cliente seleccionado
   - `factura` = NULL
   - `tipoPago` = ADELANTO
   - `estado` = CONFIRMADO
8. Adelanto queda disponible para aplicar a futuras facturas

---

### Flujo 3: Aplicar Adelanto a Factura (FUTURO)

1. Usuario crea/edita factura de cliente
2. Sistema muestra adelantos disponibles del cliente
3. Usuario selecciona adelanto(s) a aplicar
4. Sistema actualiza pago(s):
   - `factura` = Factura actual
   - `tipoPago` = PARCIAL (o TOTAL si cubre todo)
5. Sistema recalcula saldo de factura

---

### Flujo 4: Anular Pago

1. Usuario abre detalle de pago
2. Si pago es editable → mostrar botón "Anular"
3. Usuario hace clic en "Anular"
4. Sistema solicita **motivo de anulación**
5. Usuario ingresa motivo
6. Sistema ejecuta `pago.anular(motivo, usuarioId)`:
   - `estado` = ANULADO
   - `anuladoPor` = ID del usuario actual
   - `anuladoEn` = Fecha/hora actual
   - `motivoAnulacion` = Motivo ingresado
7. Sistema recalcula saldo de factura
8. Sistema registra en log de auditoría

---

## 📈 Métricas de Impacto

| Métrica | Valor |
|---------|-------|
| **Archivos Creados** | 2 (TipoPago.java, NumeroPagoGeneratorService.java) |
| **Archivos Modificados** | 4 (Pago.java, PagoDTO.java, PagoMapper.java, PagoRepository.java) |
| **Líneas Agregadas** | ~350 |
| **Campos Nuevos** | 9 |
| **Métodos Nuevos** | 10 |
| **Índices Agregados** | 3 |
| **Cobertura SQL** | 60% → 100% |
| **Errores de Compilación** | 0 |

---

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

## 🎯 Próxima Sesión

Continuar con:

1. ✅ Actualizar `PagoServiceImpl`
2. ✅ Actualizar `PagoController`
3. ✅ Actualizar vistas HTML
4. ✅ Probar flujo completo
5. ✅ Migración de datos existentes
6. ✅ Tests unitarios

---

**Generado**: 19 de enero de 2026, 00:05  
**Versión**: 1.0  
**Estado**: ✅ Modelo actualizado sin errores
