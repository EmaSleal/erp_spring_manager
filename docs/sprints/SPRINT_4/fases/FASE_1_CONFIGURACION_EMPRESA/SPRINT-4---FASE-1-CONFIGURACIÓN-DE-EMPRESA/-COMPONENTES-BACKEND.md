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

