# Consistencia visual por módulos

Objetivo: identificar qué módulos siguen el diseño común y dónde hay desviaciones.

## Base visual común esperada

- `templates/shared/layout.html`
- `templates/shared/components/navbar.html`
- `templates/shared/components/sidebar.html`
- `static/shared/css/common.css`
- `static/shared/css/navbar.css`
- `static/shared/css/sidebar.css`

## Módulos de referencia

| Módulo | Estado | Evidencia |
|---|---|---|
| Producto | ✅ Alineado | [templates/modules/producto/productos.html](../../../src/main/resources/templates/modules/producto/productos.html) |
| Facturación | ✅ Alineado | [templates/modules/facturacion/facturas.html](../../../src/main/resources/templates/modules/facturacion/facturas.html) |
| Reportes | ✅ Alineado | [templates/modules/reportes/index.html](../../../src/main/resources/templates/modules/reportes/index.html) |

## Módulos a evaluar por posible desviación

| Módulo | Hallazgo principal | Evidencia | Acción sugerida |
|---|---|---|---|
| Cliente | Mezcla de patrón común y `confirm()` nativo | [templates/modules/cliente/clientes.html](../../../src/main/resources/templates/modules/cliente/clientes.html) | Unificar confirmaciones con SweetAlert2 |
| Configuración | Tabs y sub-vistas con estilo propio | [templates/modules/configuracion/index.html](../../../src/main/resources/templates/modules/configuracion/index.html) | Validar si es excepción funcional |
| Seguridad | Variantes propias de stats/cards y toasts en JS | [templates/modules/seguridad/usuarios/lista-admin.html](../../../src/main/resources/templates/modules/seguridad/usuarios/lista-admin.html), [static/modules/seguridad/js/usuarios-admin.js](../../../src/main/resources/static/modules/seguridad/js/usuarios-admin.js) | Evaluar convergencia a `common.js` |
| Contabilidad | Patrón general con flujo propio | [templates/modules/contabilidad/contabilidad.html](../../../src/main/resources/templates/modules/contabilidad/contabilidad.html) | Mantener si el dominio lo exige |
| WhatsApp | UI conversacional especializada | [templates/modules/whatsapp/mensajes.html](../../../src/main/resources/templates/modules/whatsapp/mensajes.html) | Tratar como excepción de dominio |

## Criterios de decisión

Para cada desviación decidir:

1. **Excepción** si mejora claridad del dominio.
2. **Deuda técnica** si duplica patrones ya resueltos en shared.
3. **Migración gradual** si impacta varias pantallas.

## Próxima iteración recomendada

- Definir checklist mínimo por pantalla.
- Marcar por módulo: `Alineado`, `Excepción`, `Pendiente de refactor`.
- Priorizar refactor de alto impacto y bajo riesgo.

Ver checklist: [checklist-conformidad.md](checklist-conformidad.md)
