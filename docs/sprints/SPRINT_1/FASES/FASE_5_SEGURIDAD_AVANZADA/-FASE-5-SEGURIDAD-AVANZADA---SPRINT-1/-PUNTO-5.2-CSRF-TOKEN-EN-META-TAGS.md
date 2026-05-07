## ⏳ PUNTO 5.2: CSRF TOKEN EN META TAGS

### 📝 Descripción

Agregar meta tags con token CSRF en `layout.html` para facilitar su uso en JavaScript.

### 🎯 Objetivos

- ✅ Agregar `<meta name="_csrf" th:content="${_csrf.token}"/>`
- ✅ Agregar `<meta name="_csrf_header" th:content="${_csrf.headerName}"/>`
- ✅ Actualizar `navbar.js` para usar meta tags
- ✅ Actualizar todos los JS que usen AJAX

### 📄 Código Implementado

#### layout.html

```html
<head th:fragment="head">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    
    <!-- CSRF Token para JavaScript -->
    <meta name="_csrf" th:content="${_csrf.token}"/>
    <meta name="_csrf_header" th:content="${_csrf.headerName}"/>
    
    <title th:text="${title ?: 'WhatsApp Orders Manager'}"></title>
    <!-- ... resto del código ... -->
</head>
```

#### navbar.js (handleLogout)

```javascript
async function handleLogout(event) {
    event.preventDefault();
    
    const confirmed = await AppUtils.showConfirmDialog(
        '¿Cerrar sesión?',
        'Estás a punto de cerrar tu sesión. ¿Deseas continuar?',
        'Sí, cerrar sesión'
    );

    if (confirmed) {
        AppUtils.showLoading();
        
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = '/logout';
        
        // Usar CSRF token desde meta tag
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        
        if (csrfToken) {
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = '_csrf';
            input.value = csrfToken;
            form.appendChild(input);
        }
        
        document.body.appendChild(form);
        form.submit();
    }
}
```

### 📊 Estado

- **Estado:** ✅ Completado
- **Progreso:** 100%
- **Fecha:** 12/10/2025
- **Responsable:** GitHub Copilot

### ✅ Validación

- ✅ Meta tags presentes en HTML
- ✅ JavaScript puede leer token CSRF
- ✅ Logout funciona correctamente con CSRF token
- ✅ No hay errores 403 Forbidden

---

