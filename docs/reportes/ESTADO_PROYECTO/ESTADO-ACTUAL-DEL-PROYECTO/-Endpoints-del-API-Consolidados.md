## 🎯 Endpoints del API (Consolidados)

### Autenticación
- `POST /auth/login` - Login de usuario
- `POST /auth/register` - Registro de usuario
- `POST /auth/logout` - Cerrar sesión

### Dashboard
- `GET /dashboard` - Dashboard principal

### Usuarios (Sprint 2)
- `GET /usuarios` - Listar usuarios (paginado)
- `GET /usuarios/form` - Formulario nuevo usuario
- `GET /usuarios/form/{id}` - Formulario editar usuario
- `POST /usuarios/save` - Guardar usuario
- `POST /usuarios/{id}/activar` - Activar usuario
- `POST /usuarios/{id}/desactivar` - Desactivar usuario
- `POST /usuarios/{id}/reset-password` - Resetear contraseña

### Configuración (Sprint 2)
- `GET /configuracion` - Panel de configuración
- `GET /configuracion/notificaciones` - Ver configuración de notificaciones
- `POST /configuracion/notificaciones` - Guardar configuración de notificaciones

### Reportes (Sprint 2)
- `GET /reportes` - Panel de reportes
- `GET /reportes/facturas` - Formulario reporte de facturas
- `GET /reportes/facturas/generar` - Generar reporte de facturas (PDF/Excel/CSV)
- `GET /reportes/clientes` - Formulario reporte de clientes
- `GET /reportes/clientes/generar` - Generar reporte de clientes (PDF/Excel/CSV)
- `GET /reportes/productos` - Formulario reporte de productos
- `GET /reportes/productos/generar` - Generar reporte de productos (PDF/Excel/CSV)
- `GET /perfil` - Ver perfil
- `GET /perfil/editar` - Formulario de edición
- `POST /perfil/actualizar` - Actualizar información
- `POST /perfil/cambiar-password` - Cambiar contraseña
- `POST /perfil/subir-avatar` - Subir avatar
- `POST /perfil/eliminar-avatar` - Eliminar avatar

### Clientes
- `GET /clientes` - Listar clientes
- `GET /clientes/form` - Formulario nuevo
- `GET /clientes/form/{id}` - Formulario editar
- `POST /clientes/save` - Guardar cliente
- `DELETE /clientes/delete/{id}` - Eliminar cliente

### Productos
- `GET /productos` - Listar productos
- `GET /productos/form` - Formulario nuevo
- `GET /productos/form/{id}` - Formulario editar
- `POST /productos/save` - Guardar producto
- `DELETE /productos/delete/{id}` - Eliminar producto

### Facturas (Sprint 1)
- `GET /facturas` - Listar facturas (paginado)
- `GET /facturas/add` - Formulario nueva factura
- `GET /facturas/form/{id}` - Editar factura
- `POST /facturas/save` - Guardar factura
- `PUT /facturas/actualizar-estado/{id}` - Actualizar estado
- `DELETE /facturas/delete/{id}` - Eliminar factura

---

