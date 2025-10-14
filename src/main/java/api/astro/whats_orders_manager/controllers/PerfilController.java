package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.services.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
 * - GET  /perfil          → Ver perfil del usuario autenticado
 * - GET  /perfil/editar   → Mostrar formulario de edición
 * - POST /perfil/actualizar → Actualizar datos del perfil
 * - POST /perfil/cambiar-password → Cambiar contraseña
 * - POST /perfil/subir-avatar → Subir imagen de perfil
 * ============================================================================
 */
@Controller
@RequestMapping("/perfil")
@Slf4j
public class PerfilController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Directorio donde se guardan los avatares
    private static final String UPLOAD_DIR = "src/main/resources/static/images/avatars/";

    /**
     * Ver perfil del usuario autenticado
     * GET /perfil
     */
    @GetMapping
    public String verPerfil(Model model, Authentication authentication) {
        Usuario usuario = obtenerUsuarioActual(authentication);
        
        if (usuario == null) {
            log.warn("Usuario no encontrado en autenticación");
            return "redirect:/auth/login";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("iniciales", generarIniciales(usuario.getNombre()));
        
        log.info("Usuario {} visualizó su perfil", usuario.getTelefono());
        return "perfil/ver";
    }

    /**
     * Mostrar formulario de edición de perfil
     * GET /perfil/editar
     */
    @GetMapping("/editar")
    public String editarPerfil(Model model, Authentication authentication) {
        Usuario usuario = obtenerUsuarioActual(authentication);
        
        if (usuario == null) {
            log.warn("Usuario no encontrado en autenticación");
            return "redirect:/auth/login";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("iniciales", generarIniciales(usuario.getNombre()));
        
        log.info("Usuario {} accedió al formulario de edición de perfil", usuario.getTelefono());
        return "perfil/editar";
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
            RedirectAttributes redirectAttributes) {
        
        try {
            Usuario usuario = obtenerUsuarioActual(authentication);
            
            if (usuario == null) {
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/auth/login";
            }

            // Validar que el nombre no esté vacío
            if (nombre == null || nombre.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "El nombre es obligatorio");
                return "redirect:/perfil/editar";
            }

            // Validar email si se proporciona
            if (email != null && !email.trim().isEmpty()) {
                if (!isValidEmail(email)) {
                    redirectAttributes.addFlashAttribute("error", "El formato del email no es válido");
                    return "redirect:/perfil/editar";
                }
                
                // Verificar que el email no esté en uso por otro usuario
                Optional<Usuario> usuarioConEmailOpt = usuarioService.findByEmail(email);
                if (usuarioConEmailOpt.isPresent()) {
                    Usuario usuarioConEmail = usuarioConEmailOpt.get();
                    if (!usuarioConEmail.getIdUsuario().equals(usuario.getIdUsuario())) {
                        redirectAttributes.addFlashAttribute("error", "El email ya está en uso por otro usuario");
                        return "redirect:/perfil/editar";
                    }
                }
            }

            // Validar teléfono único
            Optional<Usuario> usuarioConTelefonoOpt = usuarioService.findByTelefono(telefono);
            if (usuarioConTelefonoOpt.isPresent()) {
                Usuario usuarioConTelefono = usuarioConTelefonoOpt.get();
                if (!usuarioConTelefono.getIdUsuario().equals(usuario.getIdUsuario())) {
                    redirectAttributes.addFlashAttribute("error", "El teléfono ya está en uso por otro usuario");
                    return "redirect:/perfil/editar";
                }
            }

            // Actualizar datos
            usuario.setNombre(nombre.trim());
            usuario.setEmail(email != null && !email.trim().isEmpty() ? email.trim() : null);
            usuario.setTelefono(telefono.trim());
            usuario.setUpdateDate(Timestamp.valueOf(LocalDateTime.now()));
            
            usuarioService.save(usuario);
            
            log.info("Perfil actualizado para usuario: {}", usuario.getTelefono());
            redirectAttributes.addFlashAttribute("success", "Perfil actualizado correctamente");
            
        } catch (Exception e) {
            log.error("Error al actualizar perfil: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al actualizar el perfil: " + e.getMessage());
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
            RedirectAttributes redirectAttributes) {
        
        try {
            Usuario usuario = obtenerUsuarioActual(authentication);
            
            if (usuario == null) {
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/auth/login";
            }

            // Validar contraseña actual
            if (!passwordEncoder.matches(currentPassword, usuario.getPassword())) {
                log.warn("Intento de cambio de contraseña con contraseña actual incorrecta para usuario: {}", 
                        usuario.getTelefono());
                redirectAttributes.addFlashAttribute("errorPassword", "La contraseña actual es incorrecta");
                return "redirect:/perfil/editar";
            }

            // Validar que las contraseñas nuevas coincidan
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("errorPassword", "Las contraseñas nuevas no coinciden");
                return "redirect:/perfil/editar";
            }

            // Validar longitud mínima
            if (newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("errorPassword", "La contraseña debe tener al menos 6 caracteres");
                return "redirect:/perfil/editar";
            }

            // Actualizar contraseña
            usuario.setPassword(passwordEncoder.encode(newPassword));
            usuario.setUpdateDate(Timestamp.valueOf(LocalDateTime.now()));
            usuarioService.save(usuario);
            
            log.info("Contraseña actualizada para usuario: {}", usuario.getTelefono());
            redirectAttributes.addFlashAttribute("success", "Contraseña actualizada correctamente");
            
        } catch (Exception e) {
            log.error("Error al cambiar contraseña: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorPassword", "Error al cambiar la contraseña: " + e.getMessage());
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
            RedirectAttributes redirectAttributes) {
        
        try {
            Usuario usuario = obtenerUsuarioActual(authentication);
            
            if (usuario == null) {
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/auth/login";
            }

            // Validar que se haya seleccionado un archivo
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorAvatar", "Por favor seleccione una imagen");
                return "redirect:/perfil/editar";
            }

            // Validar tipo de archivo
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                redirectAttributes.addFlashAttribute("errorAvatar", "Solo se permiten archivos de imagen");
                return "redirect:/perfil/editar";
            }

            // Validar tamaño (máximo 2MB)
            if (file.getSize() > 2 * 1024 * 1024) {
                redirectAttributes.addFlashAttribute("errorAvatar", "La imagen no puede superar los 2MB");
                return "redirect:/perfil/editar";
            }

            // Generar nombre único para el archivo
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                    : ".jpg";
            String fileName = "avatar_" + usuario.getIdUsuario() + "_" + UUID.randomUUID().toString() + extension;

            // Crear directorio si no existe
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Guardar archivo
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Eliminar avatar anterior si existe
            if (usuario.getAvatar() != null && !usuario.getAvatar().isEmpty()) {
                try {
                    String oldFileName = usuario.getAvatar().substring(usuario.getAvatar().lastIndexOf("/") + 1);
                    Path oldFilePath = uploadPath.resolve(oldFileName);
                    Files.deleteIfExists(oldFilePath);
                } catch (Exception e) {
                    log.warn("No se pudo eliminar el avatar anterior: {}", e.getMessage());
                }
            }

            // Actualizar ruta del avatar en el usuario
            String avatarUrl = "/images/avatars/" + fileName;
            usuario.setAvatar(avatarUrl);
            usuario.setUpdateDate(Timestamp.valueOf(LocalDateTime.now()));
            usuarioService.save(usuario);

            log.info("Avatar actualizado para usuario: {} → {}", usuario.getTelefono(), avatarUrl);
            redirectAttributes.addFlashAttribute("success", "Avatar actualizado correctamente");
            
        } catch (IOException e) {
            log.error("Error al subir avatar: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorAvatar", "Error al subir la imagen: " + e.getMessage());
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
            RedirectAttributes redirectAttributes) {
        
        try {
            Usuario usuario = obtenerUsuarioActual(authentication);
            
            if (usuario == null) {
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/auth/login";
            }

            // Eliminar archivo físico si existe
            if (usuario.getAvatar() != null && !usuario.getAvatar().isEmpty()) {
                try {
                    String fileName = usuario.getAvatar().substring(usuario.getAvatar().lastIndexOf("/") + 1);
                    Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName);
                    Files.deleteIfExists(filePath);
                } catch (Exception e) {
                    log.warn("No se pudo eliminar el archivo de avatar: {}", e.getMessage());
                }
            }

            // Actualizar usuario
            usuario.setAvatar(null);
            usuario.setUpdateDate(Timestamp.valueOf(LocalDateTime.now()));
            usuarioService.save(usuario);

            log.info("Avatar eliminado para usuario: {}", usuario.getTelefono());
            redirectAttributes.addFlashAttribute("success", "Avatar eliminado correctamente");
            
        } catch (Exception e) {
            log.error("Error al eliminar avatar: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el avatar: " + e.getMessage());
        }
        
        return "redirect:/perfil/editar";
    }

    // ============================================================================
    // MÉTODOS AUXILIARES
    // ============================================================================

    /**
     * Obtener el usuario actual desde la autenticación
     */
    private Usuario obtenerUsuarioActual(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
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
        return usuarioOpt.orElse(null);
    }

    /**
     * Generar iniciales del nombre para el avatar
     */
    private String generarIniciales(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "??";
        }

        String[] partes = nombre.trim().split("\\s+");
        
        if (partes.length >= 2) {
            return String.valueOf(partes[0].charAt(0)) + partes[1].charAt(0);
        }
        
        return nombre.substring(0, Math.min(2, nombre.length())).toUpperCase();
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
}
