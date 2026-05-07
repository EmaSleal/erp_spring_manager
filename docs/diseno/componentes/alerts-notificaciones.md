# Alerts y notificaciones

Referencia para mensajes, toasts y confirmaciones en UI.

## Base común recomendada

- Utilidades compartidas en [static/shared/js/common.js](../../../src/main/resources/static/shared/js/common.js)
  - `showToast()`
  - `showConfirmDialog()`
  - `showLoading()` / `hideLoading()`

## Patrones observados

- SweetAlert2 como patrón principal en módulos de gestión.
- Bootstrap alerts en algunas vistas servidor-renderizadas.
- En notificaciones en tiempo real se usan toasts Bootstrap vía WebSocket.

## Diferencias a vigilar

- En seguridad hay implementación propia de toasts/alerts en JS:
  - [static/modules/seguridad/js/usuarios-admin.js](../../../src/main/resources/static/modules/seguridad/js/usuarios-admin.js)

## Decisión sugerida

- Mantener base común (`common.js`) para confirmaciones y feedback general.
- Permitir excepción solo cuando el flujo del módulo lo requiera explícitamente.

Ver evaluación completa: [consistencia-modulos.md](../sistema-visual/consistencia-modulos.md)
