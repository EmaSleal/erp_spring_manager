## 🔐 SEGURIDAD

### ¿Por qué POST en lugar de GET?

Spring Security **requiere POST para logout** por seguridad:

1. **Protección CSRF:** Los enlaces GET pueden ser atacados con CSRF (Cross-Site Request Forgery)
2. **Mejores prácticas REST:** Operaciones que cambian estado (como logout) deben usar POST/DELETE
3. **Prevención de ataques:** Un atacante no puede engañar a un usuario para que haga logout con solo un enlace malicioso

### ¿Por qué `/logout` en lugar de `/auth/logout`?

1. **Convención de Spring Security:** `/logout` es la URL por defecto
2. **Simplicidad:** Menos configuración = menos errores
3. **Compatibilidad:** Muchas librerías y plugins esperan `/logout`
4. **Mantenibilidad:** Código más estándar es más fácil de mantener

---

