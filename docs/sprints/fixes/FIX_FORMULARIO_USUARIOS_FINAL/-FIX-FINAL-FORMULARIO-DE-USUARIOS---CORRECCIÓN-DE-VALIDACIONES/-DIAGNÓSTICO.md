## 🔍 DIAGNÓSTICO

### Problema 1: Scripts no se cargan correctamente
**Causa:** El archivo `usuarios.js` se incluye al final, **ANTES** de los scripts comunes del layout que incluyen jQuery y Bootstrap.

**Orden incorrecto actual:**
```html
<!-- Scripts específicos del formulario -->
<script th:src="@{/js/usuarios.js}"></script>  <!-- ❌ Se carga PRIMERO -->

<!-- Scripts comunes -->
<th:block th:replace="~{layout :: scripts}"></th:block>  <!-- ❌ jQuery y Bootstrap después -->
```

**Problema:** `usuarios.js` usa `$(document).ready()` pero jQuery no está cargado todavía.

---

### Problema 2: Inconsistencias en validación de teléfono
**Encontrado:**
- `pattern="[0-9]{9}"` pero `maxlength="8"` ❌ INCONSISTENTE
- Placeholder: `987654321` (9 dígitos) pero mensaje dice 8 dígitos
- JavaScript valida 9 dígitos pero HTML pide 8

---

### Problema 3: Event listeners no se registran
**Causa:** Los botones de contraseña intentan registrarse en `setupFormValidation()` que se llama desde `$(document).ready()`, pero este código se ejecuta antes de que jQuery esté disponible.

---

