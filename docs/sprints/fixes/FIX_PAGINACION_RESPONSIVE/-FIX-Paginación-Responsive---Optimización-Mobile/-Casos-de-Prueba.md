## 🧪 Casos de Prueba

### ✅ **Caso 1: Mobile - Página Inicial**
- **Entrada:** Página 1 de 17, pantalla 375px
- **Esperado:** `[<] 1 2 3 ... 17 [>]`
- **Resultado:** ✅ PASS

### ✅ **Caso 2: Mobile - Página Intermedia**
- **Entrada:** Página 9 de 17, pantalla 375px
- **Esperado:** `[<] 1 ... 8 9 10 ... 17 [>]`
- **Resultado:** ✅ PASS

### ✅ **Caso 3: Mobile - Última Página**
- **Entrada:** Página 17 de 17, pantalla 375px
- **Esperado:** `[<] 1 ... 15 16 17 [>]`
- **Resultado:** ✅ PASS

### ✅ **Caso 4: Resize - Desktop → Mobile**
- **Entrada:** Cambiar de 1200px a 375px
- **Esperado:** Paginación se recalcula automáticamente
- **Resultado:** ✅ PASS (con debounce de 250ms)

### ✅ **Caso 5: Pocas Páginas**
- **Entrada:** 5 páginas totales, pantalla 375px
- **Esperado:** `[<] 1 2 3 4 5 [>]` (todas visibles)
- **Resultado:** ✅ PASS

---

