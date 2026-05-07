## 🔧 Archivos de Configuración

### `application.yml`
```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
  jpa:
    hibernate:
      ddl-auto: update                 # Auto-migraciones
  thymeleaf:
    cache: false                       # Desarrollo: sin cache
  security:
    enabled: true
```

### `.env.local`
```properties
DB_HOST=192.168.100.8
DB_PORT=3306
DB_NAME=facturas_monrachem
DB_USER=admin_facturas
DB_PASSWORD=********
WHATSAPP_TOKEN=********
WHATSAPP_PHONE_ID=********
```

### `pom.xml` - Dependencias Principales
```xml
- Spring Boot 3.5.0
- Spring Data JPA
- Spring Security
- MySQL Connector 8.0
- Thymeleaf + Extras Security
- Lombok
- Validation
- DevTools
```

---

