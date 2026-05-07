## 📦 FASE 3: GESTIÓN DE USUARIOS

### Objetivo
Permitir a los administradores gestionar usuarios del sistema.

### Tareas

#### 3.1 Controller y Vistas
**Archivo:** `UsuarioController.java` (extender existente)

**Endpoints nuevos:**
```java
GET  /usuarios                     → Lista de usuarios
GET  /usuarios/form                → Formulario crear usuario
GET  /usuarios/form/{id}           → Formulario editar usuario
POST /usuarios/save                → Guardar usuario
POST /usuarios/delete/{id}         → Eliminar usuario
POST /usuarios/toggle-active/{id}  → Activar/desactivar
POST /usuarios/reset-password/{id} → Resetear contraseña
```

#### 3.2 Vista Principal
**Archivo:** `usuarios/usuarios.html`

**Características:**
- Tabla con todos los usuarios
- Columnas: ID, Nombre, Teléfono, Email, Rol, Estado, Acciones
- Filtros: por rol, por estado (activo/inactivo)
- Búsqueda por nombre o teléfono
- Paginación
- Botones: Nuevo Usuario, Editar, Eliminar, Activar/Desactivar

#### 3.3 Vista Formulario
**Archivo:** `usuarios/form.html`

**Campos:**
- Nombre (required)
- Teléfono (required, unique)
- Email (optional, unique)
- Contraseña (required en creación)
- Confirmar contraseña
- Rol (select: ADMIN, USER, VENDEDOR)
- Estado activo (checkbox)

#### 3.4 Funcionalidades
- ✅ CRUD completo de usuarios
- ✅ Validación de teléfono único
- ✅ Validación de email único
- ✅ Generar contraseña aleatoria
- ✅ Enviar contraseña por email/WhatsApp
- ✅ Activar/desactivar usuarios (soft delete)
- ✅ Resetear contraseña
- ✅ Ver última actividad del usuario
- ✅ Restricción: solo ADMIN puede gestionar usuarios

---

