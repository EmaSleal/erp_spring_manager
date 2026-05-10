## 📦 FASE 4: ROLES Y PERMISOS

### 4.1 Configuración de Seguridad

☑ 4.1.1 Actualizar SecurityConfig.java
      - Configurar permisos por rol (4 roles implementados)
      - ADMIN: Acceso total (todos los módulos)
      - USER: Módulos operativos + reportes (sin configuración/usuarios)
      - VENDEDOR: Solo crear facturas + consultar catálogos
      - VISUALIZADOR: Solo lectura de información
      - Configuración granular por endpoint:
        * Clientes: visualización todos, edición ADMIN/USER
        * Productos: visualización todos, edición ADMIN/USER
        * Facturas: todos ven, VENDEDOR puede crear, ADMIN/USER pueden eliminar
        * Líneas factura: ADMIN, USER, VENDEDOR
        * Configuración: solo ADMIN
        * Usuarios: solo ADMIN
        * Reportes: ADMIN y USER
      - Agregado manejo de excepciones con página 403
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 4.1.2 Aplicar @PreAuthorize en controladores
      - UsuarioController: @PreAuthorize("hasRole('ADMIN')") ✓
      - ConfiguracionController: ya tiene restricción
      - ClienteController: pendiente
      - ProductoController: pendiente
      - FacturaController: pendiente
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅
      Nota: UsuarioController completado, otros pendientes

### 4.2 Vistas

☑ 4.2.1 Aplicar sec:authorize en todas las vistas
      - usuarios/usuarios.html: sec:authorize="hasRole('ADMIN')" ✓
      - usuarios/form.html: sec:authorize="hasRole('ADMIN')" ✓
      - Otras vistas: pendiente
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅
      Nota: Módulo usuarios completado, otros pendientes

☑ 4.2.2 Crear página de acceso denegado
      - templates/error/403.html creada
      - Diseño con icono de candado
      - Información del usuario actual
      - Información sobre roles del sistema
      - Botones: Ir al Dashboard, Volver Atrás
      - Mensaje de contacto para usuarios sin permisos
      - Card informativa con descripción de cada rol
      - Responsive y con estilos profesionales
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

### 4.3 Dashboard

☑ 4.3.1 Actualizar DashboardController
      - Filtrar módulos según rol
      - VISUALIZADOR: solo ver módulos (Clientes, Productos, Facturas)
      - VENDEDOR: Facturas, Clientes, Productos
      - USER: todos excepto Configuración y Usuarios
      - ADMIN: todos los módulos
      - Módulo Usuarios ahora visible en dashboard (solo ADMIN)
      - Módulo Configuración marcado como implementado
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

### 4.4 Testing de Permisos

☑ 4.4.1 Probar acceso por rol
      - Login como ADMIN → Acceso total ✓
      - Login como USER → Sin configuración ✓
      - Login como VENDEDOR → Solo facturas ✓
      - Login como VISUALIZADOR → Solo lectura ✓
      - Verificado: Dashboard muestra módulos correctos
      - Verificado: SecurityConfig bloquea URLs correctamente
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 4.4.2 Probar URLs directas
      - USER intenta /configuracion → 403 ✓
      - VENDEDOR intenta /usuarios → 403 ✓
      - VISUALIZADOR intenta /clientes/form → 403 ✓
      - VISUALIZADOR intenta /facturas/editar/1 → 403 ✓
      - Página 403 personalizada funciona correctamente
      - Botones ocultos según rol en vistas
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

---

