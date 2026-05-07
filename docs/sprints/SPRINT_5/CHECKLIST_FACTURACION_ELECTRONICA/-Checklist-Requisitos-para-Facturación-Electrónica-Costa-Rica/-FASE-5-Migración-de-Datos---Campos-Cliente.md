## 📦 FASE 5: Migración de Datos - Campos Cliente

**Estado:** ❌ **PENDIENTE**  
**Prioridad:** 🔴 **CRÍTICA**  
**Tiempo estimado:** 2 horas

### Tareas:

#### 5.1 Crear Script SQL de Migración
- [ ] Crear archivo: `docs/base de datos/MIGRATION_CLIENTE_FACTURACION_CR.sql`
- [ ] Agregar campo `tipo_identificacion` VARCHAR(2) DEFAULT '01'
- [ ] Agregar campo `codigo_actividad_economica` VARCHAR(6) (opcional)
- [ ] Agregar campo `provincia` VARCHAR(2)
- [ ] Agregar campo `canton` VARCHAR(2)
- [ ] Agregar campo `distrito` VARCHAR(2)
- [ ] Agregar campo `otras_senas` VARCHAR(300)
- [ ] Crear índice: `idx_cliente_tipo_identificacion`

**Script SQL:**
```sql
-- MIGRATION_CLIENTE_FACTURACION_CR.sql
ALTER TABLE cliente 
ADD COLUMN tipo_identificacion VARCHAR(2) DEFAULT '01' COMMENT '01=Física, 02=Jurídica, 03=DIMEX, 04=NITE';

ALTER TABLE cliente 
ADD COLUMN codigo_actividad_economica VARCHAR(6) NULL COMMENT 'Opcional: para créditos fiscales y gastos deducibles';

ALTER TABLE cliente 
ADD COLUMN provincia VARCHAR(2) COMMENT 'Código provincia CR';

ALTER TABLE cliente 
ADD COLUMN canton VARCHAR(2) COMMENT 'Código cantón';

ALTER TABLE cliente 
ADD COLUMN distrito VARCHAR(2) COMMENT 'Código distrito';

ALTER TABLE cliente 
ADD COLUMN otras_senas VARCHAR(300) COMMENT 'Dirección descriptiva del cliente';

-- Índices
CREATE INDEX idx_cliente_tipo_identificacion ON cliente(tipo_identificacion);
CREATE INDEX idx_cliente_identificacion_tipo ON cliente(identificacion, tipo_identificacion);

-- Validar
SHOW CREATE TABLE cliente;
```

#### 5.2 Actualizar Entidad JPA
- [ ] Abrir: `modules/cliente/model/Cliente.java`
- [ ] Agregar campo `tipoIdentificacion` (enum)
- [ ] Agregar campo `codigoActividadEconomica` (opcional)
- [ ] Agregar campos de ubicación
- [ ] Actualizar validaciones

#### 5.3 Actualizar Formulario Cliente
- [ ] Abrir: `templates/clientes/form.html`
- [ ] Agregar select para `tipoIdentificacion`
- [ ] Agregar campo para `codigoActividadEconomica` (opcional)
- [ ] Agregar selects de ubicación
- [ ] Campo textarea para `otrasSenas`

---

