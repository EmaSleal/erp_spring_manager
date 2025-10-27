# 📦 Documentación Legacy - Sprint 2

> **⚠️ IMPORTANTE: Estos archivos están DEPRECADOS**
> 
> Esta carpeta contiene archivos históricos que **NO son la fuente de verdad actual**.
> Se mantienen únicamente para referencia histórica y auditoría.

---

## 🚫 NO USAR ESTOS ARCHIVOS

**La documentación actual y consolidada está en:**
- `../fases/FASE_*.md` - Un archivo por fase con toda la información
- `../INDICE_SPRINT_2.md` - Índice maestro de navegación
- `../RESUMEN_SPRINT_2.md` - Resumen ejecutivo completo

---

## 📋 ¿Por Qué Fueron Deprecados?

### Problema Original:
La documentación del Sprint 2 tenía una estructura fragmentada:
- **35+ archivos** individuales (PUNTO_4.1, PUNTO_4.2, PUNTO_5.1, etc.)
- **Múltiples resúmenes redundantes** para cada fase
- **Difícil navegación** y búsqueda de información
- **Alta complejidad** para mantenimiento

### Solución Implementada:
Se consolidó toda la documentación en archivos por fase:
```
Antes (fragmentado):          →    Ahora (consolidado):
├── PUNTO_4.1_COMPLETADO.md   →    ├── fases/FASE_4_ROLES_PERMISOS.md
├── PUNTO_4.2_COMPLETADO.md   →    │
├── PUNTO_4.3_COMPLETADO.md   →    │
├── PUNTO_5.1_COMPLETADO.md   →    ├── fases/FASE_5_NOTIFICACIONES.md
├── PUNTO_5.2_COMPLETADO.md   →    │
├── PUNTO_5.3_COMPLETADO.md   →    │
├── PUNTO_5.3.1_COMPLETADO.md →    │
├── PUNTO_5.3.2_COMPLETADO.md →    │
├── PUNTO_5.3.3_COMPLETADO.md →    │
└── ...                       →    └── (1 archivo por fase)
```

---

## 🗺️ Mapa de Migración

### ¿Dónde encontrar la información ahora?

| Archivo Legacy | Documentación Actual |
|----------------|---------------------|
| `PUNTO_4.1_COMPLETADO.md` | `../fases/FASE_4_ROLES_PERMISOS.md` |
| `PUNTO_4.2_COMPLETADO.md` | `../fases/FASE_4_ROLES_PERMISOS.md` |
| `PUNTO_4.3_COMPLETADO.md` | `../fases/FASE_4_ROLES_PERMISOS.md` |
| `PUNTO_5.1_COMPLETADO.md` | `../fases/FASE_5_NOTIFICACIONES.md` |
| `PUNTO_5.2_COMPLETADO.md` | `../fases/FASE_5_NOTIFICACIONES.md` |
| `PUNTO_5.3_COMPLETADO.md` | `../fases/FASE_5_NOTIFICACIONES.md` |
| `PUNTO_5.3.1_COMPLETADO.md` | `../fases/FASE_5_NOTIFICACIONES.md` |
| `PUNTO_5.3.2_COMPLETADO.md` | `../fases/FASE_5_NOTIFICACIONES.md` |
| `PUNTO_5.3.3_COMPLETADO.md` | `../fases/FASE_5_NOTIFICACIONES.md` |
| `PUNTO_5.4.1_COMPLETADO.md` | `../fases/FASE_5_NOTIFICACIONES.md` |
| `PUNTO_5.4.2_COMPLETADO.md` | `../fases/FASE_5_NOTIFICACIONES.md` |
| `PUNTO_8.3_COMPLETADO.md` | `../fases/FASE_8_TESTING_OPTIMIZACION.md` |
| `FASE_2_COMPLETADA.md` | `../SPRINT_2_CHECKLIST.txt` (Fase 2) |
| `FASE_2_INTEGRACION_FACTURACION.md` | `../SPRINT_2_CHECKLIST.txt` (Fase 2) |
| `FASE_3_GESTION_USUARIOS_COMPLETADA.md` | `../fases/FASE_3_USUARIOS.md` |
| `FASE_5_NOTIFICACIONES_COMPLETADA.md` | `../fases/FASE_5_NOTIFICACIONES.md` |
| `FASE_7_COMPLETADA.md` | `../fases/FASE_7_INTEGRACION.md` |
| `CAMBIO_FACTURA_FECHA_PAGO.md` | `../fases/FASE_2_FACTURACION.md` (ver checklist) |
| `MEJORA_CAMPOS_FACTURA.md` | `../fases/FASE_2_FACTURACION.md` (ver checklist) |
| `RESUMEN_EJECUTIVO_FASE_5.md` | `../RESUMEN_SPRINT_2.md` (Fase 5) |
| `RESUMEN_FASE_5_PUNTOS_5.2_5.3.md` | `../RESUMEN_SPRINT_2.md` (Fase 5) |
| `FIX_MEJORA_NAVEGACION_REPORTES.md` | `../fixes/` (ver README_FIXES.md) |

---

## 📚 Cómo Navegar la Nueva Documentación

### 1. Empezar Aquí:
```
../INDICE_SPRINT_2.md
```
Índice maestro con toda la estructura del Sprint 2.

### 2. Resumen Ejecutivo:
```
../RESUMEN_SPRINT_2.md
```
Resumen completo con métricas, logros y análisis.

### 3. Por Fase:
```
../fases/
├── FASE_3_USUARIOS.md
├── FASE_4_ROLES_PERMISOS.md
├── FASE_5_NOTIFICACIONES.md
├── FASE_7_INTEGRACION.md
└── FASE_8_TESTING_OPTIMIZACION.md
```

### 4. Checklist Completo:
```
../SPRINT_2_CHECKLIST.txt
```
Todas las 95 tareas completadas.

---

## 🔍 Buscar Información Específica

### Por Funcionalidad:
1. Abrir `../INDICE_SPRINT_2.md`
2. Buscar en la sección "Búsqueda por Funcionalidad"
3. Seguir el enlace al documento consolidado

### Por Fase:
1. Identificar el número de fase (1-8)
2. Abrir `../fases/FASE_N_*.md`
3. Buscar la sección específica

### Por Tecnología:
1. Abrir `../INDICE_SPRINT_2.md`
2. Buscar en la sección "Búsqueda por Tecnología"
3. Ver todos los archivos relacionados

---

## ⏱️ Información de Deprecación

- **Fecha de Deprecación:** 20 de octubre de 2025
- **Razón:** Consolidación de documentación fragmentada
- **Archivos Movidos:** 22 archivos
- **Nueva Estructura:** `fases/` + índices maestros
- **Ventajas:** 
  - ✅ Navegación más simple (5 archivos vs 35+)
  - ✅ Información consolidada por tema
  - ✅ Eliminación de redundancias
  - ✅ Mantenimiento más fácil
  - ✅ Búsqueda más rápida

---

## 📖 Para Auditoría Histórica

Si necesitas revisar la documentación original por razones de auditoría:

1. **Los archivos en esta carpeta son las versiones originales**
2. **Contienen la misma información** que los archivos consolidados
3. **Están desactualizados** - no reciben más actualizaciones
4. **Use solo para:** 
   - Comparación histórica
   - Auditoría de cambios
   - Verificación de fechas originales
   - Análisis de evolución del proyecto

---

## ✅ Mejores Prácticas

### ✅ SÍ HACER:
- Usar los archivos en `../fases/`
- Consultar `../INDICE_SPRINT_2.md` para navegación
- Leer `../RESUMEN_SPRINT_2.md` para contexto general
- Actualizar solo los archivos consolidados

### ❌ NO HACER:
- Editar archivos en esta carpeta `legacy/`
- Referenciar estos archivos en nueva documentación
- Copiar contenido sin verificar versión actual
- Mover archivos fuera de `legacy/`

---

## 🆘 ¿Necesitas Ayuda?

Si no encuentras información que antes estaba en estos archivos:

1. **Buscar en:** `../INDICE_SPRINT_2.md` (sección "Búsqueda Rápida")
2. **Consultar:** `../RESUMEN_SPRINT_2.md` (resumen ejecutivo)
3. **Ver mapa:** Tabla de migración arriba en este README
4. **Checklist completo:** `../SPRINT_2_CHECKLIST.txt`

---

## 📝 Notas Adicionales

- Estos archivos **NO se eliminarán** - se mantienen para referencia histórica
- La información **NO se perdió** - está consolidada en `../fases/`
- La nueva estructura **es más mantenible** y fácil de navegar
- Cualquier actualización futura se hará en los archivos consolidados

---

**Última actualización:** 20 de octubre de 2025  
**Estado:** 🔒 CERRADO - Solo lectura (histórico)  
**Versión:** Legacy v1.0

---

## 🔗 Enlaces Rápidos

- [↗️ Volver al Sprint 2](../)
- [📋 Índice Maestro Sprint 2](../INDICE_SPRINT_2.md)
- [📊 Resumen Ejecutivo](../RESUMEN_SPRINT_2.md)
- [📁 Documentación por Fases](../fases/)
- [🔧 Fixes y Correcciones](../fixes/)
