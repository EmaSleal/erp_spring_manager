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

