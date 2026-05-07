## 🔍 Debugging Tips

### Verificar Orden de Carga en el Navegador

1. Abrir DevTools (F12)
2. Ir a la pestaña **Network**
3. Filtrar por **JS**
4. Recargar la página
5. Verificar que jQuery se carga **antes** que scripts personalizados

### Verificar que jQuery Está Disponible

En la consola del navegador:

```javascript
// Verificar jQuery está cargado
console.log(typeof $);          // Debe devolver "function"
console.log(typeof jQuery);     // Debe devolver "function"
console.log($.fn.jquery);       // Debe devolver versión (ej: "3.6.0")

// Si devuelve "undefined", jQuery NO está cargado
```

### Detectar Errores de Dependencias

```javascript
// En usuarios.js - Agregar al inicio temporalmente
if (typeof $ === 'undefined') {
    console.error('❌ ERROR: jQuery no está cargado!');
    alert('Error crítico: jQuery no está disponible. Contacte al administrador.');
} else {
    console.log('✅ jQuery está disponible:', $.fn.jquery);
}
```

