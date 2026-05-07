## 🔍 ANÁLISIS

### Campos del Modelo Producto

```java
@Entity
@Table(name = "producto")
public class Producto {
    // ...
    
    @Column(name = "precioInstitucional")
    private BigDecimal precioInstitucional;
    
    @Column(name = "precioMayorista")
    private BigDecimal precioMayorista;
    
    // NO EXISTE: precioPublico ❌
}
```

### Contexto del Error
- Usuario accedió a `/reportes/productos`
- La tabla intenta mostrar dos columnas de precios
- Primera columna: "Precio Mayorista" → `precioMayorista` ✅
- Segunda columna: "Precio Público" → `precioPublico` ❌ (no existe)

---

