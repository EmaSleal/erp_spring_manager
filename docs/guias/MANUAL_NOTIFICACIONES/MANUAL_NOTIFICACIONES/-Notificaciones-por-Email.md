## 📧 Notificaciones por Email

### Requisitos Previos

1. **Email del usuario configurado**
   - En su perfil de usuario
   - Debe ser válido y activo

2. **Sistema de email habilitado**
   - Configurado por el administrador
   - Servidor SMTP funcional

3. **Preferencias activadas**
   - El usuario debe tener habilitado el canal EMAIL

### Tipos de Emails

#### 1. Email de Factura Creada

**Asunto:** `Nueva Factura ${numeroFactura} - ${nombreEmpresa}`

**Contenido:**
- Saludo personalizado
- Detalles de la factura (número, cliente, montos)
- Tabla con productos/servicios
- Botón "Ver Factura"
- Adjunto: PDF de la factura

**Destinatarios:**
- Cliente (email principal)
- Copia oculta (BCC): Email de contabilidad (si configurado)

---

#### 2. Email de Recordatorio de Pago

**Asunto:** `Recordatorio: Factura ${numeroFactura} próxima a vencer`

**Contenido:**
- Recordatorio amigable
- Fecha de vencimiento
- Monto adeudado
- Instrucciones de pago
- Datos bancarios (si configurados)

**Destinatarios:**
- Cliente con deuda
- Copia: Administrador (opcional)

---

#### 3. Email de Confirmación de Pago

**Asunto:** `Pago Recibido - Factura ${numeroFactura}`

**Contenido:**
- Agradecimiento
- Confirmación del pago
- Monto pagado
- Fecha de pago
- Recibo/comprobante

**Destinatarios:**
- Cliente que pagó
- Copia: Usuario que registró el pago

---

### Configuración de Emails (Administrador)

**Ubicación:** Configuración > Notificaciones > Email

**Parámetros:**

| Campo | Descripción | Ejemplo |
|-------|-------------|---------|
| **Activar Email** | Habilitar envío de emails | ✅ Activado |
| **Envío Automático** | Enviar factura al crearla | ✅ Activado |
| **Email Admin** | Email para notificaciones admin | admin@empresa.com |
| **Email Copia Facturas** | BCC para todas las facturas | contabilidad@empresa.com |
| **Días Recordatorio** | Días antes de vencimiento | 3 |

### Solución de Problemas con Email

**Problema: No llegan emails**

✅ **Verificar:**
1. Email del usuario está correcto
2. Sistema de email está activado
3. Preferencias del usuario permiten email
4. Revisar carpeta de SPAM
5. Verificar configuración SMTP (admin)

---

