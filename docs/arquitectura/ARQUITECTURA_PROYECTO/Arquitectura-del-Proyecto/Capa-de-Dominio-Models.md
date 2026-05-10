##  Capa de Dominio (Models)

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/models/`

### Entidades Principales

| Clase | Descripción | Anotaciones JPA |
|-------|-------------|-----------------|
| `Cliente.java` | Representa un cliente del sistema | `@Entity`, `@Table`, `@EntityListeners` |
| `Factura.java` | Representa una factura | `@Entity`, `@Table`, `@EntityListeners` |
| `LineaFactura.java` | Línea de detalle de factura | `@Entity`, `@Table`, `@EntityListeners` |
| `Producto.java` | Representa un producto | `@Entity`, `@Table`, `@EntityListeners` |
| `Usuario.java` | Usuario del sistema | `@Entity`, `@Table`, `@EntityListeners` |
| `Empresa.java` | Datos de la empresa | `@Entity`, `@Table`, `@EntityListeners` |
| `Presentacion.java` | Presentación de productos | `@Entity`, `@Table` |
| `ConfiguracionFacturacion.java` | Configuración de facturación | `@Entity`, `@Table` |
| `ConfiguracionNotificaciones.java` | Configuración de notificaciones | `@Entity`, `@Table`, `@EntityListeners` |
| `WebhookLog.java` | Registro de webhooks de WhatsApp | `@Entity`, `@Table` |

### DTOs y Records

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/models/dto/`

| Clase | Tipo | Descripción |
|-------|------|-------------|
| `ModuloDTO.java` | DTO | Transferencia de datos de módulos |
| `ProductoRecord.java` | Record | Record inmutable de producto |
| `LineaFacturaR.java` | Record | Record inmutable de línea de factura |

### Características de Auditoría

Todas las entidades principales implementan auditoría automática con:
- `createdBy` - Usuario que creó el registro
- `createdDate` - Fecha de creación
- `lastModifiedBy` - Último usuario que modificó
- `lastModifiedDate` - Fecha de última modificación

**Implementación:** `@EntityListeners(AuditingEntityListener.class)`

---

