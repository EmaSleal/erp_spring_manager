package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.services.UsuarioService;
import api.astro.whats_orders_manager.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

/**
 * ============================================================================
 * PERFIL CONTROLLER
 * WhatsApp Orders Manager
 * ============================================================================
 * Controlador para gestionar el perfil de usuario.
 * 
 * Endpoints:
 * - GET  /perfil                    → Ver perfil del usuario autenticado
 * - GET  /perfil/editar             → Mostrar formulario de edición
 * - POST /perfil/actualizar         → Actualizar datos del perfil
 * - POST /perfil/cambiar-password   → Cambiar contraseña
 * - POST /perfil/subir-avatar       → Subir imagen de perfil
 * - POST /perfil/eliminar-avatar    → Eliminar avatar
 * 
 * @version 2.1 - Aplicado StringUtil
 * @since 26/10/2025
 * ============================================================================
 */
@Controller
@RequestMapping("/perfil")
@RequiredArgsConstructor
@Slf4j
public class PerfilController {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;

    // Directorio donde se guardan los avatares
    private static final String UPLOAD_DIR = "src/main/resources/static/images/avatars/";
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final int MIN_PASSWORD_LENGTH = 6;

    /**
     * Ver perfil del usuario autenticado
     * GET /perfil
     */
    @GetMapping
    public String verPerfil(Model model, Authentication authentication) {
        log.info("Solicitud de visualización de perfil");
        
        try {
            Usuario usuario = obtenerUsuarioActual(authentication);
            
            if (usuario == null) {
                log.warn("Usuario no encontrado en autenticación");
                return "redirect:/auth/login";
            }

            agregarDatosUsuarioAlModelo(model, usuario);
            
            log.info("Usuario {} visualizó su perfil", usuario.getTelefono());
            return "perfil/ver";
            
        } catch (Exception e) {
            log.error("Error al cargar perfil: {}", e.getMessage(), e);
            return "redirect:/auth/login";
        }
    }

    /**
     * Mostrar formulario de edición de perfil
     * GET /perfil/editar
     */
    @GetMapping("/editar")
    public String editarPerfil(Model model, Authentication authentication) {
        log.info("Solicitud de edición de perfil");
        
        try {
            Usuario usuario = obtenerUsuarioActual(authentication);
            
            if (usuario == null) {
                log.warn("Usuario no encontrado en autenticación");
                return "redirect:/auth/login";
            }

            agregarDatosUsuarioAlModelo(model, usuario);
            
            log.info("Usuario {} accedió al formulario de edición de perfil", usuario.getTelefono());
            return "perfil/editar";
            
        } catch (Exception e) {
            log.error("Error al cargar formulario de edición: {}", e.getMessage(), e);
            return "redirect:/perfil";
        }
    }

    /**
     * Actualizar datos del perfil (nombre, email, teléfono)
     * POST /perfil/actualizar
     */
    @PostMapping("/actualizar")
    public String actualizarPerfil(
            @RequestParam String nombre,
            @RequestParam(required = false) String email,
            @RequestParam String telefono,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        log.info("Actualizando perfil de usuario");
        
        try {
            Usuario usuario = obtenerUsuarioActual(authentication);
            
            if (usuario == null) {
                log.warn("Usuario no encontrado al actualizar perfil");
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/auth/login";
            }

            // Validación básica de nombre
            if (!validarNombre(nombre, redirectAttributes)) {
                return "redirect:/perfil/editar";
            }

            // Validación de email
            if (!validarEmail(email, usuario, redirectAttributes)) {
                return "redirect:/perfil/editar";
            }

            // Validación de teléfono único
            if (!validarTelefonoUnico(telefono, usuario, redirectAttributes)) {
                return "redirect:/perfil/editar";
            }

            // Actualizar datos (las fechas se manejan automáticamente)
            usuario.setNombre(nombre.trim());
            usuario.setEmail(email != null && !email.trim().isEmpty() ? email.trim() : null);
            usuario.setTelefono(telefono.trim());
            
            usuarioService.save(usuario);
            
            log.info("✅ Perfil actualizado exitosamente para usuario: {}", usuario.getTelefono());
            redirectAttributes.addFlashAttribute("success", "Perfil actualizado correctamente");
            
        } catch (Exception e) {
            log.error("Error al actualizar perfil: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al actualizar el perfil. Por favor intente nuevamente.");
            return "redirect:/perfil/editar";
        }
        
        return "redirect:/perfil";
    }

    /**
     * Cambiar contraseña del usuario
     * POST /perfil/cambiar-password
     */
    @PostMapping("/cambiar-password")
    public String cambiarPassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        log.info("Solicitud de cambio de contraseña");
        
        try {
            Usuario usuario = obtenerUsuarioActual(authentication);
            
            if (usuario == null) {
                log.warn("Usuario no encontrado al cambiar contraseña");
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/auth/login";
            }

            // Validar contraseña actual
            if (!passwordEncoder.matches(currentPassword, usuario.getPassword())) {
                log.warn("Intento de cambio de contraseña con contraseña actual incorrecta para usuario: {}", 
                        usuario.getTelefono());
                redirectAttributes.addFlashAttribute("errorPassword", 
                    "La contraseña actual es incorrecta");
                return "redirect:/perfil/editar";
            }

            // Validar que las contraseñas nuevas coincidan
            if (!newPassword.equals(confirmPassword)) {
                log.debug("Las contraseñas nuevas no coinciden");
                redirectAttributes.addFlashAttribute("errorPassword", 
                    "Las contraseñas nuevas no coinciden");
                return "redirect:/perfil/editar";
            }

            // Validar longitud mínima
            if (newPassword.length() < MIN_PASSWORD_LENGTH) {
                log.debug("Contraseña demasiado corta: {} caracteres", newPassword.length());
                redirectAttributes.addFlashAttribute("errorPassword", 
                    "La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
                return "redirect:/perfil/editar";
            }

            // Actualizar contraseña (las fechas se manejan automáticamente)
            usuario.setPassword(passwordEncoder.encode(newPassword));
            usuarioService.save(usuario);
            
            log.info("✅ Contraseña actualizada exitosamente para usuario: {}", usuario.getTelefono());
            redirectAttributes.addFlashAttribute("success", "Contraseña actualizada correctamente");
            
        } catch (Exception e) {
            log.error("Error al cambiar contraseña: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorPassword", 
                "Error al cambiar la contraseña. Por favor intente nuevamente.");
            return "redirect:/perfil/editar";
        }
        
        return "redirect:/perfil";
    }

    /**
     * Subir imagen de avatar
     * POST /perfil/subir-avatar
     */
    @PostMapping("/subir-avatar")
    public String subirAvatar(
            @RequestParam("avatar") MultipartFile file,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        log.info("Solicitud de subida de avatar");
        
        try {
            Usuario usuario = obtenerUsuarioActual(authentication);
            
            if (usuario == null) {
                log.warn("Usuario no encontrado al subir avatar");
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/auth/login";
            }

            // Validar archivo
            if (!validarArchivoImagen(file, redirectAttributes)) {
                return "redirect:/perfil/editar";
            }

            // Generar nombre único para el archivo
            String fileName = generarNombreArchivoUnico(usuario, file);

            // Crear directorio si no existe y guardar archivo
            Path uploadPath = Paths.get(UPLOAD_DIR);
            crearDirectorioSiNoExiste(uploadPath);

            // Guardar archivo
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Eliminar avatar anterior si existe
            eliminarAvatarAnterior(usuario, uploadPath);

            // Actualizar ruta del avatar en el usuario (fechas automáticas)
            String avatarUrl = "/images/avatars/" + fileName;
            usuario.setAvatar(avatarUrl);
            usuarioService.save(usuario);

            log.info("✅ Avatar actualizado para usuario: {} → {}", usuario.getTelefono(), avatarUrl);
            redirectAttributes.addFlashAttribute("success", "Avatar actualizado correctamente");
            
        } catch (IOException e) {
            log.error("Error de I/O al subir avatar: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorAvatar", 
                "Error al subir la imagen. Por favor intente nuevamente.");
            return "redirect:/perfil/editar";
        } catch (Exception e) {
            log.error("Error inesperado al subir avatar: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorAvatar", 
                "Error inesperado al subir la imagen.");
            return "redirect:/perfil/editar";
        }
        
        return "redirect:/perfil";
    }

    /**
     * Eliminar avatar (volver a usar iniciales)
     * POST /perfil/eliminar-avatar
     */
    @PostMapping("/eliminar-avatar")
    public String eliminarAvatar(
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        log.info("Solicitud de eliminación de avatar");
        
        try {
            Usuario usuario = obtenerUsuarioActual(authentication);
            
            if (usuario == null) {
                log.warn("Usuario no encontrado al eliminar avatar");
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/auth/login";
            }

            // Eliminar archivo físico si existe
            eliminarAvatarAnterior(usuario, Paths.get(UPLOAD_DIR));

            // Actualizar usuario (fechas automáticas)
            usuario.setAvatar(null);
            usuarioService.save(usuario);

            log.info("✅ Avatar eliminado para usuario: {}", usuario.getTelefono());
            redirectAttributes.addFlashAttribute("success", "Avatar eliminado correctamente");
            
        } catch (Exception e) {
            log.error("Error al eliminar avatar: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al eliminar el avatar. Por favor intente nuevamente.");
        }
        
        return "redirect:/perfil/editar";
    }

    // ============================================================================
    // MÉTODOS AUXILIARES - AUTENTICACIÓN Y MODELO
    // ============================================================================

    /**
     * Obtener el usuario actual desde la autenticación
     */
    private Usuario obtenerUsuarioActual(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Autenticación nula o no autenticada");
            return null;
        }

        Object principal = authentication.getPrincipal();
        String telefono;

        if (principal instanceof UserDetails) {
            telefono = ((UserDetails) principal).getUsername();
        } else {
            telefono = principal.toString();
        }

        Optional<Usuario> usuarioOpt = usuarioService.findByTelefono(telefono);
        if (usuarioOpt.isEmpty()) {
            log.warn("Usuario no encontrado con teléfono: {}", telefono);
        }
        return usuarioOpt.orElse(null);
    }

    /**
     * Agrega los datos del usuario al modelo (usuario e iniciales)
     */
    private void agregarDatosUsuarioAlModelo(Model model, Usuario usuario) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("iniciales", StringUtil.generarIniciales(usuario.getNombre()));
    }

    // ============================================================================
    // MÉTODOS AUXILIARES - VALIDACIONES
    // ============================================================================

    /**
     * Validar que el nombre no esté vacío
     */
    private boolean validarNombre(String nombre, RedirectAttributes redirectAttributes) {
        if (nombre == null || nombre.trim().isEmpty()) {
            log.debug("Validación fallida: nombre vacío");
            redirectAttributes.addFlashAttribute("error", "El nombre es obligatorio");
            return false;
        }
        return true;
    }

    /**
     * Validar email (formato y unicidad)
     */
    private boolean validarEmail(String email, Usuario usuario, RedirectAttributes redirectAttributes) {
        if (email != null && !email.trim().isEmpty()) {
            if (!isValidEmail(email)) {
                log.debug("Validación fallida: formato de email inválido");
                redirectAttributes.addFlashAttribute("error", "El formato del email no es válido");
                return false;
            }
            
            // Verificar que el email no esté en uso por otro usuario
            Optional<Usuario> usuarioConEmailOpt = usuarioService.findByEmail(email);
            if (usuarioConEmailOpt.isPresent()) {
                Usuario usuarioConEmail = usuarioConEmailOpt.get();
                if (!usuarioConEmail.getIdUsuario().equals(usuario.getIdUsuario())) {
                    log.debug("Validación fallida: email ya en uso");
                    redirectAttributes.addFlashAttribute("error", 
                        "El email ya está en uso por otro usuario");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Validar que el teléfono sea único
     */
    private boolean validarTelefonoUnico(String telefono, Usuario usuario, RedirectAttributes redirectAttributes) {
        Optional<Usuario> usuarioConTelefonoOpt = usuarioService.findByTelefono(telefono);
        if (usuarioConTelefonoOpt.isPresent()) {
            Usuario usuarioConTelefono = usuarioConTelefonoOpt.get();
            if (!usuarioConTelefono.getIdUsuario().equals(usuario.getIdUsuario())) {
                log.debug("Validación fallida: teléfono ya en uso");
                redirectAttributes.addFlashAttribute("error", 
                    "El teléfono ya está en uso por otro usuario");
                return false;
            }
        }
        return true;
    }

    /**
     * Validar formato de email
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * Validar archivo de imagen (tipo y tamaño)
     */
    private boolean validarArchivoImagen(MultipartFile file, RedirectAttributes redirectAttributes) {
        // Validar que se haya seleccionado un archivo
        if (file.isEmpty()) {
            log.debug("Validación fallida: archivo vacío");
            redirectAttributes.addFlashAttribute("errorAvatar", "Por favor seleccione una imagen");
            return false;
        }

        // Validar tipo de archivo
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            log.debug("Validación fallida: tipo de archivo no es imagen");
            redirectAttributes.addFlashAttribute("errorAvatar", "Solo se permiten archivos de imagen");
            return false;
        }

        // Validar tamaño
        if (file.getSize() > MAX_FILE_SIZE) {
            log.debug("Validación fallida: archivo demasiado grande ({} bytes)", file.getSize());
            redirectAttributes.addFlashAttribute("errorAvatar", "La imagen no puede superar los 2MB");
            return false;
        }

        return true;
    }

    // ============================================================================
    // MÉTODOS AUXILIARES - GESTIÓN DE ARCHIVOS
    // ============================================================================

    /**
     * Generar nombre único para archivo de avatar
     */
    private String generarNombreArchivoUnico(Usuario usuario, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".jpg";
        return "avatar_" + usuario.getIdUsuario() + "_" + UUID.randomUUID().toString() + extension;
    }

    /**
     * Crear directorio si no existe
     */
    private void crearDirectorioSiNoExiste(Path directorio) throws IOException {
        if (!Files.exists(directorio)) {
            Files.createDirectories(directorio);
            log.info("Directorio creado: {}", directorio);
        }
    }

    /**
     * Eliminar avatar anterior del usuario
     */
    private void eliminarAvatarAnterior(Usuario usuario, Path uploadPath) {
        if (usuario.getAvatar() != null && !usuario.getAvatar().isEmpty()) {
            try {
                String oldFileName = usuario.getAvatar().substring(usuario.getAvatar().lastIndexOf("/") + 1);
                Path oldFilePath = uploadPath.resolve(oldFileName);
                Files.deleteIfExists(oldFilePath);
                log.debug("Avatar anterior eliminado: {}", oldFileName);
            } catch (Exception e) {
                log.warn("No se pudo eliminar el avatar anterior: {}", e.getMessage());
            }
        }
    }
}
