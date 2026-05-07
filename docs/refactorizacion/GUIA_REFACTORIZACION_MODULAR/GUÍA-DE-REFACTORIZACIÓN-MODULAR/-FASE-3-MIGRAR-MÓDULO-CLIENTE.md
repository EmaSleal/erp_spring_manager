## 📦 FASE 3: MIGRAR MÓDULO CLIENTE

**Duración:** 2 horas  
**Complejidad:** ⭐ Baja

### Archivos a Migrar

```
controllers/ClienteController.java → modules/cliente/controller/
services/ClienteService.java → modules/cliente/service/
repositories/ClienteRepository.java → modules/cliente/repository/
models/Cliente.java → modules/cliente/model/
models/dto/ClienteDTO.java → modules/cliente/dto/
```

### Pasos (Seguir mismo proceso que Producto)

1. ✅ Mover ClienteController.java
2. ✅ Actualizar package a `api.astro.whats_orders_manager.modules.cliente.controller`
3. ✅ Mover ClienteService.java
4. ✅ Actualizar package a `api.astro.whats_orders_manager.modules.cliente.service`
5. ✅ Mover ClienteRepository.java
6. ✅ Mover Cliente.java (modelo)
7. ✅ Mover ClienteDTO.java
8. ✅ Actualizar todos los imports (Find & Replace All)
9. ✅ Compilar: `mvn clean compile`
10. ✅ Tests: `mvn test`
11. ✅ Run: `mvn spring-boot:run`
12. ✅ Commit: "refactor: Migrar módulo Cliente a estructura modular"

---

