## 🎨 MEJORA 1: Columna "Estado FE" en Tabla de Facturas

### Ubicación
**Archivo:** `src/main/resources/templates/modules/facturacion/facturas.html`

### Implementación

#### 1.1 Agregar Columna en Tabla

```html
<!-- En la sección <thead> -->
<thead>
    <tr>
        <th>ID</th>
        <th>Cliente</th>
        <th>Número</th>
        <th>Fecha</th>
        <th>Total</th>
        <th>Estado FE</th> <!-- NUEVA COLUMNA -->
        <th>Acciones</th>
    </tr>
</thead>

<!-- En la sección <tbody> -->
<tbody>
    <tr th:each="factura : ${facturas}">
        <td th:text="${factura.idFactura}"></td>
        <td th:text="${factura.cliente.nombre}"></td>
        <td th:text="${factura.numeroFactura}"></td>
        <td th:text="${#temporals.format(factura.createDate, 'dd/MM/yyyy')}"></td>
        <td th:text="${'S/ ' + #numbers.formatDecimal(factura.total, 1, 2)}"></td>
        
        <!-- NUEVA COLUMNA: Estado FE con Badge -->
        <td>
            <span th:if="${factura.comprobanteElectronico}" 
                  th:class="${'badge ' + #strings.concat('bg-', factura.comprobanteElectronico.estado.color)}"
                  th:title="${factura.comprobanteElectronico.mensajeRespuesta ?: 'Sin mensaje'}"
                  data-bs-toggle="tooltip">
                
                <i th:classappend="${factura.comprobanteElectronico.estado.icono}"></i>
                <span th:text="${factura.comprobanteElectronico.estado.descripcion}"></span>
            </span>
            
            <span th:unless="${factura.comprobanteElectronico}" 
                  class="badge bg-secondary"
                  title="No enviado a Hacienda"
                  data-bs-toggle="tooltip">
                <i class="bi bi-dash-circle"></i> Sin FE
            </span>
            
            <!-- Link al comprobante (si existe) -->
            <a th:if="${factura.comprobanteElectronico}"
               th:href="@{/facturas/comprobantes/{id}(id=${factura.comprobanteElectronico.id})}"
               class="ms-2"
               title="Ver comprobante electrónico">
                <i class="bi bi-box-arrow-up-right"></i>
            </a>
        </td>
        
        <td>
            <!-- Acciones existentes -->
        </td>
    </tr>
</tbody>
```

#### 1.2 Agregar Colores y Íconos en Enum

**Archivo:** `src/main/java/.../electronica/enums/EstadoComprobante.java`

```java
public enum EstadoComprobante {
    GENERADO("Generado", "warning", "bi-file-earmark-text"),
    FIRMADO("Firmado", "info", "bi-shield-check"),
    ENVIADO("Enviado", "primary", "bi-send"),
    ACEPTADO("Aceptado", "success", "bi-check-circle-fill"),
    RECHAZADO("Rechazado", "danger", "bi-x-circle-fill"),
    ERROR("Error", "danger", "bi-exclamation-triangle-fill");
    
    private final String descripcion;
    private final String color;
    private final String icono;
    
    EstadoComprobante(String descripcion, String color, String icono) {
        this.descripcion = descripcion;
        this.color = color;
        this.icono = icono;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public String getColor() {
        return color;
    }
    
    public String getIcono() {
        return icono;
    }
}
```

#### 1.3 CSS Personalizado

```css
/* En facturas.html o archivo CSS separado */
.badge {
    font-size: 0.875rem;
    padding: 0.35em 0.65em;
    font-weight: 500;
}

.badge i {
    margin-right: 0.25rem;
}

/* Animación para estados en proceso */
.badge.bg-primary {
    animation: pulse 2s infinite;
}

@keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.7; }
}
```

---

