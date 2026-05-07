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

