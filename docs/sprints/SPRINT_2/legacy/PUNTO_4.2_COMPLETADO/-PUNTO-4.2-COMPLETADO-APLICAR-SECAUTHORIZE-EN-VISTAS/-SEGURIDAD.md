## 🔐 SEGURIDAD

### Capas de seguridad implementadas:

**Capa 1: Backend (SecurityConfig)**
```java
.requestMatchers("/clientes/form/**").hasAnyRole("ADMIN", "USER")
```
- ✅ Bloquea acceso a nivel de servidor
- ✅ Devuelve 403 si se intenta acceder directamente

**Capa 2: Frontend (sec:authorize)**
```html
<button sec:authorize="hasAnyRole('ADMIN', 'USER')">Editar</button>
```
- ✅ Oculta elementos no permitidos
- ✅ Mejora UX al no mostrar opciones inaccesibles

**Capa 3: JavaScript (para tablas dinámicas)**
```javascript
style="${(userRole === 'ROLE_VENDEDOR') ? 'display:none;' : ''}"
```
- ✅ Aplica restricciones en contenido renderizado por JS
- ✅ Complementa sec:authorize donde no es posible usarlo

**Resultado:** Sistema seguro en múltiples capas con buena experiencia de usuario

---

