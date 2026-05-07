## 📦 FASE 5: MIGRAR MÓDULO CONFIGURACIÓN

**Duración:** 3 horas  
**Complejidad:** ⭐⭐ Media

### Archivos a Migrar

```
Controllers (5):
├── ConfiguracionController.java
├── ConfiguracionEmailRestController.java
├── ConfiguracionEmpresaRestController.java
├── ConfiguracionFacturacionRestController.java
└── ParametroSistemaRestController.java

Services (5):
├── ConfiguracionEmailService.java
├── ConfiguracionEmpresaService.java
├── ConfiguracionFacturacionService.java
├── EmpresaService.java
└── ParametroSistemaService.java

Repositories (5):
├── ConfiguracionEmailRepository.java
├── ConfiguracionEmpresaRepository.java
├── ConfiguracionFacturacionRepository.java
├── EmpresaRepository.java
└── ParametroSistemaRepository.java

Models (5):
├── ConfiguracionEmail.java
├── ConfiguracionEmpresa.java
├── ConfiguracionFacturacion.java
├── Empresa.java
└── ParametroSistema.java

DTOs:
└── (Si existen, moverlos a modules/configuracion/dto/)
```

### Script de Ayuda (PowerShell)

```powershell
# Cambiar al directorio del proyecto
cd "D:\programacion\java\spring-boot\whats_orders_manager\src\main\java\api\astro\whats_orders_manager"

# Mover Controllers
Move-Item "controllers/ConfiguracionController.java" "modules/configuracion/controller/"
Move-Item "controllers/ConfiguracionEmailRestController.java" "modules/configuracion/controller/"
Move-Item "controllers/ConfiguracionEmpresaRestController.java" "modules/configuracion/controller/"
Move-Item "controllers/ConfiguracionFacturacionRestController.java" "modules/configuracion/controller/"
Move-Item "controllers/ParametroSistemaRestController.java" "modules/configuracion/controller/"

# Mover Services
Move-Item "services/ConfiguracionEmailService.java" "modules/configuracion/service/"
Move-Item "services/ConfiguracionEmpresaService.java" "modules/configuracion/service/"
Move-Item "services/ConfiguracionFacturacionService.java" "modules/configuracion/service/"
Move-Item "services/EmpresaService.java" "modules/configuracion/service/"
Move-Item "services/ParametroSistemaService.java" "modules/configuracion/service/"

# Continuar con repositories y models...
```

### Pasos

1. ✅ Mover todos los controllers (5 archivos)
2. ✅ Mover todos los services (5 archivos)
3. ✅ Mover todos los repositories (5 archivos)
4. ✅ Mover todos los models (5 archivos)
5. ✅ Mover DTOs relacionados
6. ✅ Actualizar packages en todos los archivos
7. ✅ Actualizar imports con Find & Replace
8. ✅ Compilar: `mvn clean compile`
9. ✅ Tests: `mvn test`
10. ✅ Commit

---

