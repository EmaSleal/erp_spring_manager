package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.services.UsuarioService;
import api.astro.whats_orders_manager.services.EmailService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controlador para la gestión de Usuarios del Sistema
 * Permite CRUD completo, activar/desactivar usuarios y reset de contraseñas.
 * Solo accesible por usuarios con rol ADMIN.
 * 
 * @author Astro Dev Team
 * @version 1.0
 * @since Sprint 2 - Fase 3
 */
@Controller
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;
    
    private static final String CARACTERES_PASSWORD = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%";
    private static final int LONGITUD_PASSWORD = 12;

    /**
     * Lista todos los usuarios con paginación, filtros y búsqueda
     * 
     * GET /usuarios
     * GET /usuarios?page=0&size=10&sort=nombre&rol=ADMIN&activo=true&search=juan
     */
    @GetMapping
    public String listarUsuarios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nombre") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String search,
            Model model,
            HttpSession session) {
        
        log.info("Listando usuarios - página: {}, tamaño: {}, orden: {} {}", page, size, sort, direction);
        
        // Agregar datos del usuario logueado
        agregarDatosUsuarioAlModelo(model, session);
        
        // Obtener todos los usuarios
        List<Usuario> todosUsuarios = usuarioService.findAll();
        
        // Aplicar filtros
        List<Usuario> usuariosFiltrados = todosUsuarios.stream()
                .filter(u -> rol == null || rol.isEmpty() || u.getRol().equalsIgnoreCase(rol))
                .filter(u -> activo == null || u.getActivo().equals(activo))
                .filter(u -> search == null || search.isEmpty() || 
                        u.getNombre().toLowerCase().contains(search.toLowerCase()) ||
                        (u.getEmail() != null && u.getEmail().toLowerCase().contains(search.toLowerCase())) ||
                        (u.getTelefono() != null && u.getTelefono().contains(search)))
                .collect(Collectors.toList());
        
        // Ordenar
        Comparator<Usuario> comparator;
        switch (sort.toLowerCase()) {
            case "email":
                comparator = Comparator.comparing(Usuario::getEmail, Comparator.nullsLast(String::compareTo));
                break;
            case "rol":
                comparator = Comparator.comparing(Usuario::getRol, Comparator.nullsLast(String::compareTo));
                break;
            case "activo":
                comparator = Comparator.comparing(Usuario::getActivo, Comparator.nullsLast(Boolean::compareTo));
                break;
            case "createdate":
                comparator = Comparator.comparing(Usuario::getCreateDate, Comparator.nullsLast(Timestamp::compareTo));
                break;
            default: // nombre
                comparator = Comparator.comparing(Usuario::getNombre, Comparator.nullsLast(String::compareTo));
        }
        
        if ("desc".equalsIgnoreCase(direction)) {
            comparator = comparator.reversed();
        }
        
        usuariosFiltrados.sort(comparator);
        
        // Paginación manual
        int start = Math.min(page * size, usuariosFiltrados.size());
        int end = Math.min((page + 1) * size, usuariosFiltrados.size());
        List<Usuario> usuariosPaginados = usuariosFiltrados.subList(start, end);
        
        int totalPages = (int) Math.ceil((double) usuariosFiltrados.size() / size);
        
        // Estadísticas
        long totalUsuarios = todosUsuarios.size();
        long usuariosActivos = todosUsuarios.stream().filter(Usuario::getActivo).count();
        long usuariosInactivos = totalUsuarios - usuariosActivos;
        long admins = todosUsuarios.stream().filter(u -> "ADMIN".equalsIgnoreCase(u.getRol())).count();
        long users = todosUsuarios.stream().filter(u -> "USER".equalsIgnoreCase(u.getRol())).count();
        
        // Agregar al modelo
        model.addAttribute("usuarios", usuariosPaginados);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalUsuarios", usuariosFiltrados.size());
        model.addAttribute("sortField", sort);
        model.addAttribute("sortDir", direction);
        
        // Filtros aplicados
        model.addAttribute("rolFiltro", rol);
        model.addAttribute("activoFiltro", activo);
        model.addAttribute("searchFiltro", search);
        
        // Estadísticas
        model.addAttribute("totalUsuariosGlobal", totalUsuarios);
        model.addAttribute("usuariosActivos", usuariosActivos);
        model.addAttribute("usuariosInactivos", usuariosInactivos);
        model.addAttribute("totalAdmins", admins);
        model.addAttribute("totalUsers", users);
        
        log.info("Mostrando {} usuarios de un total de {}", usuariosPaginados.size(), usuariosFiltrados.size());
        
        return "usuarios/usuarios";
    }

    /**
     * Muestra formulario para crear nuevo usuario
     * 
     * GET /usuarios/form
     */
    @GetMapping("/form")
    public String mostrarFormularioNuevo(Model model, HttpSession session) {
        log.info("Mostrando formulario para nuevo usuario");
        
        agregarDatosUsuarioAlModelo(model, session);
        
        Usuario usuario = new Usuario();
        usuario.setActivo(true); // Activo por defecto
        usuario.setRol("USER"); // Rol USER por defecto
        
        model.addAttribute("usuario", usuario);
        model.addAttribute("esNuevo", true);
        model.addAttribute("titulo", "Nuevo Usuario");
        
        return "usuarios/form";
    }

    /**
     * Muestra formulario para editar usuario existente
     * 
     * GET /usuarios/form/{id}
     */
    @GetMapping("/form/{id}")
    public String mostrarFormularioEditar(@PathVariable Integer id, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        log.info("Mostrando formulario para editar usuario ID: {}", id);
        
        agregarDatosUsuarioAlModelo(model, session);
        
        Optional<Usuario> usuarioOpt = usuarioService.findById(id);
        
        if (usuarioOpt.isEmpty()) {
            log.error("Usuario no encontrado con ID: {}", id);
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
            return "redirect:/usuarios";
        }
        
        Usuario usuario = usuarioOpt.get();
        
        // No enviar la contraseña al formulario
        usuario.setPassword(null);
        
        model.addAttribute("usuario", usuario);
        model.addAttribute("esNuevo", false);
        model.addAttribute("titulo", "Editar Usuario");
        
        return "usuarios/form";
    }

    /**
     * Guarda un nuevo usuario o actualiza uno existente
     * 
     * POST /usuarios/save
     */
    @PostMapping("/save")
    public String guardarUsuario(
            @Valid @ModelAttribute("usuario") Usuario usuario,
            BindingResult result,
            @RequestParam(required = false) String passwordNueva,
            @RequestParam(required = false) String passwordConfirmacion,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        log.info("Guardando usuario: {}", usuario.getNombre());
        
        boolean esNuevo = (usuario.getIdUsuario() == null || usuario.getIdUsuario() == 0);
        
        // Validaciones
        if (result.hasErrors()) {
            log.error("Errores de validación al guardar usuario");
            model.addAttribute("esNuevo", esNuevo);
            model.addAttribute("titulo", esNuevo ? "Nuevo Usuario" : "Editar Usuario");
            agregarDatosUsuarioAlModelo(model, session);
            return "usuarios/form";
        }
        
        try {
            // Validar teléfono único
            Optional<Usuario> usuarioExistenteTelefono = usuarioService.findByTelefono(usuario.getTelefono());
            if (usuarioExistenteTelefono.isPresent() && 
                !usuarioExistenteTelefono.get().getIdUsuario().equals(usuario.getIdUsuario())) {
                log.error("El teléfono {} ya está registrado", usuario.getTelefono());
                redirectAttributes.addFlashAttribute("error", "El teléfono ya está registrado por otro usuario");
                return "redirect:/usuarios/form" + (esNuevo ? "" : "/" + usuario.getIdUsuario());
            }
            
            // Validar email único
            if (usuario.getEmail() != null && !usuario.getEmail().isEmpty()) {
                Optional<Usuario> usuarioExistenteEmail = usuarioService.findByEmail(usuario.getEmail());
                if (usuarioExistenteEmail.isPresent() && 
                    !usuarioExistenteEmail.get().getIdUsuario().equals(usuario.getIdUsuario())) {
                    log.error("El email {} ya está registrado", usuario.getEmail());
                    redirectAttributes.addFlashAttribute("error", "El email ya está registrado por otro usuario");
                    return "redirect:/usuarios/form" + (esNuevo ? "" : "/" + usuario.getIdUsuario());
                }
            }
            
            if (esNuevo) {
                // Nuevo usuario - contraseña obligatoria
                if (passwordNueva == null || passwordNueva.isEmpty()) {
                    log.error("Contraseña requerida para nuevo usuario");
                    redirectAttributes.addFlashAttribute("error", "La contraseña es requerida para nuevos usuarios");
                    return "redirect:/usuarios/form";
                }
                
                if (!passwordNueva.equals(passwordConfirmacion)) {
                    log.error("Las contraseñas no coinciden");
                    redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden");
                    return "redirect:/usuarios/form";
                }
                
                // Encriptar contraseña
                usuario.setPassword(passwordEncoder.encode(passwordNueva));
                
                Usuario guardado = usuarioService.save(usuario);
                log.info("Usuario creado exitosamente con ID: {}", guardado.getIdUsuario());
                
                // Enviar credenciales por email si el usuario tiene email configurado
                if (guardado.getEmail() != null && !guardado.getEmail().isBlank()) {
                    try {
                        String urlLogin = "http://localhost:8080/auth/login"; // TODO: Obtener de configuración
                        emailService.enviarCredencialesUsuario(guardado, passwordNueva, urlLogin);
                        log.info("📧 Credenciales enviadas por email a: {}", guardado.getEmail());
                        redirectAttributes.addFlashAttribute("success", 
                            "Usuario creado exitosamente. Se han enviado las credenciales por email.");
                    } catch (MessagingException e) {
                        log.error("❌ Error al enviar credenciales por email: {}", e.getMessage());
                        redirectAttributes.addFlashAttribute("warning", 
                            "Usuario creado exitosamente, pero no se pudieron enviar las credenciales por email.");
                    }
                } else {
                    redirectAttributes.addFlashAttribute("success", "Usuario creado exitosamente");
                }
                
            } else {
                // Actualizar usuario existente
                Usuario usuarioExistente = usuarioService.findById(usuario.getIdUsuario())
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
                
                // Actualizar campos
                usuarioExistente.setNombre(usuario.getNombre());
                usuarioExistente.setTelefono(usuario.getTelefono());
                usuarioExistente.setEmail(usuario.getEmail());
                usuarioExistente.setRol(usuario.getRol());
                usuarioExistente.setActivo(usuario.getActivo());
                
                // Actualizar contraseña solo si se proporcionó una nueva
                if (passwordNueva != null && !passwordNueva.isEmpty()) {
                    if (!passwordNueva.equals(passwordConfirmacion)) {
                        log.error("Las contraseñas no coinciden");
                        redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden");
                        return "redirect:/usuarios/form/" + usuario.getIdUsuario();
                    }
                    usuarioExistente.setPassword(passwordEncoder.encode(passwordNueva));
                }
                
                usuarioService.save(usuarioExistente);
                log.info("Usuario actualizado exitosamente: ID {}", usuarioExistente.getIdUsuario());
                redirectAttributes.addFlashAttribute("success", "Usuario actualizado exitosamente");
            }
            
        } catch (Exception e) {
            log.error("Error al guardar usuario", e);
            redirectAttributes.addFlashAttribute("error", "Error al guardar el usuario: " + e.getMessage());
            return "redirect:/usuarios/form" + (esNuevo ? "" : "/" + usuario.getIdUsuario());
        }
        
        return "redirect:/usuarios";
    }

    /**
     * Elimina un usuario
     * 
     * DELETE /usuarios/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarUsuario(@PathVariable Integer id, HttpSession session) {
        log.info("Eliminando usuario ID: {}", id);
        
        try {
            // Verificar que el usuario no se esté eliminando a sí mismo
            Usuario usuarioLogueado = (Usuario) session.getAttribute("usuario");
            if (usuarioLogueado != null && usuarioLogueado.getIdUsuario().equals(id)) {
                log.error("Un usuario no puede eliminarse a sí mismo");
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No puedes eliminar tu propia cuenta"
                ));
            }
            
            Optional<Usuario> usuarioOpt = usuarioService.findById(id);
            if (usuarioOpt.isEmpty()) {
                log.error("Usuario no encontrado con ID: {}", id);
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Usuario no encontrado"
                ));
            }
            
            usuarioService.deleteById(id);
            log.info("Usuario eliminado exitosamente: ID {}", id);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Usuario eliminado exitosamente"
            ));
            
        } catch (Exception e) {
            log.error("Error al eliminar usuario", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al eliminar el usuario: " + e.getMessage()
            ));
        }
    }

    /**
     * Activa o desactiva un usuario
     * 
     * POST /usuarios/toggle-active/{id}
     */
    @PostMapping("/toggle-active/{id}")
    @ResponseBody
    public ResponseEntity<?> toggleActivo(@PathVariable Integer id, HttpSession session) {
        log.info("Cambiando estado activo del usuario ID: {}", id);
        
        try {
            // Verificar que el usuario no se esté desactivando a sí mismo
            Usuario usuarioLogueado = (Usuario) session.getAttribute("usuario");
            if (usuarioLogueado != null && usuarioLogueado.getIdUsuario().equals(id)) {
                log.error("Un usuario no puede desactivarse a sí mismo");
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No puedes desactivar tu propia cuenta"
                ));
            }
            
            Usuario usuario = usuarioService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            
            boolean nuevoEstado = !usuario.getActivo();
            usuario.setActivo(nuevoEstado);
            usuarioService.save(usuario);
            
            log.info("Usuario {} {}", id, nuevoEstado ? "activado" : "desactivado");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "activo", nuevoEstado,
                "message", "Usuario " + (nuevoEstado ? "activado" : "desactivado") + " exitosamente"
            ));
            
        } catch (Exception e) {
            log.error("Error al cambiar estado del usuario", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al cambiar el estado: " + e.getMessage()
            ));
        }
    }

    /**
     * Resetea la contraseña de un usuario y genera una nueva aleatoria
     * 
     * POST /usuarios/reset-password/{id}
     */
    @PostMapping("/reset-password/{id}")
    @ResponseBody
    public ResponseEntity<?> resetPassword(@PathVariable Integer id) {
        log.info("Reseteando contraseña del usuario ID: {}", id);
        
        try {
            Usuario usuario = usuarioService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            
            // Generar contraseña aleatoria
            String nuevaPassword = generarPasswordAleatoria();
            
            // Encriptar y guardar
            usuario.setPassword(passwordEncoder.encode(nuevaPassword));
            usuarioService.save(usuario);
            
            log.info("Contraseña reseteada exitosamente para usuario ID: {}", id);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "password", nuevaPassword,
                "message", "Contraseña reseteada exitosamente"
            ));
            
        } catch (Exception e) {
            log.error("Error al resetear contraseña", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al resetear la contraseña: " + e.getMessage()
            ));
        }
    }

    /**
     * Genera una contraseña aleatoria segura
     * 
     * GET /usuarios/generar-password (AJAX)
     */
    @GetMapping("/generar-password")
    @ResponseBody
    public ResponseEntity<?> generarPassword() {
        String password = generarPasswordAleatoria();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "password", password
        ));
    }

    /**
     * Reenvía las credenciales de un usuario por email
     * Genera una nueva contraseña temporal y la envía por email
     * 
     * POST /usuarios/{id}/reenviar-credenciales
     */
    @PostMapping("/{id}/reenviar-credenciales")
    @ResponseBody
    public ResponseEntity<?> reenviarCredenciales(@PathVariable Integer id) {
        log.info("Reenviando credenciales del usuario ID: {}", id);
        
        try {
            Usuario usuario = usuarioService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            
            // Validar que el usuario tenga email
            if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
                log.error("El usuario {} no tiene email configurado", usuario.getNombre());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "El usuario no tiene email configurado"
                ));
            }
            
            // Generar nueva contraseña temporal
            String nuevaPassword = generarPasswordAleatoria();
            
            // Encriptar y guardar
            usuario.setPassword(passwordEncoder.encode(nuevaPassword));
            usuarioService.save(usuario);
            
            // Enviar credenciales por email
            String urlLogin = "http://localhost:8080/auth/login"; // TODO: Obtener de configuración
            emailService.enviarCredencialesUsuario(usuario, nuevaPassword, urlLogin);
            
            log.info("✅ Credenciales reenviadas exitosamente a: {}", usuario.getEmail());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Credenciales enviadas exitosamente a " + usuario.getEmail()
            ));
            
        } catch (MessagingException e) {
            log.error("❌ Error al enviar credenciales por email: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al enviar el email: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("❌ Error al reenviar credenciales", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al reenviar credenciales: " + e.getMessage()
            ));
        }
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Genera una contraseña aleatoria segura
     */
    private String generarPasswordAleatoria() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(LONGITUD_PASSWORD);
        
        for (int i = 0; i < LONGITUD_PASSWORD; i++) {
            int index = random.nextInt(CARACTERES_PASSWORD.length());
            password.append(CARACTERES_PASSWORD.charAt(index));
        }
        
        return password.toString();
    }

    /**
     * Agrega los datos del usuario logueado al modelo
     */
    private void agregarDatosUsuarioAlModelo(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        
        if (usuario != null) {
            model.addAttribute("userName", usuario.getNombre());
            model.addAttribute("userRole", usuario.getRol());
            
            // Generar iniciales para avatar
            String[] nombres = usuario.getNombre().split(" ");
            String iniciales = nombres.length >= 2 
                ? (nombres[0].substring(0, 1) + nombres[1].substring(0, 1)).toUpperCase()
                : usuario.getNombre().substring(0, Math.min(2, usuario.getNombre().length())).toUpperCase();
            
            model.addAttribute("userInitials", iniciales);
            
            // Avatar si existe
            if (usuario.getAvatar() != null && !usuario.getAvatar().isEmpty()) {
                model.addAttribute("userAvatar", usuario.getAvatar());
            }
        }
    }
}
