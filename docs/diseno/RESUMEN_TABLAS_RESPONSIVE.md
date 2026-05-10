# Sistema de tablas responsive

## Resumen
El proyecto tiene un patrón CSS para mostrar tablas en desktop y tarjetas en móvil.

## Archivos reales
- `src/main/resources/static/shared/css/responsive-table.css`
- `src/main/resources/templates/shared/components/responsive-table.html`
- `src/main/resources/templates/shared/layout.html`
- `docs/guias/GUIA_TABLAS_RESPONSIVE.md`

## Estado actual
- `layout.html` ya importa `responsive-table.css`.
- El patrón está aplicado manualmente en módulos como `producto`, `cliente`, `facturacion` y `pagos`.
- El fragmento Thymeleaf existe, pero la implementación visible hoy usa markup propio por módulo.

## Clases principales
- `.responsive-table-container`
- `.table-responsive`
- `.mobile-card-view`
- `.mobile-card`
- `.mobile-card-header`
- `.mobile-card-body`
- `.mobile-card-row`
- `.mobile-card-footer`

## Referencias
- `docs/diseno/responsive/tablas-responsive.md`
- `src/main/resources/templates/modules/producto/productos.html`
- `src/main/resources/templates/modules/facturacion/facturas.html`
- `src/main/resources/templates/modules/cliente/clientes.html`
- `src/main/resources/templates/modules/facturacion/pagos/listar.html`

**Tiempo estimado de implementación por módulo**: 15-30 minutos

---

**Desarrollador**: GitHub Copilot  
**Fecha**: 2 de mayo de 2026  
**Estado**: ✅ Documentado
