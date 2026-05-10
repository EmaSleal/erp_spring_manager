# Animaciones

Referencia de transiciones y efectos visuales.

## Base compartida
- `src/main/resources/static/shared/css/common.css`

## Variables útiles
- `--transition-fast: 0.2s ease`
- `--transition-normal: 0.3s ease`
- `--transition-slow: 0.5s ease`

## Patrones observados
- Hover suave en cards y botones
- Sidebar móvil con `transform 0.3s ease-in-out`
- Dropdowns y overlays con opacidad/transición corta

## Nota
- Reutilizar tokens globales cuando la animación se repite.
- Si la animación es de dominio, documentarla como excepción.
