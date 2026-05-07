## 🔧 Archivos Modificados

### **1. productos.js** - Función `renderPagination()`

**Cambios principales:**
- ✅ Detección de ancho de pantalla con `window.innerWidth`
- ✅ Cálculo de `maxVisiblePages` según breakpoint
- ✅ Algoritmo sliding window para rango visible
- ✅ Primera y última página siempre visibles
- ✅ Separadores "..." para indicar páginas ocultas

**Líneas modificadas:** ~100 líneas (función completa reescrita)

### **2. productos.js** - Event Listener de Resize

**Nuevo código:**
```javascript
// Re-renderizar paginación al cambiar tamaño de ventana
let resizeTimer;
window.addEventListener('resize', () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => {
        renderPagination();
    }, 250); // Debounce de 250ms
});
```

**Propósito:** Actualizar paginación automáticamente al rotar dispositivo o redimensionar ventana.

### **3. common.css** - Estilos de Paginación

**Nuevos estilos agregados:**
```css
/* Paginación Responsive */
#pagination {
    display: flex;
    flex-wrap: wrap;
    gap: 0.25rem;
    align-items: center;
}

#pagination span {
    color: #6c757d;
    font-weight: 500;
    padding: 0 0.25rem;
    user-select: none;
}

/* Botones más pequeños en mobile */
@media (max-width: 575px) {
    #pagination .btn-sm {
        font-size: 0.7rem;
        padding: 0.25rem 0.5rem;
        min-width: 32px;
    }
    
    #pagination span {
        font-size: 0.8rem;
    }
}
```

---

