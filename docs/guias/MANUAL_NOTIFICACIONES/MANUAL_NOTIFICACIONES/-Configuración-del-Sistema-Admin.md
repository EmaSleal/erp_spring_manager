## 🔧 Configuración del Sistema (Admin)

> **⚠️ Sección solo para administradores**

### Acceso a Configuración

**URL:** `/configuracion?tab=notificaciones`

**Ruta de navegación:**
1. Menú lateral → **"Configuración"**
2. Tab **"Notificaciones"**

### Sección 1: Configuración de Email

```
┌────────────────────────────────────────────────┐
│  Configuración de Email                       │
├────────────────────────────────────────────────┤
│                                                │
│  [✓] Activar notificaciones por email        │
│                                                │
│  [✓] Enviar factura automáticamente           │
│      (Al crear una factura, enviar por email) │
│                                                │
│  Email del administrador:                      │
│  [admin@empresa.com                  ]        │
│  (Recibe notificaciones administrativas)      │
│                                                │
│  Email copia de facturas (BCC):               │
│  [contabilidad@empresa.com           ]        │
│  (Recibe copia oculta de todas las facturas)  │
│                                                │
│  [Probar Email]  [Guardar]                    │
│                                                │
└────────────────────────────────────────────────┘
```

**Campos:**

| Campo | Descripción | Obligatorio |
|-------|-------------|-------------|
| **Activar email** | Habilita el sistema de emails | No |
| **Envío automático** | Envía factura al crearla | No |
| **Email admin** | Recibe notificaciones del sistema | Opcional |
| **Email copia facturas** | BCC en todas las facturas | Opcional |

**Botón "Probar Email":**
- Envía un email de prueba al administrador
- Verifica que la configuración SMTP funcione
- Muestra mensaje de éxito/error

---

### Sección 2: Recordatorios de Pago

```
┌────────────────────────────────────────────────┐
│  Recordatorios de Pago                        │
├────────────────────────────────────────────────┤
│                                                │
│  [✓] Activar recordatorios de pago            │
│                                                │
│  Recordatorio preventivo:                      │
│  Enviar [3] días antes del vencimiento        │
│                                                │
│  Recordatorio de vencimiento:                  │
│  Enviar al vencer la factura                  │
│                                                │
│  Recordatorios posteriores:                    │
│  Enviar cada [7] días después del vencimiento │
│                                                │
│  Frecuencia máxima:                           │
│  Hasta [3] recordatorios por factura          │
│                                                │
│  [Ejecutar Ahora] [Guardar]                   │
│                                                │
└────────────────────────────────────────────────┘
```

**Configuraciones:**

| Parámetro | Descripción | Valor por defecto |
|-----------|-------------|-------------------|
| **Días preventivo** | Días antes de vencer | 3 |
| **Días posteriores** | Frecuencia tras vencer | 7 |
| **Máximo recordatorios** | Límite de recordatorios | 3 |

**Botón "Ejecutar Ahora":**
- Ejecuta el scheduler de recordatorios manualmente
- Útil para testing
- Normalmente se ejecuta a las 9:00 AM diariamente

---

### Sección 3: Notificaciones Administrativas

```
┌────────────────────────────────────────────────┐
│  Notificaciones Administrativas               │
├────────────────────────────────────────────────┤
│                                                │
│  [✓] Notificar al crear nuevo cliente        │
│      Enviar notificación al admin cuando se   │
│      registra un nuevo cliente                │
│                                                │
│  [✓] Notificar al crear nuevo usuario        │
│      Enviar notificación al super admin       │
│                                                │
│  [ ] Notificar stock bajo                     │
│      Alertar cuando productos alcancen el     │
│      stock mínimo                             │
│                                                │
│  [Guardar]                                     │
│                                                │
└────────────────────────────────────────────────┘
```

---

### Configuración SMTP (Avanzado)

**Ubicación:** Archivo `application.properties`

```properties
# Configuración SMTP para Gmail
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=tu-email@gmail.com
spring.mail.password=tu-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

**Proveedores comunes:**

| Proveedor | Host | Puerto |
|-----------|------|--------|
| **Gmail** | smtp.gmail.com | 587 |
| **Outlook** | smtp-mail.outlook.com | 587 |
| **Yahoo** | smtp.mail.yahoo.com | 465 |
| **Custom** | (consultar proveedor) | 25/587 |

⚠️ **Importante:** Para Gmail, usar "App Password", no la contraseña normal.

---

