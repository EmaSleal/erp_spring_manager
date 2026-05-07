## 🔍 MEJORA 3: Filtros de Facturación Electrónica

### Ubicación
**Archivo:** `src/main/resources/templates/modules/facturacion/facturas.html`

### Implementación

#### 3.1 Agregar Filtro de Estado FE

```html
<!-- En la sección de filtros -->
<div class="row mb-3">
    <div class="col-md-3">
        <label for="filtroEstadoFE" class="form-label">Estado FE</label>
        <select id="filtroEstadoFE" class="form-select" onchange="aplicarFiltros()">
            <option value="">Todos</option>
            <option value="CON_FE">Con FE</option>
            <option value="SIN_FE">Sin FE</option>
            <option value="ACEPTADO">Aceptados</option>
            <option value="RECHAZADO">Rechazados</option>
            <option value="ENVIADO">Enviados (pendientes)</option>
            <option value="ERROR">Con errores</option>
        </select>
    </div>
    
    <!-- Filtros existentes (fecha, cliente, etc.) -->
</div>
```

#### 3.2 JavaScript para Filtrado

```javascript
/**
 * Aplica filtros combinados (incluyendo estado FE).
 */
function aplicarFiltros() {
    const estadoFE = document.getElementById('filtroEstadoFE').value;
    const fechaDesde = document.getElementById('fechaDesde').value;
    const fechaHasta = document.getElementById('fechaHasta').value;
    const cliente = document.getElementById('filtroCliente').value;
    
    // Construir URL con parámetros
    const params = new URLSearchParams();
    if (estadoFE) params.append('estadoFE', estadoFE);
    if (fechaDesde) params.append('fechaDesde', fechaDesde);
    if (fechaHasta) params.append('fechaHasta', fechaHasta);
    if (cliente) params.append('clienteId', cliente);
    
    // Guardar en localStorage
    localStorage.setItem('filtrosFacturas', JSON.stringify({
        estadoFE, fechaDesde, fechaHasta, cliente
    }));
    
    // Recargar página con filtros
    window.location.href = `/facturas?${params.toString()}`;
}

/**
 * Restaurar filtros desde localStorage al cargar página.
 */
document.addEventListener('DOMContentLoaded', () => {
    const filtrosGuardados = localStorage.getItem('filtrosFacturas');
    if (filtrosGuardados) {
        const filtros = JSON.parse(filtrosGuardados);
        if (filtros.estadoFE) document.getElementById('filtroEstadoFE').value = filtros.estadoFE;
        if (filtros.fechaDesde) document.getElementById('fechaDesde').value = filtros.fechaDesde;
        if (filtros.fechaHasta) document.getElementById('fechaHasta').value = filtros.fechaHasta;
        if (filtros.cliente) document.getElementById('filtroCliente').value = filtros.cliente;
    }
});
```

#### 3.3 Controlador Backend (Filtrado)

**Archivo:** `src/main/java/.../controller/FacturaController.java`

```java
@GetMapping
public String listarFacturas(
        @RequestParam(required = false) String estadoFE,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        @RequestParam(required = false) Long clienteId,
        @PageableDefault(size = 20, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
        Model model
) {
    // Aplicar filtros
    Specification<Factura> spec = Specification.where(null);
    
    // Filtro de estado FE
    if (estadoFE != null && !estadoFE.isEmpty()) {
        spec = spec.and(FacturaSpecifications.withEstadoFE(estadoFE));
    }
    
    // Filtros existentes (fecha, cliente)...
    
    Page<Factura> facturas = facturaService.findAll(spec, pageable);
    model.addAttribute("facturas", facturas);
    
    return "modules/facturacion/facturas";
}
```

**Specifications para Estado FE:**

```java
public class FacturaSpecifications {
    
    public static Specification<Factura> withEstadoFE(String estadoFE) {
        return (root, query, cb) -> {
            Join<Factura, ComprobanteElectronico> comprobanteJoin = 
                root.join("comprobanteElectronico", JoinType.LEFT);
            
            return switch (estadoFE) {
                case "CON_FE" -> comprobanteJoin.isNotNull();
                case "SIN_FE" -> comprobanteJoin.isNull();
                case "ACEPTADO" -> cb.equal(comprobanteJoin.get("estado"), EstadoComprobante.ACEPTADO);
                case "RECHAZADO" -> cb.equal(comprobanteJoin.get("estado"), EstadoComprobante.RECHAZADO);
                case "ENVIADO" -> cb.equal(comprobanteJoin.get("estado"), EstadoComprobante.ENVIADO);
                case "ERROR" -> cb.equal(comprobanteJoin.get("estado"), EstadoComprobante.ERROR);
                default -> cb.conjunction();
            };
        };
    }
}
```

---

