## 📊 Configuración de Logging

### Niveles de Log por Paquete

#### Aplicación (Default - INFO)
```yaml
api.astro.whats_orders_manager: INFO
api.astro.whats_orders_manager.controllers: INFO
api.astro.whats_orders_manager.services: INFO
api.astro.whats_orders_manager.repositories: DEBUG
```

#### Hibernate/JPA (DEBUG)
```yaml
org.hibernate.SQL: DEBUG                              # Muestra queries SQL
org.hibernate.type.descriptor.sql.BasicBinder: TRACE  # Muestra parámetros
org.hibernate.orm.jdbc.bind: TRACE                    # Muestra binding
```

#### Spring Framework (INFO)
```yaml
org.springframework.web: INFO
org.springframework.security: INFO
org.springframework.jdbc.core: DEBUG
org.springframework.transaction: DEBUG
```

### Formato de Salida

```
%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

**Ejemplo:**
```
2025-10-26 14:30:15 [http-nio-8080-exec-1] INFO  a.a.w.controllers.AuthController - ✅ Login exitoso para usuario: admin
```

### Archivos de Log

| Propiedad | Valor | Descripción |
|-----------|-------|-------------|
| **Ruta** | `logs/whats-orders-manager.log` | Ubicación del archivo |
| **Tamaño Máximo** | 10 MB | Por archivo individual |
| **Historial** | 30 días | Archivos antiguos se eliminan |
| **Límite Total** | 1 GB | Tamaño máximo de todos los logs |

---

