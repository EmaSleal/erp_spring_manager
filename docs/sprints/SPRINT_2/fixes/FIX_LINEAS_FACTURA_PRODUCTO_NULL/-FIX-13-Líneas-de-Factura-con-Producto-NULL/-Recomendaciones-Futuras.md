## 📝 Recomendaciones Futuras

### 1. Validación en Backend

Agregar validación en `LineaFacturaServiceImpl.java`:

```java
@Transactional
public void updateLineas(List<LineaFacturaR> lineas) {
    // ✅ Filtrar líneas inválidas
    List<LineaFacturaR> lineasValidas = lineas.stream()
        .filter(linea -> linea.id_producto() != null && linea.id_producto() > 0)
        .collect(Collectors.toList());
    
    if (lineasValidas.isEmpty()) {
        throw new IllegalArgumentException("Debe proporcionar al menos una línea válida");
    }
    
    // Renumerar líneas válidas
    for (int i = 0; i < lineasValidas.size(); i++) {
        lineasValidas.get(i).setNumeroLinea(i + 1);
    }
    
    // Continuar con guardado...
}
```

### 2. Toast de Notificación

Mejorar feedback cuando se omiten líneas:

```javascript
if (lineasVacias > 0) {
    Swal.fire({
        icon: 'info',
        title: 'Líneas omitidas',
        text: `Se omitieron ${lineasVacias} línea(s) sin producto`,
        timer: 3000,
        toast: true,
        position: 'top-end'
    });
}
```

### 3. Deshabilitar Botón Eliminar de Última Línea

Si solo hay una línea, deshabilitar botón eliminar para evitar guardado sin líneas:

```javascript
function updateDeleteButtons() {
    const rows = document.querySelectorAll("#lineas-body tr");
    const deleteButtons = document.querySelectorAll("#lineas-body button[onclick*='removeLinea']");
    
    if (rows.length === 1) {
        deleteButtons[0].disabled = true;
    }
}
```

---

