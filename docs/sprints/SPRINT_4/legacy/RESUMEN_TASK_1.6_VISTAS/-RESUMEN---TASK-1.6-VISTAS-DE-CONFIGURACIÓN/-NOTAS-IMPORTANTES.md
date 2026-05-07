## 📌 NOTAS IMPORTANTES

### Consideraciones técnicas:

1. **Thymeleaf Fragments:**
   - Los fragments usan sintaxis `th:fragment="nombreFragment"`
   - Se incluyen con `th:replace="~{ruta :: fragment}"`
   - Variables Thymeleaf disponibles: `${activeTab}`, `${success}`, `${error}`

2. **Bootstrap 5:**
   - Tabs con `data-bs-toggle="tab"`
   - Modales con `data-bs-dismiss="modal"`
   - Grid system responsive
   - Utilities: `d-flex`, `justify-content-between`, `align-items-center`

3. **Font Awesome:**
   - Iconos: `fas fa-[nombre]`
   - Tamaños: `fa-2x`, `fa-3x`, `fa-4x`
   - Clases adicionales: `me-2` (margin-end), `text-primary`

4. **Formularios:**
   - Validación HTML5: `required`, `pattern`, `min`, `max`, `email`
   - Switches: `form-check-input` + `form-check-label`
   - Input groups para botones adyacentes

5. **JavaScript pendiente:**
   - Usar `fetch()` para AJAX
   - Promesas/async-await
   - Toast notifications (Bootstrap o custom)
   - Event listeners en formularios
   - Manipulación del DOM

---

