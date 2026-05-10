## 💬 Notificaciones por WhatsApp

### Requisitos Previos

1. **Integración WhatsApp Business API**
   - Configurado por el administrador
   - Plantillas aprobadas por Meta

2. **Teléfono del destinatario**
   - Cliente debe tener teléfono registrado
   - Formato internacional: +51987654321

3. **Plantillas aprobadas**
   - Solo se pueden usar plantillas pre-aprobadas
   - No se puede enviar texto libre (restricción de Meta)

### Plantillas Disponibles

#### 1. Recordatorio de Pago

**Nombre:** `recordatorio_pago`

**Parámetros:**
- `{cliente}` - Nombre del cliente
- `{numeroFactura}` - Número de factura
- `{fechaVencimiento}` - Fecha de vencimiento
- `{monto}` - Monto total
- `{enlace}` - Link a la factura

**Mensaje:**
```
Hola {{1}},

Le recordamos que la factura {{2}} vence el {{3}}.

Monto: S/ {{4}}

Ver detalles: {{5}}

Gracias por su preferencia.
```

---

#### 2. Factura Nueva

**Nombre:** `factura_nueva`

**Parámetros:**
- `{cliente}` - Nombre del cliente
- `{numeroFactura}` - Número de factura
- `{total}` - Monto total
- `{enlace}` - Link para ver

**Mensaje:**
```
Estimado/a {{1}},

Se ha generado la factura {{2}} por S/ {{3}}.

Puede verla aquí: {{4}}

Gracias.
```

---

#### 3. Confirmación de Pago

**Nombre:** `confirmacion_pago`

**Parámetros:**
- `{cliente}` - Nombre del cliente
- `{numeroFactura}` - Número de factura
- `{monto}` - Monto pagado
- `{fecha}` - Fecha del pago

**Mensaje:**
```
¡Gracias {{1}}!

Hemos recibido su pago de S/ {{3}} para 
la factura {{2}}.

Fecha: {{4}}

Su factura está PAGADA ✓
```

---

### Envío Manual de WhatsApp

**Desde la vista de facturas:**

1. Ir a **Facturas** > Listado
2. Localizar la factura deseada
3. Clic en botón **"WhatsApp"** 💬
4. Seleccionar plantilla a usar
5. Confirmar envío

**Estados de envío:**
- ⏳ **Enviando:** Mensaje en proceso
- ✅ **Enviado:** Mensaje entregado a WhatsApp
- 📱 **Recibido:** Cliente recibió el mensaje
- ❌ **Error:** Fallo en el envío

### Envío Automático de WhatsApp

**Configuración:**

El administrador puede configurar envío automático en:
- Configuración > Notificaciones > WhatsApp

**Eventos automáticos:**
1. **Factura creada:** Enviar automáticamente al crear
2. **Recordatorio preventivo:** X días antes del vencimiento
3. **Factura vencida:** Cuando pasa la fecha de vencimiento

**Ejemplo de configuración:**
```
✅ Enviar factura automática por WhatsApp
✅ Recordatorio 3 días antes
✅ Recordatorio al vencer
Plantilla: recordatorio_pago
```

---

