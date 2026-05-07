## 🔗 DEPENDENCIAS

### Dependencias Técnicas

**Frontend (ya incluidas):**
- Bootstrap 5 (responsive)
- Chart.js 4.4.0 (gráficos)
- Font Awesome (iconos)

**Nuevas herramientas:**
- Lighthouse CI (testing automatizado)
- axe-core (accesibilidad)
- Workbox (Service Worker - opcional)

### Configuración Servidor

**Nginx - Compresión y caché:**
```nginx
# Gzip
gzip on;
gzip_types text/css application/javascript image/svg+xml;

# Brotli (opcional)
brotli on;
brotli_types text/css application/javascript;

# Cache
location ~* \.(css|js|jpg|jpeg|png|gif|svg|woff|woff2)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}
```

---

