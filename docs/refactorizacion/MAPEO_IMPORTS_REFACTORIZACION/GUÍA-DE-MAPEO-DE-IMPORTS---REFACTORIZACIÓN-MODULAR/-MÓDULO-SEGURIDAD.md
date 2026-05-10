## 📦 MÓDULO SEGURIDAD

### Controllers
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.controllers.AuthController;` | `import api.astro.whats_orders_manager.modules.seguridad.controller.AuthController;` |
| `import api.astro.whats_orders_manager.controllers.UsuarioController;` | `import api.astro.whats_orders_manager.modules.seguridad.controller.UsuarioController;` |
| `import api.astro.whats_orders_manager.controllers.UsuarioAdminController;` | `import api.astro.whats_orders_manager.modules.seguridad.controller.UsuarioAdminController;` |
| `import api.astro.whats_orders_manager.controllers.PermisosController;` | `import api.astro.whats_orders_manager.modules.seguridad.controller.PermisosController;` |
| `import api.astro.whats_orders_manager.controllers.PermisoAdminController;` | `import api.astro.whats_orders_manager.modules.seguridad.controller.PermisoAdminController;` |
| `import api.astro.whats_orders_manager.controllers.RolAdminController;` | `import api.astro.whats_orders_manager.modules.seguridad.controller.RolAdminController;` |
| `import api.astro.whats_orders_manager.controllers.PerfilController;` | `import api.astro.whats_orders_manager.modules.seguridad.controller.PerfilController;` |

### Services
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.services.UsuarioService;` | `import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;` |
| `import api.astro.whats_orders_manager.services.PermisoService;` | `import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;` |
| `import api.astro.whats_orders_manager.services.RolService;` | `import api.astro.whats_orders_manager.modules.seguridad.service.RolService;` |
| `import api.astro.whats_orders_manager.services.UsuarioPermisoService;` | `import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioPermisoService;` |
| `import api.astro.whats_orders_manager.services.UsuarioActividadService;` | `import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;` |

### Repositories
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.repositories.UsuarioRepository;` | `import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;` |
| `import api.astro.whats_orders_manager.repositories.PermisoRepository;` | `import api.astro.whats_orders_manager.modules.seguridad.repository.PermisoRepository;` |
| `import api.astro.whats_orders_manager.repositories.RolRepository;` | `import api.astro.whats_orders_manager.modules.seguridad.repository.RolRepository;` |
| `import api.astro.whats_orders_manager.repositories.UsuarioPermisoRepository;` | `import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioPermisoRepository;` |
| `import api.astro.whats_orders_manager.repositories.UsuarioActividadRepository;` | `import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioActividadRepository;` |

### Models (⚠️ MUCHOS ARCHIVOS USAN ESTOS)
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.Usuario;` | `import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;` |
| `import api.astro.whats_orders_manager.models.Permiso;` | `import api.astro.whats_orders_manager.modules.seguridad.model.Permiso;` |
| `import api.astro.whats_orders_manager.models.Rol;` | `import api.astro.whats_orders_manager.modules.seguridad.model.Rol;` |
| `import api.astro.whats_orders_manager.models.UsuarioPermiso;` | `import api.astro.whats_orders_manager.modules.seguridad.model.UsuarioPermiso;` |
| `import api.astro.whats_orders_manager.models.UsuarioActividad;` | `import api.astro.whats_orders_manager.modules.seguridad.model.UsuarioActividad;` |
| `import api.astro.whats_orders_manager.models.UsuarioSesion;` | `import api.astro.whats_orders_manager.modules.seguridad.model.UsuarioSesion;` |

### DTOs
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.dto.UsuarioDTO;` | `import api.astro.whats_orders_manager.modules.seguridad.dto.UsuarioDTO;` |
| `import api.astro.whats_orders_manager.models.dto.PermisoDTO;` | `import api.astro.whats_orders_manager.modules.seguridad.dto.PermisoDTO;` |

### Enums
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.enums.TipoPermiso;` | `import api.astro.whats_orders_manager.modules.seguridad.enums.TipoPermiso;` |

---

