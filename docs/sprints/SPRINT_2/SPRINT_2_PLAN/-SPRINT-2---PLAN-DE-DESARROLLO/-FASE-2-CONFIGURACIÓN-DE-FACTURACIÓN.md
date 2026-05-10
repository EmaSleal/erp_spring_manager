## 📦 FASE 2: CONFIGURACIÓN DE FACTURACIÓN

### Objetivo
Configurar parámetros de facturación (serie, numeración, impuestos).

### Tareas

#### 2.1 Modelo de Datos
**Archivo:** `ConfiguracionFacturacion.java`

```java
@Entity
@Table(name = "configuracion_facturacion")
public class ConfiguracionFacturacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "serie_factura", length = 10)
    private String serieFactura = "F001";
    
    @Column(name = "numero_inicial")
    private Integer numeroInicial = 1;
    
    @Column(name = "numero_actual")
    private Integer numeroActual = 1;
    
    @Column(name = "prefijo_factura", length = 10)
    private String prefijoFactura;
    
    @Column(name = "igv", precision = 5, scale = 2)
    private BigDecimal igv = new BigDecimal("18.00");
    
    @Column(name = "moneda", length = 3)
    private String moneda = "PEN";
    
    @Column(name = "terminos_condiciones", columnDefinition = "TEXT")
    private String terminosCondiciones;
    
    @Column(name = "nota_pie_pagina", length = 500)
    private String notaPiePagina;
    
    @Column(name = "incluir_igv_en_precio")
    private Boolean incluirIgvEnPrecio = true;
    
    @Column(name = "activo")
    private Boolean activo = true;
}
```

**Script SQL:**
```sql
CREATE TABLE configuracion_facturacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    serie_factura VARCHAR(10) DEFAULT 'F001',
    numero_inicial INT DEFAULT 1,
    numero_actual INT DEFAULT 1,
    prefijo_factura VARCHAR(10),
    igv DECIMAL(5,2) DEFAULT 18.00,
    moneda VARCHAR(3) DEFAULT 'PEN',
    terminos_condiciones TEXT,
    nota_pie_pagina VARCHAR(500),
    incluir_igv_en_precio BOOLEAN DEFAULT TRUE,
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Configuración por defecto
INSERT INTO configuracion_facturacion (serie_factura, numero_inicial, numero_actual, igv, moneda) 
VALUES ('F001', 1, 1, 18.00, 'PEN');
```

#### 2.2 Funcionalidades
- ✅ Configurar serie de factura
- ✅ Configurar número inicial y actual
- ✅ Configurar prefijo
- ✅ Configurar IGV/IVA (%)
- ✅ Configurar moneda
- ✅ Configurar términos y condiciones
- ✅ Configurar nota de pie de página
- ✅ Toggle: incluir IGV en precio

#### 2.3 Integración
- Modificar `FacturaService` para usar configuración
- Auto-incrementar número de factura al crear una nueva
- Aplicar IGV según configuración
- Mostrar términos y condiciones en PDF de factura

---

