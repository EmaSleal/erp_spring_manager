## 4️⃣ BASE DE DATOS

### **Decisión 4.1: MySQL 8.0**

#### ✅ Decisión Final:
**MySQL 8.0**

#### 🎯 Justificación:
- ✅ Open source y gratuito
- ✅ Rendimiento comprobado
- ✅ JSON support nativo
- ✅ Window functions (para reportes)
- ✅ Amplia documentación

#### 📝 Configuración:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/whatsapp_orders
    driver-class-name: com.mysql.cj.jdbc.Driver
```

#### ❌ Alternativas Descartadas:
- **PostgreSQL:** Overkill para el alcance actual
- **SQLite:** No soporta concurrencia
- **MongoDB:** No relacional (no justificado)

---

### **Decisión 4.2: Estrategia de Migración**

#### ✅ Decisión Final:
**Hibernate DDL Auto + Scripts SQL manuales**

#### 🎯 Justificación:
- ✅ `ddl-auto: update` en desarrollo (rápido)
- ✅ Scripts SQL para cambios complejos
- ✅ `ddl-auto: validate` en producción (seguro)

#### 📝 Implementación:
```yaml
# Desarrollo
spring:
  jpa:
    hibernate:
      ddl-auto: update

# Producción
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

#### 📄 Scripts Manuales:
- `MIGRATION_USUARIO_FASE_4.sql` - Agregar campos a Usuario

---

### **Decisión 4.3: Convenciones de Nomenclatura**

#### ✅ Decisión Final:
**snake_case para BD, camelCase para Java**

#### 📝 Ejemplos:
```java
// Java
private String nombreCompleto;

// Base de datos
nombre_completo VARCHAR(100)

// JPA mapea automáticamente
@Column(name = "nombre_completo")
private String nombreCompleto;
```

---

