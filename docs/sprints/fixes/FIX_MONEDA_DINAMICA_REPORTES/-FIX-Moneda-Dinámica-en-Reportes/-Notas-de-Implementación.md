## 📝 Notas de Implementación

### Pendientes (Opcional):
- [ ] Actualizar `reportes/clientes.html` si tiene montos
- [ ] Actualizar gráficos de Chart.js para usar símbolo dinámico
- [ ] Crear directiva Thymeleaf personalizada para formateo de moneda
- [ ] Agregar configuración de separador de miles y decimales

### Consideraciones:
- El símbolo se obtiene **una sola vez** por request (eficiente)
- Usa el operador Elvis `?:` para fallback en templates
- Compatible con Thymeleaf 3.x
- No requiere cambios en la base de datos

---

