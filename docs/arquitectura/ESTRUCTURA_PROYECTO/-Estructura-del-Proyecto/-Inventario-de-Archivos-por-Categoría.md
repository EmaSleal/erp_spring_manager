## 📄 Inventario de Archivos por Categoría

### 🗂️ DTOs (Data Transfer Objects)

| Archivo | Ubicación | Propósito |
|---------|-----------|-----------|
| `EstadisticasUsuariosDTO.java` | `models/dto/` | Estadísticas de usuarios |
| `ModuloDTO.java` | `models/dto/` | Información de módulos del sistema |
| `PaginacionDTO.java` | `models/dto/` | Datos de paginación |
| `PlantillaWhatsAppDTO.java` | `models/dto/` | DTO para plantillas WhatsApp |
| `ResponseDTO.java` | `models/dto/` | Respuesta genérica de API |
| `WebhookValidationDTO.java` | `models/dto/` | Validación de webhooks |
| `WhatsAppMensajeDTO.java` | `models/dto/` | DTO para mensajes WhatsApp |

**Total:** 7 DTOs

---

### 🎲 Enums (Enumeraciones)

**Nota:** Los enums están definidos como inner classes dentro de las entidades que los utilizan.

| Enum | Ubicación | Valores |
|------|-----------|---------|
| `TipoMensaje` | `MensajeWhatsApp.java` | ENVIADO, RECIBIDO |
| `EstadoMensaje` | `MensajeWhatsApp.java` | PENDIENTE, ENVIADO, ENTREGADO, LEIDO, FALLIDO |
| `CategoriaPlantilla` | `PlantillaWhatsApp.java` | MARKETING, UTILITY, AUTHENTICATION |
| `EstadoMeta` | `PlantillaWhatsApp.java` | PENDIENTE, APROBADO, RECHAZADO |

**Total:** 4 Enums (como inner classes)

**Recomendación futura:** Extraer enums a archivos separados en `models/enums/` si se reutilizan en múltiples contextos.

---

### 📝 Records (Java 17+)

| Record | Ubicación | Campos | Propósito |
|--------|-----------|--------|-----------|
| `LineaFacturaR` | `models/records/` | id, idProducto, cantidad, precioUnitario, subtotal, nombreProducto, codigoProducto | Representación inmutable de línea de factura |
| `ProductoRecord` | `models/records/` | id, codigo, nombre, descripcion, precio, stock, categoria, activo | Representación inmutable de producto |

**Total:** 2 Records

**Ventajas de Records:**
- ✅ Inmutabilidad garantizada
- ✅ Menos código boilerplate
- ✅ Equals, hashCode y toString automáticos
- ✅ Compatibilidad con pattern matching (Java 21+)

---

### 🗄️ Entidades JPA

| Entidad | Tabla | Relaciones | Descripción |
|---------|-------|------------|-------------|
| `Cliente` | `clientes` | - | Clientes del sistema |
| `ConfiguracionFacturacion` | `configuracion_facturacion` | - | Configuración de facturación |
| `ConfiguracionNotificaciones` | `configuracion_notificaciones` | - | Configuración de notificaciones |
| `Empresa` | `empresa` | - | Datos de la empresa |
| `Factura` | `facturas` | @OneToMany → LineaFactura<br>@ManyToOne → Cliente | Facturas emitidas |
| `LineaFactura` | `lineas_factura` | @ManyToOne → Factura<br>@ManyToOne → Producto | Líneas de factura |
| `MensajeWhatsApp` | `mensaje_whatsapp` | - | Mensajes enviados/recibidos |
| `PlantillaWhatsApp` | `plantilla_whatsapp` | - | Plantillas aprobadas |
| `Presentacion` | `presentaciones` | @ManyToOne → Producto | Presentaciones de productos |
| `Producto` | `productos` | @OneToMany → Presentacion | Productos del catálogo |
| `Usuario` | `usuarios` | - | Usuarios del sistema |
| `WebhookLog` | `webhook_log` | - | Logs de webhooks |

**Total:** 12 Entidades JPA

---

