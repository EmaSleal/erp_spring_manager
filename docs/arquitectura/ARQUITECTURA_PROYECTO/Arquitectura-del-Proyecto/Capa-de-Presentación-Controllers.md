##  Capa de Presentación (Controllers)

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/controllers/`

### Controladores Web

| Controlador | Ruta Base | Descripción |
|-------------|-----------|-------------|
| `HomeController.java` | `/` | Página de inicio |
| `AuthController.java` | `/auth` | Autenticación y registro |
| `DashboardController.java` | `/dashboard` | Panel principal |
| `ClienteController.java` | `/clientes` | CRUD de clientes |
| `ProductoController.java` | `/productos` | CRUD de productos |
| `FacturaController.java` | `/facturas` | CRUD de facturas |
| `LineaFacturaController.java` | `/lineas-factura` | API REST para líneas |
| `UsuarioController.java` | `/usuarios` | Gestión de usuarios |
| `ReporteController.java` | `/reportes` | Reportes y gráficos |
| `ConfiguracionController.java` | `/configuracion` | Configuración del sistema |
| `PerfilController.java` | `/perfil` | Perfil de usuario |
| `WhatsAppWebhookController.java` | `/webhook/whatsapp` | Webhook de WhatsApp |
| `WebhookLogController.java` | `/webhook-logs` | Logs de webhooks |

### Controlador de Errores

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/controller/`

| Controlador | Función |
|-------------|---------|
| `CustomErrorController.java` | Manejo centralizado de errores HTTP |

### Tipos de Respuesta

- **Vistas:** Return `String` (nombre de plantilla Thymeleaf)
- **REST API:** `@ResponseBody` + `ResponseEntity<?>` para JSON

---

