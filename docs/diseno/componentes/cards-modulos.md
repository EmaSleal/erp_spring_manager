# Cards de módulos

Referencia visual para tarjetas de acceso a módulos.

## Componente base

- [templates/shared/components/modules-grid.html](../../../src/main/resources/templates/shared/components/modules-grid.html)

## Estados soportados

- Activo (`modulo.activo = true`)
- Inactivo/próximamente (`modulo.activo = false`)

## Convención visual

- Icono grande por módulo
- Nombre + descripción corta
- Badge de estado (`Disponible` / `Próximamente`)

## Uso actual destacado

- Contabilidad consume este componente para submódulos:
  - [templates/modules/contabilidad/contabilidad.html](../../../src/main/resources/templates/modules/contabilidad/contabilidad.html)

## Nota de consistencia

Este componente ayuda a mantener uniformidad visual entre módulos. Si un módulo usa cards propias, validar si es excepción o si conviene migrar al componente compartido.

Ver evaluación general: [consistencia-modulos.md](../sistema-visual/consistencia-modulos.md)
