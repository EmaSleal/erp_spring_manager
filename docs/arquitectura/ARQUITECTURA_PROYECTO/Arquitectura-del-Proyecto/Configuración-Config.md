##  Configuración (Config)

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/config/`

| Clase de Configuración | Propósito |
|------------------------|-----------|
| `SecurityConfig.java` | Configuración de Spring Security (autenticación, autorización, rutas públicas) |
| `PersistenceConfig.java` | Configuración de JPA y auditoría (`@EnableJpaAuditing`) |
| `AuditorAwareImpl.java` | Proveedor del usuario actual para auditoría |
| `GlobalControllerAdvice.java` | Manejo global de excepciones y atributos comunes |

### Características de Seguridad

- Autenticación basada en formulario
- Codificación BCrypt para contraseñas
- Roles de usuario: `ADMIN`, `USER`
- CSRF habilitado
- Logout personalizado

---

