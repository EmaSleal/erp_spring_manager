# 🔔 Manual de Usuario - Sistema de Notificaciones

**Versión:** 1.0  
**Fecha:** 4 de enero de 2026  
**Audiencia:** Todos los usuarios del sistema  
**Nivel de acceso requerido:** ROL_ADMIN, ROL_USER

---

## 📑 Tabla de Contenidos

1. [Introducción](#introducción)
2. [Tipos de Notificaciones](#tipos-de-notificaciones)
3. [Canales de Notificación](#canales-de-notificación)
4. [Notificaciones Web (En la Aplicación)](#notificaciones-web-en-la-aplicación)
5. [Notificaciones por Email](#notificaciones-por-email)
6. [Notificaciones por WhatsApp](#notificaciones-por-whatsapp)
7. [Preferencias de Notificaciones](#preferencias-de-notificaciones)
8. [Configuración del Sistema (Admin)](#configuración-del-sistema-admin)
9. [Gestión de Notificaciones](#gestión-de-notificaciones)
10. [Solución de Problemas](#solución-de-problemas)
11. [Preguntas Frecuentes](#preguntas-frecuentes)

---

## 📖 Introducción

El **Sistema de Notificaciones** mantiene a los usuarios informados sobre eventos importantes del negocio en tiempo real. Las notificaciones se pueden recibir por múltiples canales según las preferencias del usuario.

### Características Principales

- ✅ **Notificaciones en tiempo real** - WebSocket para actualizaciones instantáneas
- ✅ **Múltiples canales** - Web, Email y WhatsApp
- ✅ **Personalización total** - Configure qué notificaciones recibir
- ✅ **Historial completo** - Vea todas sus notificaciones pasadas
- ✅ **Marcado de leídas** - Controle qué notificaciones ha revisado
- ✅ **Badge de contador** - Vea cuántas notificaciones sin leer tiene
- ✅ **Filtros avanzados** - Busque por tipo, canal, fecha

### ⚙️ Componentes del Sistema

```
┌─────────────────────────────────────────────────┐
│     SISTEMA DE NOTIFICACIONES                  │
├─────────────────────────────────────────────────┤
│                                                 │
│  📱 WEB (WebSocket)                            │
│    └─ Badge + Dropdown en navbar              │
│                                                 │
│  📧 EMAIL (SMTP)                               │
│    └─ Plantillas HTML personalizadas          │
│                                                 │
│  💬 WHATSAPP (API)                             │
│    └─ Plantillas aprobadas por Meta           │
│                                                 │
│  ⚙️ PREFERENCIAS                                │
│    └─ Configuración individual por usuario     │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

## 📬 Tipos de Notificaciones

El sistema soporta **9 tipos** diferentes de notificaciones:

### 1. Factura Creada 📄

**¿Cuándo se envía?**
- Al crear una nueva factura en el sistema

**Destinatarios:**
- Usuario que creó la factura
- Cliente (si tiene email configurado)

**Canales disponibles:**
- ✅ WEB
- ✅ EMAIL

**Contenido:**
- Número de factura
- Cliente
- Total
- Enlace para ver detalles

**Ejemplo:**
```
Título: Nueva Factura Creada
Mensaje: Se ha creado la factura F001-00125 para el cliente 
         "ABC Company" por un total de S/ 1,250.00
```

---

### 2. Factura Vencida ⚠️

**¿Cuándo se envía?**
- Cuando una factura sobrepasa su fecha de vencimiento sin pagar
- Se envía diariamente a las 9:00 AM (si está configurado)

**Destinatarios:**
- Administradores
- Cliente con deuda

**Canales disponibles:**
- ✅ WEB
- ✅ EMAIL
- ✅ WHATSAPP

**Contenido:**
- Número de factura
- Días de atraso
- Monto adeudado
- Enlace para pagar

**Ejemplo:**
```
Título: Factura Vencida - Atención Requerida
Mensaje: La factura F001-00120 está vencida hace 5 días.
         Monto pendiente: S/ 850.00
         Cliente: XYZ Corp
```

---

### 3. Factura Próxima a Vencer 📅

**¿Cuándo se envía?**
- X días antes del vencimiento (configurable)
- Por defecto: 3 días antes

**Destinatarios:**
- Cliente
- Administradores (copia)

**Canales disponibles:**
- ✅ WEB
- ✅ EMAIL
- ✅ WHATSAPP

**Contenido:**
- Número de factura
- Fecha de vencimiento
- Monto total
- Recordatorio amigable

**Ejemplo:**
```
Título: Recordatorio: Factura próxima a vencer
Mensaje: La factura F001-00122 vence en 3 días (07/01/2026).
         Monto: S/ 1,500.00
         Cliente: Empresa Demo SAC
```

---

### 4. Pago Recibido ✅

**¿Cuándo se envía?**
- Al registrar un pago en una factura

**Destinatarios:**
- Usuario que registró el pago
- Cliente (confirmación)
- Administradores (notificación)

**Canales disponibles:**
- ✅ WEB
- ✅ EMAIL

**Contenido:**
- Número de factura
- Monto pagado
- Fecha del pago
- Saldo restante (si es pago parcial)

**Ejemplo:**
```
Título: Pago Recibido
Mensaje: Se ha registrado un pago de S/ 1,500.00 para la 
         factura F001-00122. 
         Estado: PAGADA ✓
```

---

### 5. Stock Bajo 📦

**¿Cuándo se envía?**
- Cuando un producto alcanza el stock mínimo configurado
- Al realizar una venta que deja el producto en stock bajo

**Destinatarios:**
- Administradores
- Usuarios con permiso de inventario

**Canales disponibles:**
- ✅ WEB
- ✅ EMAIL

**Contenido:**
- Código y nombre del producto
- Stock actual
- Stock mínimo configurado
- Alerta para reabastecer

**Ejemplo:**
```
Título: Alerta de Stock Bajo
Mensaje: El producto "Laptop Dell XPS 15" tiene stock bajo.
         Stock actual: 2 unidades
         Stock mínimo: 5 unidades
         ⚠️ Se requiere reabastecimiento
```

---

### 6. Nuevo Cliente 👥

**¿Cuándo se envía?**
- Al registrar un nuevo cliente en el sistema

**Destinatarios:**
- Administradores (si está configurado)

**Canales disponibles:**
- ✅ WEB
- ✅ EMAIL

**Contenido:**
- Nombre del cliente
- RUC/DNI
- Email de contacto
- Usuario que lo creó

**Ejemplo:**
```
Título: Nuevo Cliente Registrado
Mensaje: Se ha registrado el cliente "Tech Solutions SAC"
         RUC: 20123456789
         Email: contacto@techsolutions.com
         Creado por: admin@empresa.com
```

---

### 7. Nuevo Usuario 🔐

**¿Cuándo se envía?**
- Al crear una nueva cuenta de usuario

**Destinatarios:**
- Super Administrador (si está configurado)

**Canales disponibles:**
- ✅ WEB
- ✅ EMAIL

**Contenido:**
- Nombre del nuevo usuario
- Email/username
- Rol asignado
- Administrador que lo creó

**Ejemplo:**
```
Título: Nuevo Usuario Creado
Mensaje: Se ha creado el usuario "Juan Pérez"
         Email: juan.perez@empresa.com
         Rol: VENDEDOR
         Creado por: admin@empresa.com
```

---

### 8. Mensaje WhatsApp 💬

**¿Cuándo se envía?**
- Al recibir un mensaje de WhatsApp de un cliente
- Solo si hay integración con WhatsApp Business API

**Destinatarios:**
- Usuarios con acceso al módulo WhatsApp

**Canales disponibles:**
- ✅ WEB

**Contenido:**
- Remitente (nombre/teléfono)
- Preview del mensaje
- Enlace para responder

**Ejemplo:**
```
Título: Nuevo Mensaje de WhatsApp
Mensaje: Cliente "ABC Company" (+51987654321) envió:
         "Hola, quisiera consultar por la factura..."
         [Ver conversación]
```

---

### 9. Sistema 🔧

**¿Cuándo se envía?**
- Mantenimiento programado
- Actualizaciones del sistema
- Anuncios importantes

**Destinatarios:**
- Todos los usuarios

**Canales disponibles:**
- ✅ WEB
- ✅ EMAIL

**Contenido:**
- Mensaje del sistema
- Acciones requeridas (si aplica)

**Ejemplo:**
```
Título: Mantenimiento Programado
Mensaje: El sistema estará en mantenimiento el sábado 
         10/01/2026 de 10:00 PM a 2:00 AM.
         Disculpe las molestias.
```

---

## 📡 Canales de Notificación

### Canal Web 📱

**Características:**
- **Tiempo real:** Notificaciones instantáneas via WebSocket
- **Badge:** Contador de notificaciones sin leer en el navbar
- **Dropdown:** Lista desplegable con últimas notificaciones
- **Sin configuración:** Funciona automáticamente al iniciar sesión

**Ubicación:**
```
┌─────────────────────────────────────────────────┐
│  [Logo]  Inicio  Facturas  Clientes  [🔔 (5)]  │
│                                        ↓        │
│                              ┌─────────────────┐│
│                              │ Notificaciones  ││
│                              ├─────────────────┤│
│                              │ 🔴 Nueva fact...││
│                              │ 📧 Pago reci... ││
│                              │ ⚠️ Stock bajo...││
│                              │                 ││
│                              │ Ver todas →     ││
│                              └─────────────────┘│
└─────────────────────────────────────────────────┘
```

**Estados visuales:**
- 🔴 **Punto rojo:** Notificación nueva (no leída)
- 🟢 **Sin punto:** Notificación leída
- 🔢 **Badge (5):** Número total sin leer

---

### Canal Email 📧

**Características:**
- **Plantillas HTML:** Emails profesionales con formato
- **Adjuntos:** Puede incluir PDF (ej: facturas)
- **Personalización:** Contenido dinámico según el evento
- **Requiere configuración:** Email del usuario debe estar registrado

**Ejemplo visual de email:**
```
┌──────────────────────────────────────────┐
│  [Logo Empresa]                         │
│                                          │
│  Hola, Juan Pérez                        │
│                                          │
│  ¡Nueva Factura Creada!                  │
│                                          │
│  Se ha generado la factura F001-00125    │
│  para el cliente ABC Company.            │
│                                          │
│  Detalles:                               │
│  • Cliente: ABC Company                  │
│  • Subtotal: S/ 1,000.00                │
│  • IGV (18%): S/ 180.00                 │
│  • Total: S/ 1,180.00                   │
│                                          │
│  [Ver Factura]  [Descargar PDF]         │
│                                          │
│  ────────────────────────────────────    │
│  Este es un correo automático.           │
│  WhatsApp Orders Manager © 2026          │
└──────────────────────────────────────────┘
```

**Configuración de Email:**
- En **Configuración > Notificaciones**
- Requiere servidor SMTP configurado
- Email del usuario en su perfil

---

### Canal WhatsApp 💬

**Características:**
- **API de Meta:** Integración con WhatsApp Business API
- **Plantillas aprobadas:** Solo plantillas pre-aprobadas por Meta
- **Requiere teléfono:** Cliente/usuario debe tener número
- **Confirmación:** Se registra el estado de entrega

**Plantillas disponibles:**
1. **recordatorio_pago:** Recordatorio de factura vencida
2. **factura_nueva:** Notificación de nueva factura
3. **confirmacion_pago:** Confirmación de pago recibido

**Ejemplo de mensaje:**
```
─────────────────────────────
  WhatsApp Notification
─────────────────────────────

🏢 *Tu Empresa*

Hola Juan,

Te recordamos que la factura 
F001-00125 vence el 10/01/2026.

💰 Monto: S/ 1,180.00

Puedes ver los detalles en:
https://app.empresa.com/facturas/125

Gracias por tu preferencia.
─────────────────────────────
```

---

## 🔔 Notificaciones Web (En la Aplicación)

### Acceso a Notificaciones Web

1. **Badge en el navbar** (esquina superior derecha)
   - Icono de campana: 🔔
   - Número rojo: Cantidad de notificaciones sin leer

2. **Hacer clic en el icono** para ver el dropdown

### Dropdown de Notificaciones

**Componentes:**

| Elemento | Descripción |
|----------|-------------|
| **Encabezado** | "Notificaciones" + enlace "Ver todas" |
| **Lista** | Últimas 5 notificaciones |
| **Iconos** | Indica el tipo de notificación |
| **Timestamp** | Hace cuánto tiempo se recibió |
| **Estado** | Leída (sin punto) / No leída (punto rojo) |

**Acciones disponibles:**

- **Ver todas:** Navega a la página completa de notificaciones
- **Marcar como leída:** Clic en una notificación
- **Ir al detalle:** Clic en el enlace de la notificación

### Página Completa de Notificaciones

**Acceso:**
1. Clic en "Ver todas" en el dropdown
2. URL: `/notificaciones`

**Funcionalidades:**

#### 1. Filtros

| Filtro | Opciones |
|--------|----------|
| **Tipo** | Todas, Factura, Pago, Cliente, Sistema... |
| **Estado** | Todas, Leídas, No leídas |
| **Fecha** | Hoy, Esta semana, Este mes, Personalizado |

#### 2. Acciones Masivas

- ✅ **Marcar todas como leídas**
- 🗑️ **Eliminar seleccionadas**
- 📥 **Exportar historial**

#### 3. Vista de Lista

Cada notificación muestra:
```
┌────────────────────────────────────────────────┐
│ 🔴 [Icono] Título de la Notificación          │
│    Mensaje descriptivo de la notificación...   │
│    hace 2 horas · WEB                          │
│    [Ver detalles] [Marcar leída] [Eliminar]   │
└────────────────────────────────────────────────┘
```

### Notificaciones en Tiempo Real

**Tecnología:** WebSocket

**¿Cómo funciona?**
1. Al iniciar sesión, se establece conexión WebSocket
2. El servidor envía notificaciones instantáneas
3. El badge se actualiza automáticamente
4. Se muestra un toast/popup breve (opcional)

**Estados de conexión:**
- 🟢 **Conectado:** Recibiendo notificaciones en tiempo real
- 🟡 **Reconectando:** Intento de reconexión
- 🔴 **Desconectado:** Sin conexión (requiere refresh)

---

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

## ⚙️ Preferencias de Notificaciones

### Acceso a Preferencias

**Opción 1: Desde el perfil**
1. Clic en su nombre (navbar superior derecho)
2. Seleccionar **"Preferencias"**
3. Tab **"Notificaciones"**

**Opción 2: URL directa**
```
/perfil/preferencias/notificaciones
```

### Configuración Global

**Desactivar todas las notificaciones:**

```
┌────────────────────────────────────────────────┐
│  Preferencias de Notificaciones               │
├────────────────────────────────────────────────┤
│                                                │
│  [ ] Desactivar TODAS las notificaciones      │
│      (No recibiré ninguna notificación)       │
│                                                │
└────────────────────────────────────────────────┘
```

⚠️ **Nota:** Si activa esta opción, NO recibirá ninguna notificación por ningún canal, incluso si tiene preferencias individuales activadas.

### Configuración por Tipo

**Control individual de cada tipo:**

```
┌────────────────────────────────────────────────┐
│  Tipos de Notificaciones                      │
├────────────────────────────────────────────────┤
│                                                │
│  📄 Factura Creada                            │
│     [✓] Web   [✓] Email   [ ] WhatsApp       │
│                                                │
│  💰 Pago Recibido                             │
│     [✓] Web   [✓] Email   [ ] WhatsApp       │
│                                                │
│  ⚠️ Factura Vencida                           │
│     [✓] Web   [✓] Email   [✓] WhatsApp       │
│                                                │
│  📦 Stock Bajo                                │
│     [✓] Web   [ ] Email   [ ] WhatsApp       │
│                                                │
│  👥 Nuevo Cliente                             │
│     [✓] Web   [ ] Email   [ ] WhatsApp       │
│                                                │
└────────────────────────────────────────────────┘
```

**Explicación:**
- ✅ **Marcado:** Recibirá notificaciones por ese canal
- ☐ **Sin marcar:** No recibirá notificaciones por ese canal

### Configuración por Canal

**Activar/desactivar cada canal:**

```
┌────────────────────────────────────────────────┐
│  Canales de Notificación                      │
├────────────────────────────────────────────────┤
│                                                │
│  [✓] 📱 Notificaciones Web                    │
│      Recibir notificaciones en la aplicación  │
│                                                │
│  [✓] 📧 Notificaciones por Email              │
│      Enviar a: usuario@empresa.com            │
│      [Cambiar email]                           │
│                                                │
│  [ ] 💬 Notificaciones por WhatsApp           │
│      Enviar a: +51987654321                   │
│      [Configurar teléfono]                     │
│                                                │
└────────────────────────────────────────────────┘
```

### Configuración Avanzada

#### Frecuencia de Notificaciones

```
┌────────────────────────────────────────────────┐
│  Frecuencia de Envío                          │
├────────────────────────────────────────────────┤
│                                                │
│  Facturas:                                     │
│  (•) Inmediata                                │
│  ( ) Resumen diario a las [09:00]            │
│  ( ) Resumen semanal (Lunes)                  │
│                                                │
│  Pagos:                                        │
│  (•) Inmediata                                │
│  ( ) Resumen diario                           │
│                                                │
└────────────────────────────────────────────────┘
```

**Opciones:**
- **Inmediata:** Notificación cada vez que ocurra el evento
- **Resumen diario:** Una notificación al día con todos los eventos
- **Resumen semanal:** Una notificación a la semana

#### Horario de Notificaciones

```
┌────────────────────────────────────────────────┐
│  Horario de Recepción                         │
├────────────────────────────────────────────────┤
│                                                │
│  [✓] Solo en horario laboral                  │
│      Lunes a Viernes, 8:00 AM - 6:00 PM      │
│                                                │
│  [ ] Recibir notificaciones a cualquier hora  │
│                                                │
│  Hora preferida para resúmenes: [09:00]      │
│                                                │
└────────────────────────────────────────────────┘
```

### Guardar Preferencias

**Pasos:**
1. Ajuste sus preferencias según necesidad
2. Haga clic en **"Guardar Preferencias"**
3. Verá mensaje de confirmación
4. Las preferencias se aplican inmediatamente

**Ejemplo de confirmación:**
```
✅ Preferencias guardadas correctamente
   Sus notificaciones se enviarán según la 
   configuración establecida.
```

---

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

## 🔧 Solución de Problemas

### Problema: No recibo notificaciones web

**Causas posibles:**
- Sesión desconectada
- WebSocket bloqueado
- Preferencias desactivadas

**Solución:**
1. Verificar que está logueado
2. Refresh de la página (F5)
3. Revisar preferencias (debe estar ✅ WEB activado)
4. Limpiar caché del navegador
5. Probar en modo incógnito

---

### Problema: No llegan emails

**Síntomas:**
- Notificaciones web funcionan
- Emails no llegan a la bandeja

**Verificar:**

1. **Email del usuario:**
   - Perfil → Email debe estar correcto
   - Confirmar que no hay typos

2. **Configuración del sistema:**
   - Configuración → Notificaciones
   - "Activar email" debe estar ✅

3. **Preferencias:**
   - Preferencias → Canal EMAIL debe estar ✅

4. **Carpeta SPAM:**
   - Revisar bandeja de correo no deseado
   - Marcar como "No es spam"

5. **Configuración SMTP (admin):**
   - Verificar credenciales SMTP
   - Probar con "Probar Email"

---

### Problema: Badge no se actualiza

**Síntomas:**
- Contador no disminuye al leer notificaciones
- Número incorrecto

**Solución:**
1. Marcar notificación como leída explícitamente
2. Refresh de página (F5)
3. Cerrar sesión y volver a entrar
4. Contactar al administrador si persiste

---

### Problema: WhatsApp no envía

**Causas:**
- Cliente sin teléfono
- Plantilla no aprobada
- API de WhatsApp desconectada

**Verificar:**
1. Cliente tiene teléfono registrado
2. Formato: +51987654321 (con código de país)
3. Plantilla está aprobada por Meta
4. Integración WhatsApp está activa (admin)

**Mensajes de error comunes:**

| Error | Significado | Solución |
|-------|-------------|----------|
| `Teléfono no válido` | Formato incorrecto | Usar +5198765432 1 |
| `Plantilla no encontrada` | Plantilla no existe | Contactar admin |
| `API no disponible` | Servicio caído | Esperar y reintentar |

---

### Problema: Demasiadas notificaciones

**Síntoma:**
- Recibo muchas notificaciones
- Interrumpen el trabajo

**Solución:**

1. **Ajustar frecuencia:**
   - Preferencias → Frecuencia
   - Cambiar a "Resumen diario"

2. **Desactivar tipos específicos:**
   - Preferencias → Tipos
   - Desmarcar tipos no importantes

3. **Desactivar canales:**
   - Mantener solo WEB
   - Desactivar EMAIL si es excesivo

4. **Horario laboral:**
   - Activar "Solo horario laboral"
   - No recibir fuera de horas

---

## ❓ Preguntas Frecuentes

### ¿Puedo desactivar todas las notificaciones?

**Sí.** En Preferencias → Marcar "Desactivar TODAS las notificaciones"

⚠️ **Advertencia:** No recibirá ninguna notificación, ni siquiera las críticas (facturas vencidas, pagos, etc.)

---

### ¿Las notificaciones web funcionan sin internet?

**No.** Las notificaciones web requieren:
- Conexión a internet activa
- Sesión activa en el sistema
- WebSocket conectado

Si pierde conexión, las notificaciones se acumularán y aparecerán cuando se reconecte.

---

### ¿Puedo recibir notificaciones en mi celular?

**Sí, por email o WhatsApp:**
- **Email:** Configure su email personal en el perfil
- **WhatsApp:** Configure su número de celular

**No hay app móvil nativa** actualmente, pero puede usar la web responsive.

---

### ¿Se pueden recuperar notificaciones eliminadas?

**No.** La eliminación es **permanente**. Las notificaciones eliminadas no se pueden recuperar.

**Recomendación:** Usar "Marcar como leída" en lugar de eliminar, para mantener historial.

---

### ¿Cuánto tiempo se guardan las notificaciones?

**Indefinidamente.** El sistema guarda todas las notificaciones sin límite de tiempo.

**Puede eliminar manualmente** notificaciones antiguas si lo desea.

---

### ¿Puedo configurar notificaciones diferentes por proyecto?

**No.** Las preferencias son **globales para el usuario**, no por proyecto/cliente/factura.

Todos los eventos del mismo tipo usan la misma configuración.

---

### ¿Los administradores ven mis preferencias?

**No.** Las preferencias de notificación son **privadas** de cada usuario.

Los administradores solo configuran las opciones del sistema, no las preferencias individuales.

---

### ¿Qué pasa si cambio mi email?

1. El sistema **actualiza automáticamente** el destino
2. Las notificaciones futuras irán al nuevo email
3. El historial **no** se pierde

**Recomendación:** Después de cambiar email, usar "Probar Email" para confirmar.

---

### ¿Puedo recibir notificaciones de otro usuario?

**No.** Cada usuario recibe solo **sus propias notificaciones**:
- Facturas que creó
- Pagos que registró
- Clientes que agregó

**Excepción:** Administradores reciben notificaciones del sistema (nuevo cliente, nuevo usuario).

---

### ¿Las notificaciones afectan el rendimiento del sistema?

**No.** El sistema de notificaciones está optimizado:
- WebSocket es asíncrono
- Emails se envían en segundo plano
- No bloquea operaciones del usuario

---

### ¿Puedo personalizar el contenido de las notificaciones?

**Solo administradores** pueden personalizar:
- Plantillas de email (HTML)
- Plantillas de WhatsApp (requiere aprobación Meta)

**Usuarios normales:** No pueden personalizar el contenido, solo elegir qué notificaciones recibir.

---

### ¿Hay límite de notificaciones que puedo recibir?

**No hay límite.** Puede recibir tantas notificaciones como eventos ocurran.

**Para reducirlas:** Use la opción de "Resumen diario" o "Resumen semanal".

---

## 📚 Casos de Uso Prácticos

### Caso 1: Vendedor que recibe muchas notificaciones

**Problema:**
- Recibe 50+ notificaciones diarias
- La mayoría son de facturas creadas
- Interrumpen el trabajo

**Solución:**
1. Preferencias → Factura Creada
2. Desactivar canal WEB (mantener solo EMAIL)
3. Cambiar frecuencia a "Resumen diario a las 18:00"
4. Resultado: Un solo email al final del día con todas las facturas

---

### Caso 2: Administrador que quiere monitoreo completo

**Objetivo:**
- Recibir todas las notificaciones importantes
- Por todos los canales
- En tiempo real

**Configuración:**
1. Preferencias → Activar TODO
2. Web: ✅ Todas marcadas
3. Email: ✅ Todas marcadas
4. Frecuencia: Inmediata
5. Email en perfil: correcto
6. Resultado: Notificación instantánea de todo

---

### Caso 3: Usuario que solo quiere pagos

**Objetivo:**
- Solo recibir notificaciones de pagos recibidos
- Ignorar todo lo demás

**Configuración:**
1. Preferencias → Desmarcar todo EXCEPTO:
   - ✅ Pago Recibido (WEB + EMAIL)
2. Resto: ☐ Desactivado
3. Resultado: Solo notificaciones de pagos

---

## 🔗 Enlaces Relacionados

- [Manual de Configuración del Sistema](MANUAL_CONFIGURACION_SISTEMA.md)
- [Manual de Reportes y Exportación](MANUAL_REPORTES_EXPORTACION.md)
- [Guía de Permisos y Roles](MANUAL_USUARIO_PERMISOS.md)
- [Configuración de Email SMTP](../configuracion/CONFIGURACION_EMAIL.md)

---

## 📝 Registro de Cambios

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0 | 04/01/2026 | Versión inicial del manual |

---

**Documento actualizado:** 4 de enero de 2026  
**Versión del sistema:** 4.0 - Sprint 4  
**Autor:** Equipo de Desarrollo ERP Spring Manager  

---

*Este manual está sujeto a cambios conforme el sistema evoluciona. Consulte siempre la versión más reciente en la documentación oficial.*
