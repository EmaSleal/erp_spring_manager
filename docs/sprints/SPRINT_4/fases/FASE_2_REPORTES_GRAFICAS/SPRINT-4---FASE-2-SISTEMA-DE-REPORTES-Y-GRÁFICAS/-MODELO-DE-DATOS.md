## 🗄️ MODELO DE DATOS

### DTOs de Reportes

#### 1. VentasPorMesDTO
```java
public class VentasPorMesDTO {
    private String mes;           // "2025-01", "2025-02"
    private BigDecimal totalVentas;
    private Long cantidadFacturas;
    private BigDecimal ticketPromedio;
    
    // Constructor para proyección nativa
    public VentasPorMesDTO(String mes, BigDecimal totalVentas, 
                          Long cantidadFacturas, BigDecimal ticketPromedio) {
        this.mes = mes;
        this.totalVentas = totalVentas;
        this.cantidadFacturas = cantidadFacturas;
        this.ticketPromedio = ticketPromedio;
    }
}
```

#### 2. ProductoVendidoDTO
```java
public class ProductoVendidoDTO {
    private Long productoId;
    private String nombreProducto;
    private String categoria;
    private Long cantidadVendida;
    private BigDecimal totalVentas;
    private BigDecimal precioPromedio;
}
```

#### 3. EstadisticasClienteDTO
```java
public class EstadisticasClienteDTO {
    private Long clienteId;
    private String nombreCliente;
    private String email;
    private Long totalCompras;
    private BigDecimal totalGastado;
    private BigDecimal ticketPromedio;
    private LocalDate ultimaCompra;
    private String categoria; // VIP, Frecuente, Ocasional, Nuevo
}
```

#### 4. ComparativaAnualDTO
```java
public class ComparativaAnualDTO {
    private String mes;
    private BigDecimal ventasAnioActual;
    private BigDecimal ventasAnioAnterior;
    private BigDecimal variacionPorcentaje;
    private String tendencia; // "CRECIMIENTO", "ESTABLE", "DECRECIMIENTO"
}
```

#### 5. DistribucionCategoriaDTO
```java
public class DistribucionCategoriaDTO {
    private String categoria;
    private Long cantidadProductos;
    private BigDecimal totalVentas;
    private BigDecimal porcentajeVentas;
    private String color; // Para Chart.js
}
```

### Clase de Filtros

```java
public class ReporteFiltrosDTO {
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaInicio;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaFin;
    
    private Long categoriaId;
    private String estadoFactura; // PENDIENTE, PAGADA, VENCIDA
    private Integer limite; // Para TOP N
    
    // Validación personalizada
    @AssertTrue(message = "La fecha de inicio debe ser anterior a la fecha de fin")
    public boolean isFechasValidas() {
        if (fechaInicio == null || fechaFin == null) return true;
        return fechaInicio.isBefore(fechaFin) || fechaInicio.isEqual(fechaFin);
    }
}
```

---

