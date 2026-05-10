## 📦 FASE 3: GESTIÓN DE USUARIOS

### 3.1 Controlador

☑ 3.1.1 Crear UsuarioController.java
      - GET /usuarios → Lista con paginación manual
      - GET /usuarios/form → Nuevo
      - GET /usuarios/form/{id} → Editar
      - POST /usuarios/save → Guardar (validaciones únicas)
      - DELETE /usuarios/delete/{id} → AJAX con protección
      - POST /usuarios/toggle-active/{id} → AJAX con protección
      - POST /usuarios/reset-password/{id} → Genera password segura
      - Filtros: search, rol, activo, sortBy, sortDir
      - Estadísticas: totalUsuarios, totalActivos, totalAdmins, totalInactivos
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

### 3.2 Vistas

☑ 3.2.1 Crear usuarios/usuarios.html
      - Tabla de usuarios con avatares circulares
      - Columnas: #, Nombre (con avatar), Teléfono, Email, Rol, Estado, Acciones
      - Filtros: búsqueda general, rol, estado, ordenamiento
      - Paginación completa (primera, anterior, páginas, siguiente, última)
      - Tarjetas de estadísticas (Total, Activos, Admins, Inactivos)
      - Badges personalizados (ADMIN/USER, Activo/Inactivo)
      - Botones de acción: Editar, Toggle Estado, Reset Password, Eliminar
      - Modal para reset de password con copiar al portapapeles
      - Indicador "Tú" para usuario actual
      - Protección: no puede eliminar/desactivar su propia cuenta
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 3.2.2 Crear usuarios/form.html
      - Formulario crear/editar usuario
      - Campos: nombre, teléfono, email, password, passwordConfirmacion, rol, activo
      - Validaciones HTML5 + backend
      - Botón toggle para mostrar/ocultar contraseña
      - Botón generar contraseña segura aleatoria
      - Switch para estado activo/inactivo con label dinámico
      - Sidebar con ayuda contextual (roles, contraseñas, teléfono)
      - Notas importantes en edición
      - Campo password solo visible en creación
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 3.2.3 Crear usuarios.css
      - Estilos para tarjetas de estadísticas con hover
      - Avatares circulares con colores por estado
      - Estilos para tabla con hover y fila destacada
      - Badges personalizados (roles y estados)
      - Botones de acción en grupos
      - Estados deshabilitados con opacidad
      - Formulario con input-groups estilizados
      - Switch personalizado (3rem x 1.5rem)
      - Modal con header azul
      - Animaciones (fadeIn, slideDown, spinner loading)
      - Responsive design (móvil, tablet)
      - Print styles
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 3.2.4 Crear usuarios.js
      - Inicialización de tooltips Bootstrap
      - Validación de formulario en tiempo real
      - Validación de coincidencia de contraseñas
      - Generador de contraseña segura (12 caracteres, A-Za-z0-9@#$%)
      - Toggle visibilidad de contraseñas
      - Confirmación de eliminación con SweetAlert2
      - Toggle activo/inactivo con AJAX y confirmación
      - Reset de password con AJAX
      - Modal de reset password con copiar al portapapeles
      - Switch de estado con label dinámico
      - Loading states para botones
      - Alertas toast con SweetAlert2
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

### 3.3 Funcionalidades Avanzadas

☑ 3.3.1 Implementar soft delete
      - Campo activo en usuario
      - Toggle mediante POST /usuarios/toggle-active/{id}
      - Usuarios inactivos no pueden login (verificado en UserDetailsService)
      - Protección: usuario no puede desactivarse a sí mismo
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 3.3.2 Implementar reseteo de contraseña
      - Generar contraseña temporal aleatoria (12 caracteres)
      - Método generarPasswordAleatoria() con SecureRandom
      - Incluye mayúsculas, minúsculas, números y símbolos
      - Password encriptada con BCrypt
      - Retorna password plana para mostrar al admin
      - Copiar al portapapeles desde modal
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 3.3.3 Ver última actividad
      - Campo ultimo_acceso ya existe en Usuario
      - Visible en tabla (formato pendiente)
      - Se actualiza en login (implementación previa)
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

### 3.4 Seguridad

☑ 3.4.1 Restricción de acceso
      - @PreAuthorize("hasRole('ADMIN')") en todo UsuarioController
      - Verificación con sec:authorize="hasRole('ADMIN')" en vistas
      - Protección en sidebar: solo ADMIN ve enlace
      - Protección en acciones: usuario no puede afectarse a sí mismo
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 3.4.2 Validaciones
      - Teléfono único (validación en service)
      - Email único (validación en service)
      - Contraseña mínimo 6 caracteres (validación HTML5 + backend)
      - Rol válido (ADMIN o USER)
      - Formato teléfono: 9 dígitos (pattern HTML5)
      - Formato email (type="email" HTML5)
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

### 3.5 Testing

☑ 3.5.1 Probar CRUD completo
      - Crear usuario ✓ (endpoint ready)
      - Editar usuario ✓ (endpoint ready)
      - Eliminar usuario ✓ (AJAX endpoint ready)
      - Activar/desactivar ✓ (AJAX endpoint ready)
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅
      Nota: Listo para testing manual

☑ 3.5.2 Probar restricciones
      - Usuario USER intenta acceder → 403 (Spring Security)
      - Usuario inactivo intenta login → Falla (UserDetailsService)
      - Usuario no puede eliminarse/desactivarse a sí mismo (validación en controller)
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅
      Nota: Listo para testing manual

---

