## 🎓 Lecciones Aprendidas

1. **`.table-responsive` solo no es suficiente:**
   - Bootstrap wrapper permite scroll, pero no optimiza contenido
   - Se requiere ocultación inteligente de columnas

2. **Media queries + Utility classes = Mejor solución:**
   - CSS maneja estilos generales (tamaños, padding)
   - Bootstrap classes manejan visibilidad por columna

3. **JavaScript rendering debe coincidir con templates:**
   - Las clases de `<th>` deben replicarse en `<td>`
   - Mantener consistencia entre server-side y client-side rendering

4. **Sticky columns mejoran UX:**
   - Acciones siempre visibles incluso con scroll horizontal
   - Shadow proporciona feedback visual de posición sticky

---

