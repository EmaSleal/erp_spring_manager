## 📦 FASE 9: MIGRAR MÓDULO SEGURIDAD

**Duración:** 5 horas  
**Complejidad:** ⭐⭐⭐⭐ Muy Alta

### Archivos a Migrar

```
Controllers (8):
├── AuthController.java
├── UsuarioController.java
├── UsuarioAdminController.java
├── PermisosController.java
├── PermisoAdminController.java
├── RolAdminController.java
├── PerfilController.java
└── (otros relacionados)

Services (5):
├── UsuarioService.java
├── PermisoService.java
├── RolService.java
├── UsuarioPermisoService.java
└── UsuarioActividadService.java

Repositories (5):
├── UsuarioRepository.java
├── PermisoRepository.java
├── RolRepository.java
├── UsuarioPermisoRepository.java
└── UsuarioActividadRepository.java

Models (6):
├── Usuario.java
├── Permiso.java
├── Rol.java
├── UsuarioPermiso.java
├── UsuarioActividad.java
└── UsuarioSesion.java

DTOs:
├── UsuarioDTO.java
├── PermisoDTO.java
└── (otros)

Enums:
└── TipoPermiso.java (si existe)
```

### ⚠️ IMPORTANTE: Este módulo es CRÍTICO

**Seguridad es usado por TODO el sistema:**
- Autenticación y autorización
- Control de acceso
- Sesiones de usuario
- Auditoría

### Estrategia Conservadora

1. ✅ **Mover en pequeños grupos** (no todo de golpe)
2. ✅ **Compilar después de cada grupo**
3. ✅ **No mover SecurityConfig** (queda en `shared/config/`)
4. ✅ **Hacer backup antes** (`git stash` o commit temporal)

### Orden Seguro

```
Grupo 1: Models básicos
├── Usuario.java
├── Rol.java
└── Permiso.java
→ Compilar y verificar

Grupo 2: Models de relación
├── UsuarioPermiso.java
├── UsuarioActividad.java
└── UsuarioSesion.java
→ Compilar y verificar

Grupo 3: Repositories
├── Todos los repositories (5 archivos)
→ Compilar y verificar

Grupo 4: Services
├── Todos los services (5 archivos)
→ Compilar y verificar

Grupo 5: Controllers
├── Todos los controllers (8 archivos)
→ Compilar y verificar
```

### Testing Crítico

```bash
# Tests de autenticación
mvn test -Dtest=AuthTest

# Tests de permisos
mvn test -Dtest=PermisoTest

# Tests de usuario
mvn test -Dtest=UsuarioTest

# Test completo
mvn test

# Arrancar aplicación y probar login
mvn spring-boot:run
# Ir a http://localhost:9090/login
# Verificar que login funciona
```

---

