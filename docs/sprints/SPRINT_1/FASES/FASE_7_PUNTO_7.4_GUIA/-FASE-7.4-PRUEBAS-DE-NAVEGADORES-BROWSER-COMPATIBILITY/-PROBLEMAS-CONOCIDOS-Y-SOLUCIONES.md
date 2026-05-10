## 🐛 PROBLEMAS CONOCIDOS Y SOLUCIONES

### **1. Flexbox Gap en Safari < 14.1**

**Problema:**
```css
.container {
    display: flex;
    gap: 1rem; /* No funciona en Safari < 14.1 */
}
```

**Solución (si necesario):**
```css
.container > * {
    margin-right: 1rem;
}
.container > *:last-child {
    margin-right: 0;
}
```

**Estado:** ✅ No requerido (Safari 14.1+ soportado)

---

### **2. Sticky Position en Safari**

**Problema:**
```css
.table td:last-child {
    position: sticky;
    right: 0; /* Puede tener glitches en Safari */
}
```

**Solución:**
```css
/* Asegurar z-index y background */
.table td:last-child {
    position: -webkit-sticky; /* Prefijo para Safari antiguo */
    position: sticky;
    right: 0;
    z-index: 1;
    background-color: white; /* Requerido */
}
```

**Estado:** ✅ Implementado en common.css

---

### **3. Scrollbar Styling en Firefox**

**Problema:**
```css
::-webkit-scrollbar {
    /* No funciona en Firefox */
}
```

**Solución:**
```css
/* Firefox usa propiedades diferentes */
* {
    scrollbar-width: thin;
    scrollbar-color: #888 #f1f1f1;
}
```

**Estado:** ⏳ Opcional (no crítico)

---

