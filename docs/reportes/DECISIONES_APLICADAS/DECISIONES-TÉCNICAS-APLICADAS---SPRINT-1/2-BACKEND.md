## 2️⃣ BACKEND

### **Decisión 2.1: Java 21 LTS**

#### ✅ Decisión Final:
**Java 21 LTS** (cambio desde Java 24)

#### 🎯 Justificación:
- ✅ LTS (Long Term Support) hasta 2029
- ✅ Estabilidad comprobada
- ✅ Compatibilidad con Spring Boot 3.5.0
- ✅ Features modernas (Records, Pattern Matching, Virtual Threads)

#### 📝 Implementación:
```xml
<!-- pom.xml -->
<properties>
    <java.version>21</java.version>
</properties>
```

#### 🔄 Cambio Realizado:
- **Antes:** Java 24 (versión experimental)
- **Ahora:** Java 21 LTS (estable)
- **Fecha:** 11/10/2025

---

### **Decisión 2.2: Spring Boot 3.5.0**

#### ✅ Decisión Final:
**Spring Boot 3.5.0** (última versión estable)

#### 🎯 Justificación:
- ✅ Compatible con Java 21
- ✅ Spring Security 6.x integrado
- ✅ Soporte nativo para observabilidad
- ✅ Mejoras de rendimiento

#### 📦 Dependencias Clave:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>spring-boot-starter-data-jpa</groupId>
</dependency>
<dependency>
    <groupId>spring-boot-starter-thymeleaf</groupId>
</dependency>
<dependency>
    <groupId>spring-boot-starter-validation</groupId>
</dependency>
```

---

### **Decisión 2.3: Hibernate como ORM**

#### ✅ Decisión Final:
**Hibernate 6.6.x** (incluido en Spring Data JPA)

#### 🎯 Justificación:
- ✅ ORM maduro y confiable
- ✅ Integración nativa con Spring
- ✅ JPQL para queries complejas
- ✅ Lazy loading y caching

#### 📝 Configuración:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # Desarrollo: update, Producción: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQL8Dialect
```

#### ❌ Alternativas Descartadas:
- **JPA puro:** Menos features
- **MyBatis:** Más verboso

---

### **Decisión 2.4: Stored Procedures vs Lógica en Java**

#### ✅ Decisión Final:
**Mixto:** Stored Procedures para queries complejas, Java para lógica de negocio

#### 🎯 Justificación:
- ✅ SPs optimizadas para consultas pesadas (ej. `ObtenerProductos()`)
- ✅ Java para validaciones y lógica de negocio
- ✅ Balance entre rendimiento y mantenibilidad

#### 📝 Ejemplo:
```java
// Uso de SP
@Query(value = "CALL ObtenerProductos()", nativeQuery = true)
List<Producto> obtenerProductosConSP();
```

---

