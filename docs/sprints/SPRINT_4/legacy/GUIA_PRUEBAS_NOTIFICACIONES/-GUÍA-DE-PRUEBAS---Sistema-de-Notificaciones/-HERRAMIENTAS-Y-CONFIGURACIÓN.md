## 🛠️ HERRAMIENTAS Y CONFIGURACIÓN

### **1. Dependencias Maven (pom.xml)**

```xml
<!-- Testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Mockito (incluido en spring-boot-starter-test) -->
<!-- JUnit 5 (incluido en spring-boot-starter-test) -->

<!-- Awaitility (para tests asíncronos) -->
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <version>4.2.0</version>
    <scope>test</scope>
</dependency>

<!-- GreenMail (Mock SMTP para email tests) -->
<dependency>
    <groupId>com.icegreen</groupId>
    <artifactId>greenmail-spring</artifactId>
    <version>2.0.0</version>
    <scope>test</scope>
</dependency>

<!-- H2 Database (para integration tests) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

---

### **2. Configuración de Test (application-test.yml)**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  
  mail:
    host: localhost
    port: 3025  # Puerto GreenMail
    username: test
    password: test
    properties:
      mail:
        smtp:
          auth: false
          starttls:
            enable: false

# Deshabilitar scheduler en tests
scheduler:
  enabled: false

# WebSocket test config
websocket:
  test-mode: true
```

---

### **3. Estructura de Carpetas Test**

```
src/test/java/api/astro/whats_orders_manager/
├── unit/                           # Tests unitarios
│   ├── service/
│   │   ├── NotificacionServiceTest.java
│   │   └── PreferenciaNotificacionServiceTest.java
│   ├── listener/
│   │   └── NotificacionListenerTest.java
│   └── events/
│       └── NotificacionEventTest.java
│
├── integration/                    # Tests de integración
│   ├── NotificacionServiceIntegrationTest.java
│   ├── EmailServiceIntegrationTest.java
│   └── WebSocketIntegrationTest.java
│
├── e2e/                           # Tests end-to-end
│   ├── NotificacionE2ETest.java
│   └── SchedulerE2ETest.java
│
└── config/                        # Configuración de tests
    ├── TestConfig.java
    └── GreenMailConfig.java
```

---

