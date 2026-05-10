## 🐛 Problema Detectado

Las páginas del módulo de administración de roles (`/admin/roles`) estaban utilizando referencias incorrectas a componentes, creando sus propias versiones en lugar de reutilizar los componentes estándar existentes en `templates/components/`.

### Síntomas:
- **Navbar y Sidebar duplicados**: Las vistas referenciaban `~{layout :: navbar}` y `~{layout :: sidebar}` en lugar de los componentes correctos
- **Estructura HTML inconsistente**: No seguían el patrón `<main class="main-content">` usado en el resto de la aplicación
- **Sin breadcrumbs**: Faltaba navegación contextual
- **Sin footer**: No incluían el componente de pie de página
- **Diseño inconsistente**: Cards y estilos diferentes al resto del sistema

### Archivos Afectados:
1. `templates/admin/roles/roles.html` (listado de roles)
2. `templates/admin/roles/formulario.html` (crear/editar roles)

---

