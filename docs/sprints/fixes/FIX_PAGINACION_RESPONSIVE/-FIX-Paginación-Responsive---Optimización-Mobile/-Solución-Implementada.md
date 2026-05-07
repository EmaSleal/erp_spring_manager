## 🎯 Solución Implementada

### **Algoritmo de Paginación Inteligente:**

Calcula dinámicamente cuántas páginas mostrar según el ancho de pantalla, con lógica de "sliding window" centrada en la página actual.

#### **1. Detección de Ancho de Pantalla**

```javascript
const screenWidth = window.innerWidth;
let maxVisiblePages;

if (screenWidth < 576) {
    maxVisiblePages = 3; // Mobile: mostrar máximo 3 páginas
} else if (screenWidth < 768) {
    maxVisiblePages = 5; // Mobile grande: 5 páginas
} else if (screenWidth < 992) {
    maxVisiblePages = 7; // Tablet: 7 páginas
} else {
    maxVisiblePages = 10; // Desktop: 10 páginas
}
```

#### **2. Cálculo de Rango Visible**

```javascript
// Calcular rango centrado en la página actual
const halfVisible = Math.floor(maxVisiblePages / 2);
startPage = Math.max(1, currentPage - halfVisible);
endPage = Math.min(pageCount, startPage + maxVisiblePages - 1);

// Ajustar si llegamos al final
if (endPage === pageCount) {
    startPage = Math.max(1, pageCount - maxVisiblePages + 1);
}
```

#### **3. Renderizado con Separadores**

```javascript
// Primera página siempre visible
if (startPage > 1) {
    // Botón "1"
    // ...separador "..." si startPage > 2
}

// Rango visible (ej: 5 6 7 8 9)
for (let i = startPage; i <= endPage; i++) {
    // Botones de páginas
}

// Última página siempre visible
if (endPage < pageCount) {
    // ...separador "..." si endPage < pageCount - 1
    // Botón "17"
}
```

---

