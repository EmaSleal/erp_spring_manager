## 1️⃣ ARQUITECTURA GENERAL

### **Decisión 1.1: Arquitectura MVC con Spring Boot**

#### ✅ Decisión Final:
**MVC (Model-View-Controller) + Service Layer**

#### 📊 Estructura Aplicada:
```
Controllers → Services → Repositories → Models
    ↓
  Views (Thymeleaf)
```

#### 🎯 Justificación:
- ✅ Separación clara de responsabilidades
- ✅ Código testeable y mantenible
- ✅ Escalable para Sprint 2+
- ✅ Estándar de la industria con Spring Boot

#### ❌ Alternativas Descartadas:
- **Arquitectura en capas plana:** Difícil de mantener a largo plazo
- **Clean Architecture:** Demasiado complejo para el alcance actual

---

### **Decisión 1.2: Monolito vs Microservicios**

#### ✅ Decisión Final:
**Aplicación monolítica**

#### 🎯 Justificación:
- ✅ Más simple de desarrollar y desplegar
- ✅ Suficiente para el alcance actual
- ✅ Menor overhead operacional
- ✅ Fácil debugging

#### ❌ Alternativas Descartadas:
- **Microservicios:** Complejidad innecesaria para v1.0
- **Serverless:** No justificado por el tamaño del equipo

---

