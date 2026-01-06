# Almacenamiento de Imágenes - Documentación

Este documento describe dónde y cómo se guardan las imágenes en el sistema ERP Spring Manager.

## 📍 Ubicaciones de Almacenamiento

El sistema utiliza **dos ubicaciones principales** para guardar imágenes:

### 1. Avatares de Usuario
- **Directorio**: `src/main/resources/static/images/avatars/`
- **URL Pública**: `/images/avatars/`
- **Controlador**: `PerfilController.java`
- **Método**: `subirAvatar()`

#### Características:
- Formato de nombre: `avatar_{idUsuario}_{UUID}.{extension}`
- Extensiones permitidas: `.jpg`, `.jpeg`, `.png`
- Tamaño máximo: **2MB**
- Acceso: Público (configurado en `SecurityConfig.java`)

#### Ejemplo de uso:
```java
// En PerfilController.java - Línea 54
private static final String UPLOAD_DIR = "src/main/resources/static/images/avatars/";

// Guardar avatar (Línea 239-296)
@PostMapping("/perfil/subir-avatar")
public String subirAvatar(@RequestParam("avatar") MultipartFile file, ...)
```

#### Flujo de guardado:
1. Usuario sube imagen desde `/perfil/editar`
2. Se valida tipo (imagen) y tamaño (máx 2MB)
3. Se genera nombre único: `avatar_{idUsuario}_{UUID}.{ext}`
4. Se crea directorio si no existe
5. Se elimina avatar anterior (si existe)
6. Se guarda nuevo archivo
7. Se actualiza URL en base de datos: `/images/avatars/{filename}`

### 2. Logos y Favicons de Empresa
- **Directorio Base**: Configurado en `application.yml` como `app.upload.dir`
- **Directorio por Defecto**: `uploads/empresa/`
- **Servicio**: `EmpresaServiceImpl.java`
- **Métodos**: `guardarLogo()`, `guardarFavicon()`

#### Características:
- Formato de nombre: 
  - Logo: `logo_{UUID}.{extension}`
  - Favicon: `favicon_{UUID}.{extension}`
- Extensiones permitidas: `.png`, `.jpg`, `.jpeg`, `.svg`, `.ico`
- Tamaño máximo: **2MB**
- Subdirectorio: `empresa/`

#### Configuración:
```yaml
# En application.yml (no configurado explícitamente, usa valor por defecto)
app:
  upload:
    dir: uploads  # Valor por defecto
```

```java
// En EmpresaServiceImpl.java - Líneas 43-49
@Value("${app.upload.dir:uploads}")
private String uploadDir;

private static final String EMPRESA_SUBDIR = "empresa";
```

#### Flujo de guardado:
1. Administrador sube logo/favicon desde `/configuracion` (tab Empresa)
2. Se valida tipo (imagen) y tamaño (máx 2MB)
3. Se valida extensión permitida
4. Se crea directorio `uploads/empresa/` si no existe
5. Se genera nombre único: `{tipo}_{UUID}.{ext}`
6. Se elimina archivo anterior (si existe)
7. Se guarda nuevo archivo
8. Se actualiza nombre en base de datos (solo nombre, no ruta completa)

## 🔒 Configuración de Seguridad

### Rutas Públicas
En `SecurityConfig.java` (línea 26):
```java
.requestMatchers("/", "/auth/**", "/css/**", "/js/**", "/images/**").permitAll()
```

Esto permite que las imágenes en `/images/**` sean accesibles públicamente sin autenticación.

### Nota Importante sobre Logos y Favicons
⚠️ **El directorio `uploads/` NO está mapeado como recurso estático por defecto.**

Para servir archivos desde `uploads/`, se necesitaría:
1. Agregar un `WebMvcConfigurer` para mapear el directorio, O
2. Crear un controlador que sirva los archivos, O
3. Mover los archivos a `src/main/resources/static/`

**Código sugerido para mapear uploads como recurso estático:**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
```

## 📊 Resumen de Ubicaciones

| Tipo de Imagen | Directorio Físico | URL Pública | Controlador/Servicio |
|----------------|-------------------|-------------|----------------------|
| Avatar Usuario | `src/main/resources/static/images/avatars/` | `/images/avatars/{filename}` | `PerfilController` |
| Logo Empresa | `uploads/empresa/` | **No mapeado** | `EmpresaServiceImpl` |
| Favicon Empresa | `uploads/empresa/` | **No mapeado** | `EmpresaServiceImpl` |

## 🛠️ Métodos Principales

### PerfilController.java
```java
// Línea 239: Subir avatar
@PostMapping("/perfil/subir-avatar")
public String subirAvatar(@RequestParam("avatar") MultipartFile file, ...)

// Línea 302: Eliminar avatar
@PostMapping("/perfil/eliminar-avatar")
public String eliminarAvatar(Authentication authentication, ...)

// Línea 54: Constante del directorio
private static final String UPLOAD_DIR = "src/main/resources/static/images/avatars/";
```

### EmpresaServiceImpl.java
```java
// Línea 153: Guardar logo
public Empresa guardarLogo(Integer empresaId, MultipartFile file, Integer usuarioId)

// Línea 177: Guardar favicon
public Empresa guardarFavicon(Integer empresaId, MultipartFile file, Integer usuarioId)

// Línea 201: Eliminar logo
public Empresa eliminarLogo(Integer empresaId, Integer usuarioId)

// Línea 218: Eliminar favicon
public Empresa eliminarFavicon(Integer empresaId, Integer usuarioId)

// Línea 309: Método privado para guardar archivo
private String guardarArchivo(MultipartFile file, String prefijo)

// Línea 334: Método privado para eliminar archivo
private void eliminarArchivo(String nombreArchivo)
```

## 📝 Validaciones

### Avatares (PerfilController)
- Archivo no vacío
- Tipo: `image/*` (ContentType)
- Tamaño: Máximo 2MB
- Código: Línea 448-472

### Logos/Favicons (EmpresaServiceImpl)
- Archivo no vacío
- Extensiones: `.png`, `.jpg`, `.jpeg`, `.svg`, `.ico`
- Tamaño: Máximo 2MB
- Código: Línea 273-300

## 🎯 Ejemplos de URLs

### Avatar de Usuario
```
Archivo guardado: src/main/resources/static/images/avatars/avatar_1_a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg
URL en DB: /images/avatars/avatar_1_a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg
URL Pública: http://localhost:8080/images/avatars/avatar_1_a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg
```

### Logo de Empresa
```
Archivo guardado: uploads/empresa/logo_a1b2c3d4-e5f6-7890-abcd-ef1234567890.png
Nombre en DB: logo_a1b2c3d4-e5f6-7890-abcd-ef1234567890.png
URL Pública: ⚠️ NO DISPONIBLE (requiere configuración adicional)
```

## 🔧 Configuración Recomendada

Para un funcionamiento completo y consistente, se recomienda:

1. **Unificar almacenamiento**: Usar una sola ubicación para todas las imágenes
2. **Mapear recurso uploads**: Agregar WebMvcConfigurer para servir archivos desde `uploads/`
3. **Configurar en application.yml**:
```yaml
app:
  upload:
    dir: uploads
    max-file-size: 2MB
```

## 📚 Referencias

- **PerfilController.java**: `/src/main/java/api/astro/whats_orders_manager/controllers/PerfilController.java`
- **ConfiguracionController.java**: `/src/main/java/api/astro/whats_orders_manager/controllers/ConfiguracionController.java`
- **EmpresaServiceImpl.java**: `/src/main/java/api/astro/whats_orders_manager/services/impl/EmpresaServiceImpl.java`
- **SecurityConfig.java**: `/src/main/java/api/astro/whats_orders_manager/config/SecurityConfig.java`
- **application.yml**: `/src/main/resources/application.yml`

---

**Fecha de creación**: 2026-01-06  
**Versión**: 1.0
