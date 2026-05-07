##  Capa de Lógica de Negocio (Services)

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/services/`

### Patrón de Diseño

Se utiliza el patrón **Interface + Implementación**:
- **Interfaces:** Definen el contrato del servicio
- **Implementaciones:** `src/main/java/api/astro/whats_orders_manager/services/impl/`

### Servicios Principales

| Servicio | Responsabilidad |
|----------|----------------|
| `ClienteService` | Gestión de clientes |
| `FacturaService` | Gestión de facturas y líneas |
| `LineaFacturaService` | Operaciones sobre líneas de factura |
| `ProductoService` | Gestión de productos y presentaciones |
| `UsuarioService` | Gestión de usuarios |
| `EmpresaService` | Configuración de la empresa |
| `ConfiguracionFacturacionService` | Configuración de facturación |
| `ConfiguracionNotificacionesService` | Configuración de notificaciones |
| `EmailService` | Envío de emails (facturas, recordatorios) |
| `ReporteService` | Generación de reportes y estadísticas |
| `ExportService` | Exportación de datos (Excel, PDF) |
| `WebhookLogService` | Gestión de webhooks de WhatsApp |
| `PresentacionService` | Gestión de presentaciones de productos |
| `UserDetailsServiceImpl` | Autenticación de usuarios (Spring Security) |

### Transaccionalidad

Los servicios están anotados con `@Transactional` para garantizar la integridad de las operaciones.

---

