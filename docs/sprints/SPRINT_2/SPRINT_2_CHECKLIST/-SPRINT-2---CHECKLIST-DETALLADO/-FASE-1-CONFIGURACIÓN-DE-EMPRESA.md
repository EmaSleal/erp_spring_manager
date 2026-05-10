## 📦 FASE 1: CONFIGURACIÓN DE EMPRESA

### 1.1 Modelo y Base de Datos

☑ 1.1.1 Crear modelo Empresa.java
      - Campos: id, nombre, ruc, dirección, teléfono, email, logo, etc.
      - Anotaciones JPA correctas
      - Validaciones con @NotNull, @Size
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 1.1.2 Crear script SQL para tabla empresa
      - CREATE TABLE empresa
      - INSERT registro por defecto
      - Índices necesarios
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

□ 1.1.3 Ejecutar migración en base de datos
      - Verificar tabla creada
      - Verificar datos por defecto
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅
      Nota: Hibernate DDL auto crea automáticamente

### 1.2 Capa de Datos

☑ 1.2.1 Crear EmpresaRepository.java
      - Extender JpaRepository<Empresa, Integer>
      - Métodos personalizados si necesario
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 1.2.2 Crear EmpresaService.java (interfaz)
      - findById()
      - findEmpresaPrincipal()
      - save()
      - update()
      - guardarLogo() / guardarFavicon()
      - eliminarLogo() / eliminarFavicon()
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 1.2.3 Crear EmpresaServiceImpl.java
      - Implementar todos los métodos
      - Manejo de excepciones
      - Logging con @Slf4j
      - Upload de archivos (logo, favicon)
      - Validaciones de negocio
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

### 1.3 Controlador

☑ 1.3.1 Crear ConfiguracionController.java
      - GET /configuracion → Vista principal
      - GET /configuracion/empresa → Tab empresa
      - POST /configuracion/empresa/guardar → Guardar datos
      - POST /configuracion/empresa/subir-logo → Upload logo
      - POST /configuracion/empresa/subir-favicon → Upload favicon
      - POST /configuracion/empresa/eliminar-logo → Eliminar logo
      - POST /configuracion/empresa/eliminar-favicon → Eliminar favicon
      - @PreAuthorize("hasRole('ADMIN')") → Solo ADMIN
      - Manejo de errores con try-catch
      - RedirectAttributes para mensajes flash
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

### 1.4 Vistas

☑ 1.4.1 Crear configuracion/index.html
      - Layout con tabs (Bootstrap)
      - Tab Empresa (activo)
      - Tab Facturación (disabled - próximo)
      - Tab Usuarios (disabled - próximo)
      - Tab Notificaciones (disabled - próximo)
      - Breadcrumbs de navegación
      - Alertas de mensajes flash (success/error)
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 1.4.2 Crear configuracion/empresa.html
      - Formulario con todos los campos
      - Validaciones HTML5
      - Upload de logo con preview
      - Upload de favicon con preview
      - Botones eliminar logo/favicon
      - Switch activo/inactivo
      - Botones guardar/limpiar
      - Fragment reutilizable
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 1.4.3 Crear CSS personalizado (configuracion.css)
      - Estilos para tabs
      - Preview de logo y favicon
      - Estilos para formulario
      - Responsive design (móvil y tablet)
      - Animaciones
      - Utilidades
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

☑ 1.4.4 Crear JavaScript (configuracion.js)
      - Validación de formularios
      - Preview de logo
      - Upload de logo con AJAX
      - Preview de favicon
      - Upload de favicon con AJAX
      - Confirmaciones con SweetAlert2
      - Limpiar formulario
      - Auto-ocultar alertas
      
      Estado: □ Pendiente  □ En progreso  ☑ Completado ✅

---

