## 🔍 ANÁLISIS DE LA CAUSA

### Problema 1: URL de Logout Incorrecta

**SecurityConfig.java tenía:**
```java
.logout(logout -> logout
    .logoutUrl("/auth/logout")  // ❌ URL incorrecta
    ...
)
```

**Spring Security por defecto espera:** `/logout` (no `/auth/logout`)

### Problema 2: Manejo Incorrecto del CSRF Token

**navbar.js tenía:**
```javascript
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
input.name = csrfHeader.replace('X-', '').toLowerCase();  // ❌ Nombre incorrecto
```

**Spring Security espera:** `_csrf` como nombre del parámetro (no el header transformado)

---

