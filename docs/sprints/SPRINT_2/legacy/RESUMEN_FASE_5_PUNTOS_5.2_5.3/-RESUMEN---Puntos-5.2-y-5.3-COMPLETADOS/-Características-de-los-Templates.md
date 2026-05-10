## 🎨 Características de los Templates

### factura.html
**Uso:** Envío de facturas a clientes

**Secciones:**
- Header con gradiente morado
- Información de empresa (logo, RFC, dirección, contacto)
- Información del cliente
- Tabla de productos/servicios
- Cálculos (subtotal, IVA, total)
- Badge de estado (PAGADA/PENDIENTE/VENCIDA)
- Información de pago (condicional)
- Mensaje de agradecimiento

**Variables Thymeleaf:**
- `${factura.*}` - Datos de la factura
- `${empresa.*}` - Datos de la empresa
- `${factura.lineas}` - Lista de productos

### credenciales-usuario.html
**Uso:** Envío de credenciales a usuarios nuevos

**Secciones:**
- Header amarillo/naranja con icono de llave
- Saludo personalizado
- Credenciales en caja destacada (email, contraseña, rol)
- Botón de acceso al sistema
- Advertencia de cambio de contraseña
- 5 pasos para comenzar
- Funcionalidades según rol (ADMIN/USER/VENDEDOR/VISUALIZADOR)
- Nota de seguridad
- Información de contacto

**Variables Thymeleaf:**
- `${usuario.*}` - Datos del usuario
- `${contrasena}` - Contraseña temporal
- `${urlLogin}` - URL del sistema
- `${empresaEmail}` - Email de soporte

### recordatorio-pago.html
**Uso:** Recordatorios de pago automáticos

**Secciones:**
- Header amarillo/naranja con reloj
- Alerta de factura pendiente
- Badge de días de atraso (condicional)
- Monto destacado en fuente grande
- Fecha de vencimiento
- Resumen de factura
- Métodos de pago disponibles:
  - Transferencia bancaria
  - Efectivo (condicional)
  - Tarjeta (condicional)
- Advertencia de consecuencias (si está vencida)
- Botón para ver factura
- Información de contacto

**Variables Thymeleaf:**
- `${cliente.*}` - Datos del cliente
- `${factura.*}` - Datos de la factura
- `${diasVencidos}` - Días de atraso
- `${empresa.*}` - Datos bancarios de la empresa

---

