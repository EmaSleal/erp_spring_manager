## 🔧 CAMBIOS REALIZADOS

### 1. **Modelo Factura.java** - Nuevos campos

**Campos agregados:**

```java
@Column(name = "numeroFactura", unique = true, length = 50)
private String numeroFactura;  // Ej: "F001-00001"

@Column(name = "serie", length = 10)
private String serie;  // Ej: "F001"

@Column(name = "subtotal", precision = 10, scale = 2)
private BigDecimal subtotal;  // Base imponible

@Column(name = "igv", precision = 10, scale = 2)
private BigDecimal igv;  // Impuesto calculado
```

**Notas:**
- `numeroFactura` es **único** (constraint en BD)
- `total` ya existía, ahora se calcula automáticamente
- Hibernate creará/actualizará las columnas automáticamente

---

### 2. **FacturaRepository.java** - Nuevos métodos

**Métodos agregados:**

```java
/**
 * Busca una factura por su número único
 */
Optional<Factura> findByNumeroFactura(String numeroFactura);

/**
 * Verifica si existe una factura con el número especificado
 */
boolean existsByNumeroFactura(String numeroFactura);
```

**Uso:**
- Validar duplicados antes de guardar
- Buscar facturas por número en lugar de ID
- Útil para búsquedas de clientes ("¿Cuál es el estado de mi factura F001-00025?")

---

### 3. **FacturaServiceImpl.java** - Integración completa

#### 3.1 Inyección de dependencias

```java
@Autowired
private ConfiguracionFacturacionService configuracionFacturacionService;
```

#### 3.2 Método `save()` mejorado

**Flujo automático al crear factura:**

```java
public Factura save(Factura factura) {
    // 1. Obtener configuración activa (o crearla si no existe)
    ConfiguracionFacturacion config = configuracionFacturacionService.getOrCreateConfiguracion();
    
    // 2. Generar número de factura automático
    String numeroFactura = config.generarNumeroFactura();  // "F001-00001"
    factura.setNumeroFactura(numeroFactura);
    factura.setSerie(config.getSerieFactura());
    
    // 3. Calcular IGV automáticamente (si no está calculado)
    if (factura.getIgv() == null && factura.getSubtotal() != null) {
        BigDecimal igvCalculado = config.calcularIgv(factura.getSubtotal());
        factura.setIgv(igvCalculado);
    }
    
    // 4. Calcular total automáticamente (si no está calculado)
    if (factura.getTotal() == null && factura.getSubtotal() != null) {
        BigDecimal totalCalculado = config.calcularTotal(factura.getSubtotal());
        factura.setTotal(totalCalculado);
    }
    
    // 5. Guardar factura en BD
    Factura facturaGuardada = facturaRepository.save(factura);
    
    // 6. Incrementar número para próxima factura
    configuracionFacturacionService.incrementarNumeroFactura();
    
    return facturaGuardada;
}
```

#### 3.3 Métodos auxiliares nuevos

```java
/**
 * Obtiene la configuración actual (para vistas)
 */
public ConfiguracionFacturacion getConfiguracionFacturacion() {
    return configuracionFacturacionService.getOrCreateConfiguracion();
}

/**
 * Preview del próximo número sin incrementar
 */
public String getPreviewNumeroFactura() {
    return configuracionFacturacionService.generarSiguienteNumeroFactura();
}
```

**Uso en controladores:**
```java
// Mostrar configuración en vistas
model.addAttribute("config", facturaService.getConfiguracionFacturacion());

// Mostrar preview en formulario
model.addAttribute("proximoNumero", facturaService.getPreviewNumeroFactura());
```

---

