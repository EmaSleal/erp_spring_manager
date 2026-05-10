# Checklist de conformidad visual por módulo

Objetivo: evaluar si una pantalla sigue el sistema visual común o si su diferencia es una excepción válida.

## Semáforo

- 🟢 Alineado
- 🟡 Parcial / revisable
- 🔴 Desalineado

## Criterios

| Criterio | Qué revisar |
|---|---|
| Layout base | Usa `shared/layout.html` y `main-content` |
| Navbar/Sidebar | Reutiliza componentes compartidos |
| Tipografía y colores | Usa variables de `common.css` |
| Cards y tablas | Sigue estilos comunes o componentes compartidos |
| Responsive | Tiene comportamiento coherente en móvil |
| Alerts/confirmaciones | Usa utilidades compartidas cuando aplica |
| Inline styles | Mínimos o justificados |

## Evaluación rápida por módulo

| Módulo | Layout | Navbar/Sidebar | Colores | Responsive | Alerts | Estado |
|---|---|---|---|---|---|---|
| Producto | 🟢 | 🟢 | 🟢 | 🟢 | 🟢 | 🟢 |
| Facturación | 🟢 | 🟢 | 🟢 | 🟢 | 🟢 | 🟢 |
| Reportes | 🟢 | 🟢 | 🟢 | 🟢 | 🟢 | 🟢 |
| Cliente | 🟢 | 🟢 | 🟢 | 🟡 | 🟡 | 🟡 |
| Configuración | 🟡 | 🟢 | 🟡 | 🟡 | 🟡 | 🟡 |
| Seguridad | 🟡 | 🟢 | 🟡 | 🟡 | 🟡 | 🟡 |
| Contabilidad | 🟢 | 🟢 | 🟢 | 🟡 | 🟢 | 🟢 |
| WhatsApp | 🟡 | 🟢 | 🟡 | 🟡 | 🟡 | 🟡 |

## Interpretación

- **🟢**: seguir como referencia.
- **🟡**: revisar si es excepción funcional o deuda técnica.
- **🔴**: candidato a refactor inmediato.

## Reglas de decisión

1. Si el módulo necesita UI especial para resolver su dominio, marcar como **excepción**.
2. Si repite patrones ya resueltos en shared, marcar como **refactor**.
3. Si afecta varias pantallas, planificar migración gradual.

## Referencias

- [Consistencia visual por módulos](consistencia-modulos.md)
- [Paleta de colores](paleta-colores.md)
- [Iconografía](iconografia.md)
- [Alerts y notificaciones](../componentes/alerts-notificaciones.md)
