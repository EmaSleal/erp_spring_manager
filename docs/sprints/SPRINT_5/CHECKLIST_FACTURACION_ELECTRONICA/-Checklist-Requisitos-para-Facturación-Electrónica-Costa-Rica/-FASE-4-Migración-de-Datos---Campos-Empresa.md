## 📦 FASE 4: Migración de Datos - Campos Empresa

**Estado:** ❌ **PENDIENTE**  
**Prioridad:** 🔴 **CRÍTICA**  
**Tiempo estimado:** 2-3 horas

### Tareas:

#### 4.1 Crear Script SQL de Migración
- [ ] Crear archivo: `docs/base de datos/MIGRATION_EMPRESA_FACTURACION_CR.sql`
- [ ] Agregar campo `codigo_actividad_economica` VARCHAR(6)
- [ ] Agregar campo `tipo_identificacion` VARCHAR(2) DEFAULT '02'
- [ ] Agregar campo `proveedor_sistemas` VARCHAR(20) DEFAULT '2100042005'
- [ ] Agregar campo `provincia` VARCHAR(2)
- [ ] Agregar campo `canton` VARCHAR(2)
- [ ] Agregar campo `distrito` VARCHAR(2)
- [ ] Agregar campo `barrio` VARCHAR(100)
- [ ] Agregar campo `otras_senas` VARCHAR(300)
- [ ] Crear índice: `idx_empresa_codigo_actividad`

**Script SQL:**
```sql
-- MIGRATION_EMPRESA_FACTURACION_CR.sql
-- Campos críticos para Facturación Electrónica Costa Rica

ALTER TABLE empresa 
ADD COLUMN codigo_actividad_economica VARCHAR(6) COMMENT 'Código de actividad económica inscrita en Hacienda CR (Ej: 4773.0)';

ALTER TABLE empresa 
ADD COLUMN tipo_identificacion VARCHAR(2) DEFAULT '02' COMMENT '01=Física, 02=Jurídica, 03=DIMEX, 04=NITE';

ALTER TABLE empresa 
ADD COLUMN proveedor_sistemas VARCHAR(20) DEFAULT '2100042005' COMMENT 'Cédula del proveedor de sistemas de facturación electrónica';

ALTER TABLE empresa 
ADD COLUMN provincia VARCHAR(2) COMMENT 'Código provincia CR (1=San José, 2=Alajuela, 3=Cartago, 4=Heredia, 5=Guanacaste, 6=Puntarenas, 7=Limón)';

ALTER TABLE empresa 
ADD COLUMN canton VARCHAR(2) COMMENT 'Código cantón dentro de la provincia';

ALTER TABLE empresa 
ADD COLUMN distrito VARCHAR(2) COMMENT 'Código distrito dentro del cantón';

ALTER TABLE empresa 
ADD COLUMN barrio VARCHAR(100) COMMENT 'Nombre del barrio (opcional)';

ALTER TABLE empresa 
ADD COLUMN otras_senas VARCHAR(300) COMMENT 'Dirección descriptiva para ubicación (Ej: 150MTS SUR DE LA ESCUELA)';

-- Índices para búsqueda
CREATE INDEX idx_empresa_codigo_actividad ON empresa(codigo_actividad_economica);
CREATE INDEX idx_empresa_provincia ON empresa(provincia);

-- Validar estructura
SHOW CREATE TABLE empresa;
```

#### 4.2 Ejecutar Migración
- [ ] Backup de base de datos actual
- [ ] Ejecutar script SQL en desarrollo
- [ ] Verificar estructura con `SHOW CREATE TABLE empresa`
- [ ] Validar no hay errores

#### 4.3 Actualizar Entidad JPA
- [ ] Abrir: `src/main/java/api/astro/whats_orders_manager/models/Empresa.java`
- [ ] Agregar campo `codigoActividadEconomica`
- [ ] Agregar campo `tipoIdentificacion`
- [ ] Agregar campo `proveedorSistemas`
- [ ] Agregar campo `provincia`
- [ ] Agregar campo `canton`
- [ ] Agregar campo `distrito`
- [ ] Agregar campo `barrio`
- [ ] Agregar campo `otrasSenas`
- [ ] Agregar validaciones @NotNull donde corresponda

#### 4.4 Crear Enums
- [ ] Crear `TipoIdentificacion.java` enum (01, 02, 03, 04)
- [ ] Crear `ProvinciaCR.java` enum (1-7 con nombres)

#### 4.5 Actualizar Formulario Web
- [ ] Abrir: `templates/configuracion/empresa.html`
- [ ] Agregar campo select para `tipoIdentificacion`
- [ ] Agregar campo input para `codigoActividadEconomica`
- [ ] Agregar campo input para `proveedorSistemas`
- [ ] Agregar selects para ubicación: provincia, cantón, distrito
- [ ] Agregar textarea para `otrasSenas`
- [ ] Agregar tooltips explicativos
- [ ] Validar formato de códigos (6 dígitos actividad, 2 dígitos ubicación)

---

