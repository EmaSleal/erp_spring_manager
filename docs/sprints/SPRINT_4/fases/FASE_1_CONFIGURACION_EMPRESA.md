# SPRINT 4 - FASE 1: CONFIGURACIÓN DE EMPRESA

**Versión:** 1.0  
**Fecha:** 27 de diciembre de 2025  
**Estado:** ✅ COMPLETADO

---

## 📋 ÍNDICE

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Arquitectura](#arquitectura)
3. [Modelo de Datos](#modelo-de-datos)
4. [Componentes Backend](#componentes-backend)
5. [Componentes Frontend](#componentes-frontend)
6. [Flujos de Trabajo](#flujos-de-trabajo)
7. [Testing](#testing)
8. [Notas de Implementación](#notas-de-implementación)

---

## 🎯 RESUMEN EJECUTIVO

### Objetivo
Permitir a los administradores configurar completamente los datos de la empresa desde la interfaz web, eliminando la necesidad de editar archivos de configuración manualmente.

### Alcance
- Gestión de datos básicos de empresa (nombre, CIF, dirección, contacto)
- Configuración de email corporativo (SMTP)
- Personalización de logotipo
- Integración con sistema de preferencias de usuario

### Resultados
- ✅ CRUD completo de datos de empresa
- ✅ Validación de configuración SMTP con email de prueba
- ✅ Carga y visualización de logotipo
- ✅ Auditoría completa de cambios
- ✅ Integración con plantillas de email

---

## 🏗️ ARQUITECTURA

### Componentes Principales

```
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE PRESENTACIÓN                      │
├─────────────────────────────────────────────────────────────┤
│  /admin/empresa/editar.html                                 │
│  - Formulario de datos básicos                              │
│  - Configuración SMTP                                       │
│  - Carga de logotipo                                        │
│  - Envío de email de prueba                                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE CONTROLADOR                       │
├─────────────────────────────────────────────────────────────┤
│  EmpresaController.java                                     │
│  - GET  /admin/empresa/editar                               │
│  - POST /admin/empresa/actualizar                           │
│  - POST /admin/empresa/logo                                 │
│  - POST /admin/empresa/enviar-prueba                        │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE SERVICIO                          │
├─────────────────────────────────────────────────────────────┤
│  EmpresaService.java                                        │
│  - obtenerConfiguracion()                                   │
│  - actualizarConfiguracion(EmpresaDTO)                      │
│  - guardarLogo(MultipartFile)                               │
│                                                              │
│  EmailService.java                                          │
│  - enviarEmailPrueba(String destinatario)                   │
│  - Validación de configuración SMTP                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE PERSISTENCIA                      │
├─────────────────────────────────────────────────────────────┤
│  EmpresaRepository.java (JPA)                               │
│  - findById(1L) // Singleton pattern                        │
│  - save(Empresa)                                            │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    BASE DE DATOS                             │
├─────────────────────────────────────────────────────────────┤
│  Tabla: empresa                                             │
│  - Datos básicos (nombre, CIF, dirección)                   │
│  - Configuración SMTP (host, puerto, usuario)               │
│  - Ruta de logotipo                                         │
│  - Auditoría (creadoPor, modificadoPor, timestamps)         │
└─────────────────────────────────────────────────────────────┘
```

---

## 🗄️ MODELO DE DATOS

### Entidad: `Empresa`

```java
@Entity
@Table(name = "empresa")
@EntityListeners(AuditingEntityListener.class)
public class Empresa {
    
    // Identificador único (siempre ID = 1, singleton)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // DATOS BÁSICOS
    @Column(nullable = false, length = 200)
    private String nombre;
    
    @Column(length = 20)
    private String cif;
    
    @Column(length = 500)
    private String direccion;
    
    @Column(length = 100)
    private String ciudad;
    
    @Column(length = 50)
    private String provincia;
    
    @Column(length = 10)
    private String codigoPostal;
    
    @Column(length = 50)
    private String pais;
    
    // CONTACTO
    @Column(length = 20)
    private String telefono;
    
    @Column(length = 100)
    private String email;
    
    @Column(length = 200)
    private String sitioWeb;
    
    // CONFIGURACIÓN SMTP
    @Column(length = 100)
    private String smtpHost;
    
    private Integer smtpPort;
    
    @Column(length = 100)
    private String smtpUsuario;
    
    @Column(length = 100)
    private String smtpPassword; // Encriptado en próximas versiones
    
    private Boolean smtpSsl;
    private Boolean smtpAuth;
    
    // PERSONALIZACIÓN
    @Column(length = 500)
    private String logoUrl;
    
    // AUDITORÍA
    @CreatedBy
    @Column(updatable = false)
    private String creadoPor;
    
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime creadoEn;
    
    @LastModifiedBy
    private String modificadoPor;
    
    @LastModifiedDate
    private LocalDateTime modificadoEn;
}
```

### DTO: `EmpresaDTO`

```java
public class EmpresaDTO {
    // Mismo mapeo de campos que la entidad
    // Usado para transferencia de datos entre capas
    // Incluye validaciones Bean Validation
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200)
    private String nombre;
    
    @Pattern(regexp = "[A-Z0-9]{9}", message = "CIF inválido")
    private String cif;
    
    @Email(message = "Email inválido")
    private String email;
    
    // ... resto de campos
}
```

---

## ⚙️ COMPONENTES BACKEND

### 1. EmpresaController

**Ubicación:** `src/main/java/api/whats_orders_manager/controller/admin/EmpresaController.java`

**Responsabilidades:**
- Renderizar vista de configuración
- Procesar formulario de actualización
- Gestionar carga de logotipo
- Coordinar envío de email de prueba

**Endpoints:**

| Método | Ruta                               | Descripción                          | Permiso Requerido      |
|--------|-----------------------------------|--------------------------------------|------------------------|
| GET    | `/admin/empresa/editar`           | Muestra formulario de configuración | `EMPRESA_VER`          |
| POST   | `/admin/empresa/actualizar`       | Actualiza datos de empresa          | `EMPRESA_EDITAR`       |
| POST   | `/admin/empresa/logo`             | Sube logotipo de empresa            | `EMPRESA_EDITAR`       |
| POST   | `/admin/empresa/enviar-prueba`    | Envía email de prueba SMTP          | `EMPRESA_CONFIGURAR`   |

**Código clave:**

```java
@Controller
@RequestMapping("/admin/empresa")
@PreAuthorize("hasAuthority('EMPRESA_VER')")
public class EmpresaController {

    private final EmpresaService empresaService;
    private final EmailService emailService;

    @GetMapping("/editar")
    public String mostrarFormulario(Model model) {
        EmpresaDTO empresa = empresaService.obtenerConfiguracion();
        model.addAttribute("empresa", empresa);
        return "admin/empresa/editar";
    }

    @PostMapping("/actualizar")
    @PreAuthorize("hasAuthority('EMPRESA_EDITAR')")
    public String actualizar(
            @Valid @ModelAttribute EmpresaDTO empresaDTO,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Datos inválidos");
            return "redirect:/admin/empresa/editar";
        }

        try {
            empresaService.actualizarConfiguracion(empresaDTO);
            redirectAttributes.addFlashAttribute("success", "Configuración actualizada");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/admin/empresa/editar";
    }

    @PostMapping("/logo")
    @PreAuthorize("hasAuthority('EMPRESA_EDITAR')")
    public String subirLogo(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        
        try {
            String logoUrl = empresaService.guardarLogo(file);
            redirectAttributes.addFlashAttribute("success", "Logo actualizado");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar logo");
        }

        return "redirect:/admin/empresa/editar";
    }

    @PostMapping("/enviar-prueba")
    @PreAuthorize("hasAuthority('EMPRESA_CONFIGURAR')")
    public ResponseEntity<Map<String, String>> enviarEmailPrueba(
            @RequestParam String destinatario) {
        
        try {
            emailService.enviarEmailPrueba(destinatario);
            return ResponseEntity.ok(Map.of("mensaje", "Email enviado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }
}
```

---

### 2. EmpresaService

**Ubicación:** `src/main/java/api/whats_orders_manager/service/EmpresaService.java`

**Funcionalidades:**
- Implementa patrón Singleton para configuración de empresa
- Mapeo entre entidad y DTO usando ModelMapper
- Validación de datos antes de persistir
- Gestión de archivos de logotipo

**Métodos principales:**

```java
@Service
@Transactional
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final ModelMapper modelMapper;
    
    @Value("${app.upload.dir}")
    private String uploadDir;

    // Obtiene la configuración única de empresa (ID = 1)
    public EmpresaDTO obtenerConfiguracion() {
        Empresa empresa = empresaRepository.findById(1L)
            .orElseGet(() -> crearEmpresaPorDefecto());
        return modelMapper.map(empresa, EmpresaDTO.class);
    }

    // Actualiza la configuración
    public void actualizarConfiguracion(EmpresaDTO dto) {
        Empresa empresa = empresaRepository.findById(1L)
            .orElseThrow(() -> new EntityNotFoundException("Empresa no encontrada"));
        
        modelMapper.map(dto, empresa);
        empresaRepository.save(empresa);
        
        log.info("Configuración de empresa actualizada por {}", getCurrentUser());
    }

    // Guarda el logotipo
    public String guardarLogo(MultipartFile file) throws IOException {
        // Validar formato
        String contentType = file.getContentType();
        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        }

        // Generar nombre único
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        String nombreArchivo = "logo_" + System.currentTimeMillis() + "." + extension;
        
        // Guardar en disco
        Path rutaCompleta = Paths.get(uploadDir, "logos", nombreArchivo);
        Files.createDirectories(rutaCompleta.getParent());
        file.transferTo(rutaCompleta.toFile());

        // Actualizar entidad
        Empresa empresa = empresaRepository.findById(1L).orElseThrow();
        empresa.setLogoUrl("/uploads/logos/" + nombreArchivo);
        empresaRepository.save(empresa);

        return empresa.getLogoUrl();
    }

    // Crea empresa por defecto si no existe
    private Empresa crearEmpresaPorDefecto() {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setNombre("Mi Empresa");
        empresa.setPais("España");
        empresa.setSmtpPort(587);
        empresa.setSmtpSsl(true);
        empresa.setSmtpAuth(true);
        return empresaRepository.save(empresa);
    }
}
```

---

### 3. EmailService (Integración)

**Ubicación:** `src/main/java/api/whats_orders_manager/service/EmailService.java`

**Método de prueba:**

```java
public void enviarEmailPrueba(String destinatario) {
    EmpresaDTO empresa = empresaService.obtenerConfiguracion();
    
    // Validar configuración SMTP
    if (empresa.getSmtpHost() == null || empresa.getSmtpUsuario() == null) {
        throw new IllegalStateException("Configuración SMTP incompleta");
    }

    // Configurar propiedades
    Properties props = new Properties();
    props.put("mail.smtp.host", empresa.getSmtpHost());
    props.put("mail.smtp.port", empresa.getSmtpPort());
    props.put("mail.smtp.auth", empresa.getSmtpAuth());
    props.put("mail.smtp.starttls.enable", empresa.getSmtpSsl());

    // Crear sesión
    Session session = Session.getInstance(props, new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(
                empresa.getSmtpUsuario(),
                empresa.getSmtpPassword()
            );
        }
    });

    // Enviar mensaje
    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(empresa.getEmail()));
    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
    message.setSubject("Email de prueba - " + empresa.getNombre());
    message.setText("Configuración SMTP funcionando correctamente.");

    Transport.send(message);
    
    log.info("Email de prueba enviado a {} desde {}", destinatario, empresa.getEmail());
}
```

---

## 🎨 COMPONENTES FRONTEND

### Vista: `editar.html`

**Ubicación:** `src/main/resources/templates/admin/empresa/editar.html`

**Estructura:**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout}">
<head>
    <title>Configuración de Empresa</title>
</head>
<body>
    <div layout:fragment="content">
        
        <!-- Breadcrumbs -->
        <nav aria-label="breadcrumb">
            <ol class="breadcrumb">
                <li class="breadcrumb-item">
                    <a href="/admin/dashboard">Inicio</a>
                </li>
                <li class="breadcrumb-item active">Configuración de Empresa</li>
            </ol>
        </nav>

        <!-- Mensajes flash -->
        <div th:if="${success}" class="alert alert-success">
            <i class="bi bi-check-circle"></i>
            <span th:text="${success}"></span>
        </div>

        <!-- Formulario principal -->
        <div class="card">
            <div class="card-header">
                <h4><i class="bi bi-building"></i> Datos de Empresa</h4>
            </div>
            <div class="card-body">
                <form th:action="@{/admin/empresa/actualizar}" 
                      method="post" 
                      th:object="${empresa}">
                    
                    <!-- Datos básicos -->
                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label>Nombre de la empresa</label>
                                <input type="text" 
                                       class="form-control" 
                                       th:field="*{nombre}" 
                                       required>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label>CIF/NIF</label>
                                <input type="text" 
                                       class="form-control" 
                                       th:field="*{cif}">
                            </div>
                        </div>
                    </div>

                    <!-- Dirección -->
                    <div class="mb-3">
                        <label>Dirección</label>
                        <input type="text" class="form-control" th:field="*{direccion}">
                    </div>

                    <!-- Contacto -->
                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label>Teléfono</label>
                                <input type="tel" class="form-control" th:field="*{telefono}">
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label>Email</label>
                                <input type="email" class="form-control" th:field="*{email}">
                            </div>
                        </div>
                    </div>

                    <button type="submit" class="btn btn-primary">
                        <i class="bi bi-save"></i> Guardar cambios
                    </button>
                </form>
            </div>
        </div>

        <!-- Configuración SMTP -->
        <div class="card mt-4">
            <div class="card-header">
                <h4><i class="bi bi-envelope-at"></i> Configuración de Email (SMTP)</h4>
            </div>
            <div class="card-body">
                <form th:action="@{/admin/empresa/actualizar}" method="post">
                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label>Host SMTP</label>
                                <input type="text" 
                                       class="form-control" 
                                       th:field="*{smtpHost}"
                                       placeholder="smtp.gmail.com">
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label>Puerto</label>
                                <input type="number" 
                                       class="form-control" 
                                       th:field="*{smtpPort}"
                                       placeholder="587">
                            </div>
                        </div>
                    </div>

                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label>Usuario SMTP</label>
                                <input type="text" class="form-control" th:field="*{smtpUsuario}">
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label>Contraseña</label>
                                <input type="password" class="form-control" th:field="*{smtpPassword}">
                            </div>
                        </div>
                    </div>

                    <div class="form-check">
                        <input type="checkbox" 
                               class="form-check-input" 
                               th:field="*{smtpSsl}"
                               id="smtpSsl">
                        <label class="form-check-label" for="smtpSsl">
                            Usar SSL/TLS
                        </label>
                    </div>

                    <button type="submit" class="btn btn-primary mt-3">
                        <i class="bi bi-save"></i> Guardar configuración
                    </button>
                    
                    <button type="button" 
                            class="btn btn-outline-secondary mt-3"
                            onclick="enviarEmailPrueba()">
                        <i class="bi bi-send"></i> Enviar email de prueba
                    </button>
                </form>
            </div>
        </div>

        <!-- Logotipo -->
        <div class="card mt-4">
            <div class="card-header">
                <h4><i class="bi bi-image"></i> Logotipo de Empresa</h4>
            </div>
            <div class="card-body">
                <div th:if="${empresa.logoUrl}" class="mb-3">
                    <img th:src="${empresa.logoUrl}" 
                         alt="Logo actual" 
                         class="img-thumbnail"
                         style="max-height: 150px;">
                </div>

                <form th:action="@{/admin/empresa/logo}" 
                      method="post" 
                      enctype="multipart/form-data">
                    <div class="mb-3">
                        <label>Seleccionar nuevo logotipo</label>
                        <input type="file" 
                               class="form-control" 
                               name="file" 
                               accept="image/*">
                    </div>
                    <button type="submit" class="btn btn-primary">
                        <i class="bi bi-upload"></i> Subir logotipo
                    </button>
                </form>
            </div>
        </div>

    </div>

    <!-- JavaScript -->
    <th:block layout:fragment="scripts">
        <script>
            function enviarEmailPrueba() {
                const destinatario = prompt('Ingrese el email de destino:');
                if (!destinatario) return;

                fetch('/admin/empresa/enviar-prueba', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: 'destinatario=' + encodeURIComponent(destinatario)
                })
                .then(response => response.json())
                .then(data => {
                    if (data.mensaje) {
                        alert('✅ ' + data.mensaje);
                    } else {
                        alert('❌ Error: ' + data.error);
                    }
                })
                .catch(error => {
                    alert('❌ Error de conexión: ' + error);
                });
            }
        </script>
    </th:block>
</body>
</html>
```

---

## 🔄 FLUJOS DE TRABAJO

### 1. Actualizar Datos de Empresa

```
Usuario (Admin)
      │
      ├─→ Accede a /admin/empresa/editar
      │
      ├─→ EmpresaController.mostrarFormulario()
      │         │
      │         └─→ EmpresaService.obtenerConfiguracion()
      │                   │
      │                   └─→ EmpresaRepository.findById(1L)
      │                             │
      │                             └─→ Retorna Empresa → DTO
      │
      ├─→ Renderiza editar.html con datos
      │
      ├─→ Usuario modifica campos
      │
      ├─→ Submit formulario → POST /admin/empresa/actualizar
      │
      ├─→ EmpresaController.actualizar(dto)
      │         │
      │         ├─→ Validación Bean Validation
      │         │
      │         └─→ EmpresaService.actualizarConfiguracion(dto)
      │                   │
      │                   ├─→ Mapea DTO → Empresa
      │                   ├─→ Registra auditoría (modificadoPor)
      │                   └─→ EmpresaRepository.save()
      │
      └─→ Redirect con mensaje de éxito
```

### 2. Enviar Email de Prueba

```
Usuario (Admin)
      │
      ├─→ Click en "Enviar email de prueba"
      │
      ├─→ JavaScript: prompt() solicita destinatario
      │
      ├─→ fetch() → POST /admin/empresa/enviar-prueba
      │
      ├─→ EmpresaController.enviarEmailPrueba(destinatario)
      │         │
      │         └─→ EmailService.enviarEmailPrueba(destinatario)
      │                   │
      │                   ├─→ Obtiene configuración SMTP
      │                   ├─→ Crea javax.mail.Session
      │                   ├─→ Configura MimeMessage
      │                   └─→ Transport.send()
      │
      └─→ Retorna JSON { mensaje: "Email enviado" }
               │
               └─→ JavaScript: alert() muestra resultado
```

### 3. Subir Logotipo

```
Usuario (Admin)
      │
      ├─→ Selecciona archivo de imagen
      │
      ├─→ Submit form → POST /admin/empresa/logo
      │
      ├─→ EmpresaController.subirLogo(file)
      │         │
      │         └─→ EmpresaService.guardarLogo(file)
      │                   │
      │                   ├─→ Valida tipo MIME (image/*)
      │                   ├─→ Genera nombre único
      │                   ├─→ Guarda en /uploads/logos/
      │                   ├─→ Actualiza empresa.logoUrl
      │                   └─→ Save en BD
      │
      └─→ Redirect con mensaje de éxito
```

---

## ✅ TESTING

### Tests Unitarios

**Archivo:** `EmpresaServiceTest.java`

```java
@SpringBootTest
class EmpresaServiceTest {

    @Autowired
    private EmpresaService empresaService;

    @Test
    void deberiaObtenerConfiguracion() {
        EmpresaDTO empresa = empresaService.obtenerConfiguracion();
        assertNotNull(empresa);
        assertEquals(1L, empresa.getId());
    }

    @Test
    void deberiaActualizarDatos() {
        EmpresaDTO dto = empresaService.obtenerConfiguracion();
        dto.setNombre("Nueva Empresa S.L.");
        
        empresaService.actualizarConfiguracion(dto);
        
        EmpresaDTO actualizada = empresaService.obtenerConfiguracion();
        assertEquals("Nueva Empresa S.L.", actualizada.getNombre());
    }

    @Test
    void deberiaRechazarLogoInvalido() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "contenido".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> {
            empresaService.guardarLogo(file);
        });
    }
}
```

### Tests de Integración

**Archivo:** `EmpresaControllerIntegrationTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
class EmpresaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = "EMPRESA_EDITAR")
    void deberiaActualizarEmpresa() throws Exception {
        mockMvc.perform(post("/admin/empresa/actualizar")
                .param("nombre", "Test Empresa")
                .param("cif", "B12345678")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/empresa/editar"))
            .andExpect(flash().attributeExists("success"));
    }
}
```

---

## 📝 NOTAS DE IMPLEMENTACIÓN

### Decisiones Técnicas

1. **Patrón Singleton:**
   - Se usa `ID = 1` para la única configuración de empresa
   - Simplifica la lógica de negocio
   - Evita complejidad de múltiples configuraciones

2. **Encriptación de Contraseñas:**
   - ⚠️ Pendiente implementar: jasypt o similar
   - Actualmente almacenado en texto plano (usar solo en desarrollo)

3. **Validación SMTP:**
   - Email de prueba valida configuración antes de producción
   - Evita errores en envío de facturas/notificaciones

4. **Gestión de Archivos:**
   - Logotipo almacenado en `/uploads/logos/`
   - Nombre único con timestamp para evitar colisiones
   - Validación de tipo MIME en servidor

### Mejoras Futuras

- [ ] Encriptar contraseña SMTP con Jasypt
- [ ] Versionado de configuraciones (histórico de cambios)
- [ ] Preview de plantillas de email con datos de empresa
- [ ] Validación de CIF contra API de Hacienda
- [ ] Multi-tenant: soporte para múltiples empresas

### Dependencias

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
</dependency>
```

---

**FIN DEL DOCUMENTO**
