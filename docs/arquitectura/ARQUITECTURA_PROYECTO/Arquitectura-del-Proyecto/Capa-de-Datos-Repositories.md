##  Capa de Datos (Repositories)

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/repositories/`

### Repositorios JPA

Todos los repositorios extienden `JpaRepository<Entity, ID>` proporcionando operaciones CRUD básicas.

| Repositorio | Entidad | Métodos Personalizados |
|-------------|---------|------------------------|
| `ClienteRepository.java` | Cliente | `findByTelefono()`, `findByNombreContaining()` |
| `FacturaRepository.java` | Factura | `findByClienteId()`, `findByEstado()`, queries de reportes |
| `LineaFacturaRepository.java` | LineaFactura | `findByFacturaId()` |
| `ProductoRepository.java` | Producto | `findByNombreContaining()`, `findByCategoriaId()` |
| `UsuarioRepository.java` | Usuario | `findByNombreUsuario()`, `findByTelefono()` |
| `EmpresaRepository.java` | Empresa | - |
| `PresentacionRepository.java` | Presentacion | - |
| `ConfiguracionFacturacionRepository.java` | ConfiguracionFacturacion | - |
| `ConfiguracionNotificacionesRepository.java` | ConfiguracionNotificaciones | - |
| `WebhookLogRepository.java` | WebhookLog | `findByTelefono()`, `findByEstado()` |

### Queries Nativas

Algunos repositorios incluyen `@Query` con SQL nativo para:
- Reportes complejos
- Agregaciones
- Estadísticas del dashboard

---

