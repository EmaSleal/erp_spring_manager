## 🎨 Templates Creados

### 1. Template de Factura (factura.html)

**Ubicación:** `src/main/resources/templates/email/factura.html`

#### Características del Diseño

**Visual:**
- 📄 Header con gradiente morado (667eea → 764ba2)
- 🏢 Sección de información de empresa con borde lateral
- 👤 Tabla de información del cliente
- 📦 Tabla de productos con hover effect
- 💰 Sección de totales con diseño destacado
- 💳 Info de pago (solo si está pendiente)
- 👍 Mensaje de agradecimiento
- 📱 Completamente responsive

**Variables Thymeleaf:**
```html
${factura.numero}           - Número de factura
${factura.fechaEmision}     - Fecha de emisión
${factura.fechaVencimiento} - Fecha de vencimiento
${factura.estado}           - Estado (PAGADA/PENDIENTE/VENCIDA)
${factura.cliente}          - Información del cliente
${factura.lineas}           - Lista de productos
${factura.subtotal}         - Subtotal
${factura.iva}              - Porcentaje de IVA
${factura.montoIva}         - Monto del IVA
${factura.total}            - Total a pagar
${factura.metodoPago}       - Método de pago
${factura.notas}            - Notas adicionales
${empresa.*}                - Información de la empresa
```

**Estados con Badges:**
- ✅ **PAGADA** → Verde (#d4edda)
- ⏳ **PENDIENTE** → Amarillo (#fff3cd)
- ❌ **VENCIDA** → Rojo (#f8d7da)

**Secciones:**
1. **Header** - Título y número de factura
2. **Información de Empresa** - Datos completos con diseño destacado
3. **Información del Cliente** - Tabla organizada
4. **Productos/Servicios** - Tabla interactiva con hover
5. **Totales** - Subtotal, IVA y Total
6. **Información de Pago** - Condicional, solo si está pendiente
7. **Agradecimiento** - Mensaje personalizado
8. **Footer** - Datos de contacto

---

### 2. Template de Credenciales de Usuario (credenciales-usuario.html)

**Ubicación:** `src/main/resources/templates/email/credenciales-usuario.html`

#### Características del Diseño

**Visual:**
- 🔑 Header amarillo/naranja (ffc107 → ff9800)
- 👋 Saludo personalizado
- 🔐 Caja destacada con credenciales
- 🚀 Botón de acceso al sistema
- ⚠️ Advertencia de cambio de contraseña
- 📝 Pasos para comenzar (numerados)
- 🔒 Nota de seguridad
- ✨ Funcionalidades según rol
- 📞 Información de contacto

**Variables Thymeleaf:**
```html
${usuario.nombre}      - Nombre del usuario
${usuario.email}       - Email/username
${usuario.rol}         - Rol asignado (ADMIN/USER/VENDEDOR/VISUALIZADOR)
${contrasena}          - Contraseña temporal
${urlLogin}            - URL del sistema
${empresaEmail}        - Email de soporte
${empresaTelefono}     - Teléfono de soporte (opcional)
```

**Roles con Badges:**
- 🔴 **ADMIN** → Rojo (#f8d7da)
- 🔵 **USER** → Azul (#d1ecf1)
- 🟢 **VENDEDOR** → Verde (#d4edda)
- ⚪ **VISUALIZADOR** → Gris (#e2e3e5)

**Secciones:**
1. **Header** - Bienvenida con icono de llave
2. **Saludo Personalizado** - Nombre del usuario
3. **Credenciales** - Email, contraseña y rol en cajas destacadas
4. **Botón de Acceso** - CTA principal
5. **Advertencia Importante** - Cambio de contraseña obligatorio
6. **Pasos para Comenzar** - 5 pasos numerados
7. **Nota de Seguridad** - Recomendaciones
8. **Funcionalidades por Rol** - Lista específica según el rol
9. **Contacto de Soporte** - Email y teléfono

**Funcionalidades por Rol:**

**ADMIN:**
- Acceso completo al sistema
- Gestión de usuarios y roles
- Configuración de empresa y facturación
- Gestión completa de clientes, productos y facturas
- Visualización del dashboard completo

**USER:**
- Gestión de clientes y productos
- Creación y edición de facturas
- Envío de facturas por email
- Visualización de reportes básicos
- Gestión de su perfil

**VENDEDOR:**
- Gestión de clientes
- Creación y edición de facturas
- Consulta de productos
- Envío de facturas por email
- Gestión de su perfil

**VISUALIZADOR:**
- Visualización de clientes
- Consulta de productos
- Visualización de facturas (sin edición)
- Visualización de reportes
- Gestión de su perfil

---

### 3. Template de Recordatorio de Pago (recordatorio-pago.html)

**Ubicación:** `src/main/resources/templates/email/recordatorio-pago.html`

#### Características del Diseño

**Visual:**
- ⏰ Header amarillo/naranja (ffc107 → ff9800)
- ⚠️ Alerta visual con icono
- 💰 Monto destacado en fuente grande
- 📅 Fecha de vencimiento prominente
- 📋 Resumen de factura en tabla
- 💳 Métodos de pago disponibles
- 🚨 Advertencia de consecuencias (si está vencida)
- 📄 Botón para ver factura completa
- 📞 Información de contacto para ayuda

**Variables Thymeleaf:**
```html
${cliente.nombre}           - Nombre del cliente
${factura.numero}           - Número de factura
${factura.fechaEmision}     - Fecha de emisión
${factura.fechaVencimiento} - Fecha de vencimiento
${factura.subtotal}         - Subtotal
${factura.iva}              - Porcentaje de IVA
${factura.montoIva}         - Monto del IVA
${factura.total}            - Total a pagar
${diasVencidos}             - Días de atraso (0 si no está vencida)
${urlFactura}               - URL para ver factura
${empresa.*}                - Información de la empresa
${empresa.banco}            - Banco
${empresa.numeroCuenta}     - Número de cuenta
${empresa.clabe}            - CLABE interbancaria
${empresa.aceptaEfectivo}   - Boolean para pago en efectivo
${empresa.aceptaTarjeta}    - Boolean para pago con tarjeta
```

**Indicadores Visuales:**
- 🟡 **Recordatorio Normal** → Fondo amarillo suave
- 🔴 **Días de Atraso** → Badge rojo con número de días
- ⚠️ **Consecuencias** → Caja roja con advertencias

**Secciones:**
1. **Header** - Recordatorio de pago con icono de reloj
2. **Saludo Personalizado** - Nombre del cliente
3. **Alerta Principal** - Factura pendiente con días de atraso (si aplica)
4. **Monto Total** - Destacado en fuente grande
5. **Fecha de Vencimiento** - Caja destacada
6. **Resumen de Factura** - Tabla completa
7. **Métodos de Pago** - Lista de opciones disponibles:
   - 🏦 Transferencia Bancaria (cuenta, CLABE)
   - 💵 Pago en Efectivo (condicional)
   - 💳 Tarjeta de Crédito/Débito (condicional)
8. **Nota Importante** - Incluir número de factura como referencia
9. **Consecuencias** - Solo si está vencida (>0 días)
10. **Botón Ver Factura** - CTA secundario
11. **Contacto para Ayuda** - Email y teléfono
12. **Mensaje Final** - Agradecimiento

**Consecuencias por Pago Atrasado:**
- Intereses moratorios
- Suspensión temporal de servicios
- Reporte a buró de crédito (casos extremos)

---

