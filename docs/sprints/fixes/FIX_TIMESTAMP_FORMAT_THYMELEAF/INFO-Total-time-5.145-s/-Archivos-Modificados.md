## 📁 Archivos Modificados

### usuarios.html

**Ubicación:** `src/main/resources/templates/usuarios/usuarios.html`

**Línea modificada:** 262

**Cambio:**
```diff
- <span th:text="${#temporals.format(usuario.ultimoAcceso, 'dd/MM/yyyy HH:mm')}">
+ <span th:text="${#temporals.format(usuario.ultimoAcceso.toLocalDateTime(), 'dd/MM/yyyy HH:mm')}">
```

---

