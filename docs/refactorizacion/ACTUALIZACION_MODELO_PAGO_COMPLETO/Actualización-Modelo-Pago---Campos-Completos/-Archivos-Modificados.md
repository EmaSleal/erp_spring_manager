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

