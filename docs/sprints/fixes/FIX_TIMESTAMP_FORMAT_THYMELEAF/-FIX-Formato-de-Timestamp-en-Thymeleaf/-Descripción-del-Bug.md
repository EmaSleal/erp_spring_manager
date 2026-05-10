## 🐛 Descripción del Bug

### Error en Logs

```
org.springframework.expression.spel.SpelEvaluationException: EL1004E: Method call: 
Method format(java.sql.Timestamp,java.lang.String) cannot be found on type 
org.thymeleaf.expression.Temporals

Exception evaluating SpringEL expression: 
"#temporals.format(usuario.ultimoAcceso, 'dd/MM/yyyy HH:mm')" 
(template: "usuarios/usuarios" - line 262, col 43)
```

### Ubicación

**Archivo:** `src/main/resources/templates/usuarios/usuarios.html`  
**Línea:** 262

### Código Problemático (ANTES)

```html
<td class="text-center">
    <small class="text-muted" th:if="${usuario.ultimoAcceso != null}">
        <i class="bi bi-clock-history me-1"></i>
        <!-- ❌ ERROR: Thymeleaf no puede formatear Timestamp directamente -->
        <span th:text="${#temporals.format(usuario.ultimoAcceso, 'dd/MM/yyyy HH:mm')}">
            01/01/2025 10:00
        </span>
    </small>
    <small class="text-muted fst-italic" th:if="${usuario.ultimoAcceso == null}">
        Nunca
    </small>
</td>
```

---

