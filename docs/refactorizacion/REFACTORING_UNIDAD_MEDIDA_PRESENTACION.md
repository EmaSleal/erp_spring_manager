# Refactorización: Unidad de Medida desde Presentación

## 📋 Resumen
Se ha refactorizado el modelo de datos para utilizar la tabla `presentacion` como fuente de la unidad de medida para Facturación Electrónica, eliminando el campo redundante `unidadMedida` de la tabla `producto`.

## 🔧 Cambios Realizados

### 1. Base de Datos
**Tabla: `presentacion`**
- ✅ Agregada columna `codigo_unidad_fe VARCHAR(10)`
- ✅ Actualizado las 22 presentaciones existentes con códigos de Hacienda
- ✅ Insertadas 15 nuevas presentaciones con unidades FE estándar

**Tabla: `producto`**
- ❌ Eliminada columna `unidad_medida` (redundante)

### 2. Entidades Java

**Presentacion.java**
```java
@Column(name = "codigo_unidad_fe", length = 10)
private String codigoUnidadFE;
```

**Producto.java**
- ❌ Eliminado campo `unidadMedida`
- ✅ Mantiene relación `@ManyToOne` con `Presentacion`

### 3. Frontend

**form.html**
- ❌ Eliminado select de `unidadMedida`
- ✅ Agregada nota informativa: "La unidad de medida para FE se toma de la presentación"
- ✅ **CORREGIDO BUG**: Agregado `value="true"` a checkboxes `gravado` y `aplicaOtroImpuesto`

**productos.js**
- ❌ Eliminada línea que cargaba `producto.unidadMedida`

## 📊 Mapeo de Presentaciones Existentes

| ID | Nombre | Código FE |
|----|--------|-----------|
| 1  | Ud | Unid |
| 2  | 1/2 litro | l |
| 3  | Galón | Galon |
| 4  | Litro | l |
| 5  | 1/2 galón | Galon |
| 6  | 200 g | kg |
| 7  | 50 g | kg |
| 8  | 1 kg | kg |
| 9  | Pichinga | l |
| 10 | 250 ml | l |
| 11 | 10 kg | kg |
| 12 | 2,5 kg | kg |
| 13 | 20 kg | kg |
| 14 | 5 kg | kg |
| 15 | 100 ml | l |
| 16 | 500 ml | l |
| 17 | Amarillo | Unid |
| 18 | Blanco | Unid |
| 19 | Rosado | Unid |
| 20 | Tambor 208l | l |
| 21 | Eco | Unid |
| 22 | Botella | Unid |

## 🆕 Nuevas Presentaciones Agregadas

| Nombre | Código FE | Categoría |
|--------|-----------|-----------|
| Servicios Profesionales | Sp | Servicios |
| Metro | m | SI Base |
| Segundo | s | SI Base |
| Amperio | A | SI Base |
| Kelvin | K | SI Base |
| Mol | mol | SI Base |
| Candela | cd | SI Base |
| Metro cuadrado | m² | SI Derivada |
| Metro cúbico | m³ | SI Derivada |
| Metro por segundo | m/s | SI Derivada |
| Metro por segundo cuadrado | m/s² | SI Derivada |
| Uno por metro | 1/m | SI Derivada |
| Kilogramo por metro cúbico | kg/m³ | SI Derivada |
| Metro cúbico por segundo | m³/s | SI Derivada |
| Otros | Otros | Especial |

## 🐛 Bug Corregido

### Problema
El checkbox `gravado` siempre se guardaba como `true` aunque se desactivara.

### Causa
En HTML, los checkboxes sin el atributo `value` envían `on` cuando están marcados y no envían nada cuando están desmarcados. Spring Boot interpretaba cualquier valor como `true`.

### Solución
```html
<input type="checkbox" name="gravado" value="true">
<input type="checkbox" name="aplicaOtroImpuesto" value="true">
```

Ahora:
- ✅ Checkbox marcado: envía `gravado=true`
- ✅ Checkbox desmarcado: no envía nada, Spring Boot lo interpreta como `false`

## 📝 Script de Migración

**Ubicación:** `docs/base de datos/MIGRATION_PRESENTACION_FE.sql`

**Pasos:**
1. Agregar columna `codigo_unidad_fe`
2. Actualizar 22 presentaciones existentes
3. Insertar 15 nuevas presentaciones
4. Verificar resultados

## 🔄 Flujo de Facturación Electrónica

```
Usuario selecciona Presentación → Producto hereda codigoUnidadFE
                 ↓
         Al generar XML FE
                 ↓
    Se usa presentacion.codigoUnidadFE
         (ej: "l", "kg", "Unid")
```

## ✅ Ventajas del Cambio

1. **Eliminación de redundancia**: Un solo lugar para gestionar unidades de medida
2. **Centralización**: Cambios en unidades FE se hacen en una tabla
3. **Reutilización**: Múltiples productos comparten la misma presentación
4. **Mantenibilidad**: Más fácil agregar/modificar unidades FE
5. **Consistencia**: Garantiza que todos los productos de una presentación usen la misma unidad FE

## 📌 Notas Importantes

- La columna `codigo_unidad_fe` es **NULLABLE** para permitir presentaciones sin FE
- Al generar XML, si `codigo_unidad_fe` es NULL, usar valor por defecto "Otros"
- Los códigos FE deben coincidir con el catálogo oficial de Hacienda
- Se recomienda actualizar el módulo de Presentaciones para gestionar `codigoUnidadFE` desde UI

## 🚀 Próximos Pasos

1. ✅ Ejecutar `MIGRATION_PRESENTACION_FE.sql` en base de datos
2. ⏳ Fase 7: Agregar campos de FE a Factura
3. ⏳ Fase 8: Actualizar generador de XML con unidades de medida desde presentación
4. ⏳ Fase 9: Testing end-to-end
5. ⏳ Fase 10: Documentación final
