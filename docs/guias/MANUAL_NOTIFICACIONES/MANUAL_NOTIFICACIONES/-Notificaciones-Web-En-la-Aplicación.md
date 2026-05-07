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

