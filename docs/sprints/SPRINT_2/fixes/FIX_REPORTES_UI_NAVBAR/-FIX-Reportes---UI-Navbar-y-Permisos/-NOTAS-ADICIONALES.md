## 📌 NOTAS ADICIONALES

### ¿Por qué Reportes solo para ADMIN y USER?

**Razones de diseño:**
1. **Información sensible:** Los reportes muestran datos financieros agregados (ventas totales, deudas, etc.)
2. **Nivel de acceso:** USER y ADMIN son roles de gestión que necesitan análisis de datos
3. **VENDEDOR:** Su trabajo es operativo (crear/editar facturas), no requiere análisis estadístico
4. **VISUALIZADOR:** Es un rol de auditoría/consulta de documentos específicos, no de reportes agregados

### Estructura de Carpetas de Templates

```
templates/
├── components/         ← Componentes reutilizables (navbar, sidebar)
├── fragments/          ← No se usa actualmente
├── layout.html         ← Layout base
├── clientes/
├── productos/
├── facturas/
├── reportes/          ← Nuevo módulo (4 vistas)
│   ├── index.html
│   ├── ventas.html
│   ├── clientes.html
│   └── productos.html
└── ...
```

---

