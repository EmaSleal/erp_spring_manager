## 🎓 Detalles Técnicos

### **Algoritmo Sliding Window:**

```
Total pages: 17
Current page: 9
Max visible: 3
Half visible: 1

Cálculo:
startPage = max(1, 9 - 1) = 8
endPage = min(17, 8 + 3 - 1) = 10

Resultado: [8, 9, 10]
Con primera/última: [1] ... [8, 9, 10] ... [17]
```

### **Breakpoints:**
| Pantalla | Ancho | Max Visible | Ejemplo |
|----------|-------|-------------|---------|
| Mobile | <576px | 3 páginas | `1 ... 8 9 10 ... 17` |
| Mobile Large | 576-767px | 5 páginas | `1 ... 7 8 9 10 11 ... 17` |
| Tablet | 768-991px | 7 páginas | `1 ... 6 7 8 9 10 11 12 ... 17` |
| Desktop | >992px | 10 páginas | `1 ... 4 5 6 7 8 9 10 11 12 13 ... 17` |

### **Debounce en Resize:**

```javascript
let resizeTimer;
window.addEventListener('resize', () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => {
        renderPagination();
    }, 250); // Esperar 250ms después del último resize
});
```

**Propósito:** Evitar renderizados excesivos durante el redimensionamiento continuo.

---

