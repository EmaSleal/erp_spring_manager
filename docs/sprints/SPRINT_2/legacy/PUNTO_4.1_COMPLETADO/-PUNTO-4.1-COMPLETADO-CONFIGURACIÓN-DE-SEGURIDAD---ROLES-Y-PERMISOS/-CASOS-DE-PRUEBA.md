## 🧪 CASOS DE PRUEBA

### Test 1: ADMIN accede a todo
```
Login: admin@example.com (ADMIN)
Intentar acceder:
- /dashboard          → ✅ OK
- /clientes           → ✅ OK
- /clientes/form      → ✅ OK
- /productos          → ✅ OK
- /facturas           → ✅ OK
- /configuracion      → ✅ OK
- /usuarios           → ✅ OK
- /reportes           → ✅ OK
```

### Test 2: USER sin acceso administrativo
```
Login: user@example.com (USER)
Intentar acceder:
- /dashboard          → ✅ OK
- /clientes           → ✅ OK
- /clientes/form      → ✅ OK
- /productos          → ✅ OK
- /facturas           → ✅ OK
- /configuracion      → ❌ 403
- /usuarios           → ❌ 403
- /reportes           → ✅ OK
```

### Test 3: VENDEDOR solo crea facturas
```
Login: vendedor@example.com (VENDEDOR)
Intentar acceder:
- /dashboard          → ✅ OK
- /clientes           → ✅ OK (solo lectura)
- /clientes/form      → ❌ 403
- /productos          → ✅ OK (solo lectura)
- /productos/form     → ❌ 403
- /facturas           → ✅ OK
- /facturas/form      → ✅ OK
- /facturas/delete/1  → ❌ 403
- /configuracion      → ❌ 403
- /usuarios           → ❌ 403
- /reportes           → ❌ 403
```

### Test 4: VISUALIZADOR solo lectura
```
Login: visualizador@example.com (VISUALIZADOR)
Intentar acceder:
- /dashboard          → ✅ OK
- /clientes           → ✅ OK (solo lectura)
- /clientes/form      → ❌ 403
- /productos          → ✅ OK (solo lectura)
- /productos/form     → ❌ 403
- /facturas           → ✅ OK (solo lectura)
- /facturas/form      → ❌ 403
- /configuracion      → ❌ 403
- /usuarios           → ❌ 403
- /reportes           → ❌ 403
```

---

