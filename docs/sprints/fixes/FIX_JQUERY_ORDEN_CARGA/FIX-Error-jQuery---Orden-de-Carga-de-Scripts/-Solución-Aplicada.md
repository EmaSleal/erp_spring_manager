## 🔧 Solución Aplicada

### Cambio Realizado

Mover el script `usuarios.js` para que se cargue **DESPUÉS** del fragmento de scripts comunes:

```html
<!-- usuarios.html - CORREGIDO -->
</section>
        </div>
    </main>

    <!-- Scripts comunes (jQuery, Bootstrap, SweetAlert2) -->
    <th:block th:replace="~{layout :: scripts}"></th:block>
    
    <!-- Scripts específicos de usuarios (después de jQuery) -->
    <script th:src="@{/js/usuarios.js}"></script>
</body>
</html>
```

### Orden de Carga CORRECTO (Después del Fix)

```
1. layout.html (fragmento scripts)
   ├── jQuery 3.6.0           ✅ Se carga PRIMERO
   ├── Bootstrap 5.3.0        ✅
   ├── SweetAlert2            ✅
   ├── common.js              ✅
   ├── navbar.js              ✅
   └── sidebar.js             ✅

2. usuarios.js                ✅ Se carga AL FINAL (después de jQuery)
```

