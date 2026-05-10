## 📝 NOTAS TÉCNICAS

### ¿Por qué @ControllerAdvice?
- ✅ Centraliza lógica común
- ✅ Se ejecuta antes de cada controller
- ✅ Evita código duplicado
- ✅ Fácil de mantener
- ✅ Testeable de forma aislada

### ¿Por qué calcular iniciales en backend?
- ✅ Consistencia: siempre el mismo cálculo
- ✅ Performance: se calcula una vez, no en cada renderizado
- ✅ Reutilizable: disponible en cualquier vista
- ✅ Testeable: lógica de negocio en Java

### ¿Por qué dos tamaños de avatar?
- ✅ UX: Avatar pequeño (36px) ahorra espacio en navbar
- ✅ UX: Avatar grande (48px) es más visible en dropdown
- ✅ Responsive: Se adapta al contexto

### ¿Por qué usar `th:classappend`?
- ✅ Permite agregar clases dinámicamente
- ✅ Mantiene clases existentes
- ✅ Thymeleaf evalúa expresiones complejas
- ✅ Código más limpio que múltiples `th:if`

---

