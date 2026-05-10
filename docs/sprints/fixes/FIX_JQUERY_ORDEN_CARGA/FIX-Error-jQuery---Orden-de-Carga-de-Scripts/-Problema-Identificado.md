## 📋 Problema Identificado

**Fecha:** 13 de octubre de 2025  
**Sprint:** Sprint 2 - Fase 5  
**Punto:** 5.3.2 (Envío de credenciales por email)  
**Severidad:** CRÍTICA - Bloqueaba toda la funcionalidad JavaScript

### Descripción del Error

Al hacer clic en el botón "Reenviar Credenciales" (o cualquier otro botón con eventos JavaScript), **no ocurría nada**. En la consola del navegador aparecía:

```javascript
Uncaught ReferenceError: $ is not defined
    at usuarios.js:641
```

### Causa Raíz

El archivo `usuarios.js` se estaba cargando **ANTES** de jQuery, lo que causaba que:

1. ❌ El símbolo `$` (jQuery) no estaba definido
2. ❌ Ningún event listener se registraba (`$(document).on(...)`)
3. ❌ Los botones no respondían a clics
4. ❌ Las funciones AJAX no funcionaban

### Orden de Carga INCORRECTO (Antes del Fix)

```html
<!-- usuarios.html -->
<section>
    <!-- Contenido de la página -->
    
    <!-- ❌ ORDEN INCORRECTO -->
    <script th:src="@{/js/usuarios.js}"></script>  <!-- Se carga PRIMERO -->
</section>
    </div>
</main>

<!-- Scripts comunes -->
<th:block th:replace="~{layout :: scripts}"></th:block>  <!-- jQuery se carga DESPUÉS -->
```

**Problema:** `usuarios.js` intenta usar `$` (jQuery) pero aún no está cargado.

