## 🔢 PARTE 2: PAGINACIÓN RESPONSIVE

### **Problema Original:**
```
[<] 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 [>]
```
- 17 botones ocupando ~850px
- Overflow horizontal en móvil
- Difícil navegar en pantallas pequeñas

### **Solución: Algoritmo Sliding Window**

#### **2.1. Detección de Pantalla**

```javascript
const screenWidth = window.innerWidth;
let maxVisiblePages;

if (screenWidth < 576)       maxVisiblePages = 3;  // Mobile
else if (screenWidth < 768)  maxVisiblePages = 5;  // Mobile grande
else if (screenWidth < 992)  maxVisiblePages = 7;  // Tablet
else                         maxVisiblePages = 10; // Desktop
```

#### **2.2. Cálculo de Rango Visible**

```javascript
// Centrar en página actual
const halfVisible = Math.floor(maxVisiblePages / 2);
startPage = Math.max(1, currentPage - halfVisible);
endPage = Math.min(pageCount, startPage + maxVisiblePages - 1);

// Ajustar si llegamos al final
if (endPage === pageCount) {
    startPage = Math.max(1, pageCount - maxVisiblePages + 1);
}
```

#### **2.3. Renderizado con Separadores**

```javascript
// Siempre mostrar primera página
if (startPage > 1) {
    // Botón [1]
    if (startPage > 2) {
        // Separador [...]
    }
}

// Rango visible
for (let i = startPage; i <= endPage; i++) {
    // Botones [startPage ... endPage]
}

// Siempre mostrar última página
if (endPage < pageCount) {
    if (endPage < pageCount - 1) {
        // Separador [...]
    }
    // Botón [pageCount]
}
```

#### **2.4. Ejemplos por Pantalla**

**Mobile (<576px) - Página 9 de 17:**
```
[<] 1 ... 8 9 10 ... 17 [>]
```
- Total botones: 7
- Ancho: ~200px

**Tablet (768-991px) - Página 9 de 17:**
```
[<] 1 ... 6 7 8 9 10 11 12 ... 17 [>]
```
- Total botones: 11
- Ancho: ~400px

**Desktop (>992px) - Página 9 de 17:**
```
[<] 1 ... 4 5 6 7 8 9 10 11 12 13 ... 17 [>]
```
- Total botones: 14
- Ancho: ~550px

#### **2.5. Auto-Actualización al Redimensionar**

```javascript
// Re-renderizar al cambiar tamaño de ventana
let resizeTimer;
window.addEventListener('resize', () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => {
        renderPagination();
    }, 250); // Debounce de 250ms
});
```

---

