## 🎯 Configuración por Ambiente

### **application.yml** (Base)
```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
  level:
    root: INFO
    api.astro.whats_orders_manager: INFO
```

### **application-dev.yml** (Desarrollo)
```yaml
logging:
  level:
    root: INFO
    api.astro.whats_orders_manager: DEBUG
    org.springframework: INFO
    org.hibernate.SQL: DEBUG
```

### **application-prod.yml** (Producción)
```yaml
logging:
  level:
    root: WARN
    api.astro.whats_orders_manager: INFO
    org.springframework: WARN
  file:
    name: /var/log/whatsapp-orders-manager/application.log
    max-size: 10MB
    max-history: 30
```

---

