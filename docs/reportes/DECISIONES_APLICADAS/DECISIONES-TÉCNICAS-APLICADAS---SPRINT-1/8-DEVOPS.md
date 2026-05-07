## 8️⃣ DEVOPS

### **Decisión 8.1: Maven como Build Tool**

#### ✅ Decisión Final:
**Maven 3.6+**

#### 🎯 Justificación:
- ✅ Estándar de Spring Boot
- ✅ Gestión de dependencias robusta
- ✅ Plugins maduros
- ✅ Compatible con IDEs principales

---

### **Decisión 8.2: Perfiles de Spring**

#### ✅ Decisión Final:
**3 perfiles:** `dev`, `test`, `prod`

#### 📝 Configuración:
```yaml
# application-dev.yml
spring:
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update

# application-prod.yml
spring:
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate
```

---

