## 📋 Gestión de Notificaciones

### Ver Historial Completo

**Acceso:** Menú → Notificaciones → Historial

**Vista de tabla:**

| Fecha | Tipo | Canal | Mensaje | Estado | Acciones |
|-------|------|-------|---------|--------|----------|
| 04/01 10:30 | Factura | WEB | Nueva factura... | ✅ Leída | [Ver] |
| 04/01 09:15 | Pago | EMAIL | Pago recibido... | ✅ Enviado | [Ver] |
| 03/01 14:22 | Stock | WEB | Stock bajo... | 🔴 No leída | [Ver] |

**Filtros disponibles:**
- Por tipo de notificación
- Por canal
- Por estado (leída/no leída)
- Por rango de fechas

### Marcar como Leída/No Leída

**Opción 1: Individual**
1. Hacer clic en la notificación
2. Se marca automáticamente como leída

**Opción 2: Masiva**
1. Seleccionar múltiples notificaciones (checkbox)
2. Clic en "Marcar como leídas"
3. Confirmación

**Opción 3: Todas**
- Botón "Marcar todas como leídas"
- Marca todo el historial

### Eliminar Notificaciones

**⚠️ Precaución:** Eliminación es permanente

**Individual:**
1. Clic en icono de papelera 🗑️
2. Confirmar eliminación

**Masiva:**
1. Seleccionar varias notificaciones
2. Clic en "Eliminar seleccionadas"
3. Confirmar

**Nota:** Solo se pueden eliminar notificaciones propias, no las de otros usuarios.

### Exportar Historial

**Formato:** CSV

**Pasos:**
1. Aplicar filtros deseados (opcional)
2. Clic en "Exportar"
3. Seleccionar formato (CSV)
4. Se descarga archivo

**Contenido del CSV:**
```csv
Fecha,Tipo,Canal,Mensaje,Estado,Leida
04/01/2026 10:30,FACTURA_CREADA,WEB,Nueva factura F001-00125,ENVIADA,SI
04/01/2026 09:15,PAGO_RECIBIDO,EMAIL,Pago recibido,ENVIADA,SI
...
```

**Uso:**
- Auditoría
- Reportes
- Análisis de comunicaciones

---

