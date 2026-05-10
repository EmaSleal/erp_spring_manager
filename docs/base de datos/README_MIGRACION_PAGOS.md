# 📦 Migración del Módulo de Pagos

## 📋 Descripción

Este directorio contiene las migraciones SQL necesarias para implementar el **Módulo de Pagos** en el sistema ERP Orders Manager (Sprint 5 - Fase 1).

---

## 📁 Archivos de Migración

### 1. **MIGRATION_PAGOS.sql**
- **Descripción**: Crea la tabla `pagos` con todos sus campos, índices y constraints
- **Componentes**:
  - Tabla principal con 25+ campos
  - 8 índices para optimización
  - Foreign keys a `clientes` y `factura`
  - Validaciones de datos (CHECK constraints)
  - Auditoría completa (creación, modificación, anulación)

### 2. **MIGRATION_PAGOS_PERMISOS.sql**
- **Descripción**: Crea los 8 permisos del módulo de pagos y los asigna a roles
- **Permisos creados**:
  - `PAGO_VER` - Ver listado y detalle
  - `PAGO_CREAR` - Registrar pagos
  - `PAGO_EDITAR` - Modificar pagos pendientes
  - `PAGO_ELIMINAR` - Eliminar pagos en borrador (CRÍTICO)
  - `PAGO_CONFIRMAR` - Confirmar y generar asiento (CRÍTICO)
  - `PAGO_ANULAR` - Anular pagos confirmados (CRÍTICO)
  - `PAGO_CONCILIAR` - Conciliación bancaria
  - `PAGO_ESTADO_CUENTA` - Ver estado de cuenta

### 3. **EJECUTAR_MIGRACION_PAGOS.sql**
- **Descripción**: Script consolidado que ejecuta todas las migraciones en orden
- **Función**: Automatiza la ejecución completa y muestra verificaciones

---

## 🚀 Instrucciones de Ejecución

### Opción 1: Script Consolidado (RECOMENDADO)

```sql
-- Conectar a la base de datos
mysql -u root -p whats_orders_manager

-- Ejecutar script consolidado
SOURCE d:/programacion/java/spring-boot/whats_orders_manager/docs/base de datos/EJECUTAR_MIGRACION_PAGOS.sql;
```

### Opción 2: Ejecución Manual

```sql
-- Paso 1: Crear tabla de pagos
SOURCE d:/programacion/java/spring-boot/whats_orders_manager/docs/base de datos/MIGRATION_PAGOS.sql;

-- Paso 2: Crear permisos
SOURCE d:/programacion/java/spring-boot/whats_orders_manager/docs/base de datos/MIGRATION_PAGOS_PERMISOS.sql;
```

### Opción 3: Desde PowerShell

```powershell
# Navegar al directorio del proyecto
cd D:\programacion\java\spring-boot\whats_orders_manager

# Ejecutar migración
mysql -u root -p whats_orders_manager < "docs\base de datos\EJECUTAR_MIGRACION_PAGOS.sql"
```

---

## ✅ Verificación Post-Migración

### 1. Verificar Tabla

```sql
-- Ver estructura
DESCRIBE pagos;

-- Ver índices
SHOW INDEXES FROM pagos;

-- Contar registros
SELECT COUNT(*) FROM pagos;
```

### 2. Verificar Permisos

```sql
-- Listar permisos de pagos
SELECT codigo, nombre, categoria, es_critico
FROM permiso
WHERE categoria = 'Pagos';

-- Ver distribución por rol
SELECT 
    r.nombre AS rol,
    GROUP_CONCAT(p.codigo SEPARATOR ', ') AS permisos_pagos
FROM rol r
LEFT JOIN rol_permiso rp ON r.id_rol = rp.id_rol
LEFT JOIN permiso p ON rp.id_permiso = p.id_permiso
WHERE p.categoria = 'Pagos'
GROUP BY r.nombre;
```

### 3. Prueba de Inserción

```sql
-- Insertar pago de prueba
INSERT INTO pagos (
    numero_pago, cliente_id, factura_id, monto, fecha_pago,
    metodo_pago, tipo_pago, estado, creado_por
) VALUES (
    'PAG-20260118-0001', 1, 1, 50000.00, '2026-01-18',
    'EFECTIVO', 'TOTAL', 'PENDIENTE', 'ADMIN'
);

-- Verificar inserción
SELECT * FROM pagos WHERE numero_pago = 'PAG-20260118-0001';
```

---

## 📊 Estructura de la Tabla

### Campos Principales

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id_pago` | BIGINT | ID autoincremental (PK) |
| `numero_pago` | VARCHAR(20) | Consecutivo único: PAG-YYYYMMDD-0001 |
| `cliente_id` | BIGINT | Cliente que paga (FK) |
| `factura_id` | INT | Factura aplicada (FK, NULL para adelantos) |
| `monto` | DECIMAL(12,2) | Monto del pago |
| `fecha_pago` | DATE | Fecha de recepción |
| `metodo_pago` | VARCHAR(30) | Método según Hacienda CR |
| `tipo_pago` | VARCHAR(20) | TOTAL/PARCIAL/ADELANTO/NOTA_CREDITO |
| `estado` | VARCHAR(20) | PENDIENTE/CONFIRMADO/ANULADO/etc. |

### Campos de Auditoría

- `creado_por`, `creado_en`
- `modificado_por`, `modificado_en`
- `anulado_por`, `anulado_en`, `motivo_anulacion`

### Índices

1. `idx_pago_numero` - UNIQUE en numero_pago
2. `idx_pago_cliente` - Cliente
3. `idx_pago_factura` - Factura
4. `idx_pago_fecha` - Fecha
5. `idx_pago_estado` - Estado
6. `idx_pago_metodo` - Método de pago
7. `idx_pago_cliente_estado` - Compuesto
8. `idx_pago_factura_estado` - Compuesto

---

## 🔐 Matriz de Permisos

| Permiso | ADMIN | GERENTE | VENDEDOR | VISUALIZADOR | CONTADOR |
|---------|:-----:|:-------:|:--------:|:------------:|:--------:|
| PAGO_VER | ✅ | ✅ | ✅ | ✅ | ✅ |
| PAGO_CREAR | ✅ | ✅ | ✅ | ❌ | ❌ |
| PAGO_EDITAR | ✅ | ✅ | ❌ | ❌ | ❌ |
| PAGO_ELIMINAR | ✅ | ❌ | ❌ | ❌ | ❌ |
| PAGO_CONFIRMAR | ✅ | ✅ | ❌ | ❌ | ❌ |
| PAGO_ANULAR | ✅ | ❌ | ❌ | ❌ | ❌ |
| PAGO_CONCILIAR | ✅ | ✅ | ❌ | ❌ | ✅ |
| PAGO_ESTADO_CUENTA | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 📝 Notas Importantes

### Métodos de Pago (Hacienda CR - Anexo 4.4)

- `01` - EFECTIVO
- `02` - TARJETA
- `03` - CHEQUE
- `04` - TRANSFERENCIA_DEPOSITO
- `05` - RECAUDADO_TERCEROS
- `99` - OTROS

### Estados del Pago

- **PENDIENTE**: Pago registrado, no confirmado
- **CONFIRMADO**: Pago confirmado, genera asiento contable
- **RECHAZADO**: Pago rechazado por validación
- **ANULADO**: Pago cancelado, revierte asiento
- **CONCILIADO**: Pago validado contra extracto bancario

### Tipos de Pago

- **TOTAL**: Pago completo de factura
- **PARCIAL**: Pago parcial (abono)
- **ADELANTO**: Pago sin factura asignada
- **NOTA_CREDITO**: Aplicación de nota de crédito

### Integración Contable

Cuando un pago es **confirmado**, se genera automáticamente un asiento contable:

- **DEBE**: Cuenta de caja/banco (según método)
  - EFECTIVO → 1.1.01 (Caja)
  - TARJETA/TRANSFERENCIA → 1.1.02 (Bancos)
  - CHEQUE → 1.1.04 (Documentos por Cobrar)
  
- **HABER**: 1.1.03 (Cuentas por Cobrar)

---

## 🔄 Rollback (en caso de error)

```sql
-- Eliminar permisos asignados
DELETE FROM rol_permiso 
WHERE id_permiso IN (
    SELECT id_permiso FROM permiso WHERE categoria = 'Pagos'
);

-- Eliminar permisos
DELETE FROM permiso WHERE categoria = 'Pagos';

-- Eliminar tabla
DROP TABLE IF EXISTS pagos;
```

---

## 📚 Referencias

- **Documentación**: `docs/sprints/SPRINT_5/FASE_1_PAGOS.md`
- **Plan Maestro**: `docs/planificacion/PLAN_MAESTRO.txt`
- **Backend**: `src/main/java/api/astro/whats_orders_manager/modules/pagos/`
- **Anexo Hacienda CR**: Resolución DGT-R-48-2016 (Anexo 4.4 - Medios de Pago)

---

## ✨ Próximos Pasos

Después de ejecutar la migración:

1. ✅ Reiniciar la aplicación Spring Boot
2. ✅ Verificar que el enum `Permiso.java` compile correctamente
3. ✅ Probar endpoints REST del módulo de pagos
4. 📋 Crear vistas Thymeleaf (frontend)
5. 📋 Implementar JavaScript para interacciones
6. 📋 Testing completo del módulo

---

**Fecha de creación**: 18 de enero de 2026  
**Sprint**: 5 - Fase 1  
**Autor**: Sistema ERP  
**Estado**: ✅ Listo para ejecución
