# === PAGOS ===
pagos.dias-vencimiento-default=30
pagos.permitir-sobrepagos=false
pagos.conciliacion-automatica=true
```

## Inicialización de Datos

1. **Ejecutar migration de Contabilidad:**

```sql
SOURCE docs/base\ de\ datos/MIGRATION_CONTABILIDAD_SPRINT_5.sql;
```

2. **Cargar Plan de Cuentas:**

```sql
SOURCE docs/base\ de\ datos/PLAN_CUENTAS_COSTA_RICA.sql;
```

3. **Configurar consecutivos:**

```sql
INSERT INTO configuracion_consecutivos (tipo, prefijo, siguiente, longitud)
VALUES 
('ASIENTO', 'ASI', 1, 5),
('PAGO', 'PAG', 1, 5),
('COMPROBANTE_FE', '001-001', 1, 10);
```
```

---

### 2.2. Guía de Configuración de Certificado Digital

**Archivo:** `docs/guias/GUIA_CERTIFICADO_DIGITAL.md`

#### Tareas:

- [ ] **2.2.1** Tutorial paso a paso para certificado

---

### 2.3. Guía de Migración de Datos

**Archivo:** `docs/guias/GUIA_MIGRACION_DATOS_SPRINT5.md`

#### Tareas:

- [ ] **2.3.1** Guía para migrar datos existentes

**Contenido:**
- Cómo migrar facturas antiguas al nuevo sistema
- Cómo importar saldos iniciales de clientes
- Cómo cargar asientos de apertura contable
- Scripts SQL incluidos

---

