## 🎯 TESTING MANUAL - SCRIPT GUIADO

### **Script para Chrome:**

```
1. Abrir Chrome
2. Navegar a http://localhost:8080
3. F12 (abrir DevTools)
4. Login con: admin / password
5. Validar Dashboard:
   ✓ 4 widgets visibles
   ✓ Estadísticas correctas
   ✓ Módulos renderizados
6. Click en "Productos"
   ✓ Tabla se carga
   ✓ Paginación funciona
   ✓ Botones operan
7. Cambiar a página 5
   ✓ Paginación muestra: [<] 1 ... 4 5 6 ... 17 [>]
8. Resize window a mobile (F12 > Device Toolbar)
   ✓ Paginación muestra: [<] 1 ... 4 5 6 ... 17 [>]
   ✓ Tabla responsive (columnas ocultas)
9. Click en "Perfil"
   ✓ Breadcrumbs: Dashboard → Perfil
   ✓ Datos de usuario visibles
10. Logout
    ✓ Redirige a /login
11. Revisar Console
    ✓ Sin errores JavaScript
12. ✅ CHROME APROBADO
```

### **Script para Firefox:**

```
(Repetir mismo script que Chrome)

Diferencias esperadas:
- DevTools tienen layout diferente
- Scrollbars pueden verse diferentes
- Fuentes pueden tener antialiasing diferente

Si todo funciona igual: ✅ FIREFOX APROBADO
```

### **Script para Edge:**

```
(Repetir mismo script que Chrome)

Edge (Chromium) debería ser idéntico a Chrome.

Si todo funciona igual: ✅ EDGE APROBADO
```

---

