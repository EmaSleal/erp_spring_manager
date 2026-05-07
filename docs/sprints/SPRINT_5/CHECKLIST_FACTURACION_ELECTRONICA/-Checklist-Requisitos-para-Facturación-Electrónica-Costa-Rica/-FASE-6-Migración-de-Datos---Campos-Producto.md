## 📦 FASE 6: Migración de Datos - Campos Producto

**Estado:** ❌ **PENDIENTE**  
**Prioridad:** 🔴 **CRÍTICA**  
**Tiempo estimado:** 3-4 horas

### Tareas:

#### 6.1 Crear Script SQL de Migración
- [ ] Crear archivo: `docs/base de datos/MIGRATION_PRODUCTO_CABYS_IVA.sql`
- [ ] Agregar campo `codigo_cabys` VARCHAR(13) **OBLIGATORIO**
- [ ] Agregar campo `unidad_medida` VARCHAR(20) DEFAULT 'Unid'
- [ ] Agregar campo `codigo_impuesto` VARCHAR(2) DEFAULT '01'
- [ ] Agregar campo `codigo_tarifa_iva` VARCHAR(2) DEFAULT '08'
- [ ] Agregar campo `tarifa_impuesto` DECIMAL(5,2) DEFAULT 13.00
- [ ] Agregar campo `tipo_tarifa` VARCHAR(20) (Gravado/Exento/Exonerado)
- [ ] Crear índice: `idx_producto_cabys`

**Script SQL:**
```sql
-- MIGRATION_PRODUCTO_CABYS_IVA.sql
ALTER TABLE producto 
ADD COLUMN codigo_cabys VARCHAR(13) NOT NULL COMMENT 'Código CABYS de 13 dígitos - OBLIGATORIO Hacienda CR';

ALTER TABLE producto 
ADD COLUMN unidad_medida VARCHAR(20) DEFAULT 'Unid' COMMENT 'Kg, L, m, Unid, Caja, Paquete, etc';

ALTER TABLE producto 
ADD COLUMN codigo_impuesto VARCHAR(2) DEFAULT '01' COMMENT '01=IVA, 02=Selectivo, 03=Único, 04=Específico, 07=Otros';

ALTER TABLE producto 
ADD COLUMN codigo_tarifa_iva VARCHAR(2) DEFAULT '08' COMMENT '08=13%, 10=0% (exento), 01=1%, 02=2%, 04=4%';

ALTER TABLE producto 
ADD COLUMN tarifa_impuesto DECIMAL(5,2) DEFAULT 13.00 COMMENT 'Porcentaje de impuesto aplicable';

ALTER TABLE producto 
ADD COLUMN tipo_tarifa VARCHAR(20) DEFAULT 'Gravado' COMMENT 'Gravado, Exento, Exonerado';

-- Índices
CREATE INDEX idx_producto_cabys ON producto(codigo_cabys);
CREATE INDEX idx_producto_tarifa ON producto(codigo_tarifa_iva);
CREATE INDEX idx_producto_tipo_tarifa ON producto(tipo_tarifa);

-- Validar
SHOW CREATE TABLE producto;
```

#### 6.2 Importar Catálogo CABYS
- [ ] Descargar catálogo oficial: https://www.hacienda.go.cr/ATV/ComprobanteElectronico/docs/esquemas/v43/Cabys/SistemaCABYS_versioned.xlsx
- [ ] Crear tabla auxiliar: `catalogo_cabys` (codigo, descripcion, categoria)
- [ ] Importar datos del Excel a tabla
- [ ] Crear servicio de búsqueda de CABYS

#### 6.3 Actualizar Entidad Producto
- [ ] Abrir: `modules/producto/model/Producto.java`
- [ ] Agregar campo `codigoCabys` **@NotNull**
- [ ] Agregar campo `unidadMedida`
- [ ] Agregar campo `codigoImpuesto`
- [ ] Agregar campo `codigoTarifaIva`
- [ ] Agregar campo `tarifaImpuesto`
- [ ] Agregar campo `tipoTarifa`

#### 6.4 Crear Enums de Impuestos
- [ ] Crear `CodigoTarifaIVA.java` enum (01-10)
- [ ] Crear `TipoTarifa.java` enum (GRAVADO, EXENTO, EXONERADO)
- [ ] Crear `UnidadMedida.java` enum (valores comunes)

#### 6.5 Actualizar Formulario Producto
- [ ] Abrir: `templates/productos/form.html`
- [ ] Agregar campo búsqueda/autocompletado para CABYS
- [ ] Agregar select para `unidadMedida`
- [ ] Agregar select para `codigoTarifaIva`
- [ ] Agregar input numérico para `tarifaImpuesto`
- [ ] Agregar radio buttons para `tipoTarifa`
- [ ] Calcular automáticamente precio con IVA incluido

#### 6.6 Crear API de Búsqueda CABYS
- [ ] Endpoint: `GET /api/catalogo/cabys/buscar?q={termino}`
- [ ] Retornar JSON con código, descripción, categoría
- [ ] Implementar paginación
- [ ] Integrar con autocompletado en formulario

---

