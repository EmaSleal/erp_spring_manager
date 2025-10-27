package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.dto.EstadisticasUsuariosDTO;
import api.astro.whats_orders_manager.dto.PaginacionDTO;
import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.services.UsuarioService;
import api.astro.whats_orders_manager.services.EmailService;
import api.astro.whats_orders_manager.util.PaginacionUtil;
import api.astro.whats_orders_manager.util.PasswordUtil;
import api.astro.whats_orders_manager.util.ResponseUtil;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controlador para la gestión de Usuarios del Sistema
 * Permite CRUD completo, activar/desactivar usuarios y reset de contraseñas.
 * Solo accesible por usuarios con rol ADMIN.
 * 
 * @version 2.1 - Refactorizado con DTOs y Utils
 * @since 26/10/2025
 */
@Controller
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    private static final String URL_LOGIN = "http://localhost:8080/auth/login"; // TODO: Mover a configuración

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
            HttpSession session
    ) {
        log.info("Listando usuarios - página: {}, tamaño: {}, orden: {} {}", page, size, sort, direction);
        
        try {
            // Agregar datos del usuario logueado
            agregarDatosUsuarioAlModelo(model, session);
            
            // Obtener todos los usuarios
            List<Usuario> todosUsuarios = usuarioService.findAll();
            
            // Convertir Boolean a String para el filtro
            String estadoFiltro = activo != null ? (activo ? "activo" : "inactivo") : null;
            
            // Aplicar filtros
            List<Usuario> usuariosFiltrados = aplicarFiltros(todosUsuarios, rol, estadoFiltro, search);
            
            // Ordenar
            usuariosFiltrados = ordenarUsuarios(usuariosFiltrados, sort, direction);
            
            // Paginación manual
            PaginacionDTO<Usuario> paginacion = aplicarPaginacion(usuariosFiltrados, page, size);
            
            // Calcular estadísticas
            EstadisticasUsuariosDTO estadisticas = calcularEstadisticas(todosUsuarios);
            
            // Agregar al modelo
            agregarAtributosPaginacion(model, paginacion);
            agregarFiltrosAlModelo(model, rol, estadoFiltro, search, sort, direction);
            agregarEstadisticasAlModelo(model, estadisticas);
            
            log.info("Mostrando {} usuarios de un total de {}", 
                    paginacion.getContenido().size(), usuariosFiltrados.size());
            
            return "usuarios/usuarios";
            
        } catch (Exception e) {
            log.error("Error al listar usuarios: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar los usuarios");
            return "error/error";
        }
    }

    /**
     * Muestra formulario para crear nuevo usuario
     * 
     * GET /usuarios/form
     */
    @GetMapping("/form")
    public String mostrarFormularioNuevo(Model model, HttpSession session) {
        log.info("Mostrando formulario para nuevo usuario");
        
        try {
            agregarDatosUsuarioAlModelo(model, session);
            
            Usuario usuario = new Usuario();
            usuario.setActivo(true); // Activo por defecto
            usuario.setRol("USER"); // Rol USER por defecto
            
            model.addAttribute("usuario", usuario);
            model.addAttribute("esNuevo", true);
            model.addAttribute("titulo", "Nuevo Usuario");
            
            return "usuarios/form";
            
        } catch (Exception e) {
            log.error("Error al cargar formulario de nuevo usuario: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar el formulario");
            return "error/error";
        }
    }

    /**
     * Muestra formulario para editar usuario existente
     * 
     * GET /usuarios/form/{id}
     */
    @GetMapping("/form/{id}")
    public String mostrarFormularioEditar(
            @PathVariable Integer id,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        log.info("Mostrando formulario para editar usuario ID: {}", id);
        
        try {
            agregarDatosUsuarioAlModelo(model, session);
            
            Optional<Usuario> usuarioOpt = usuarioService.findById(id);
            
            if (usuarioOpt.isEmpty()) {
                log.warn("Usuario no encontrado con ID: {}", id);
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
            
        } catch (Exception e) {
            log.error("Error al cargar formulario de edición: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al cargar el formulario");
            return "redirect:/usuarios";
        }
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
            Model model
    ) {
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
            if (!validarTelefonoUnico(usuario, redirectAttributes)) {
                return "redirect:/usuarios/form" + (esNuevo ? "" : "/" + usuario.getIdUsuario());
            }
            
            // Validar email único
            if (!validarEmailUnico(usuario, redirectAttributes)) {
                return "redirect:/usuarios/form" + (esNuevo ? "" : "/" + usuario.getIdUsuario());
            }
            
            if (esNuevo) {
                return guardarNuevoUsuario(usuario, passwordNueva, passwordConfirmacion, redirectAttributes);
            } else {
                return actualizarUsuarioExistente(usuario, passwordNueva, passwordConfirmacion, redirectAttributes);
            }
            
        } catch (Exception e) {
            log.error("Error al guardar usuario: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al guardar el usuario. Por favor intente nuevamente.");
            return "redirect:/usuarios/form" + (esNuevo ? "" : "/" + usuario.getIdUsuario());
        }
    }

    /**
     * Elimina un usuario
     * 
     * DELETE /usuarios/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarUsuario(
            @PathVariable Integer id,
            HttpSession session
    ) {
        log.info("Eliminando usuario ID: {}", id);
        
        try {
            // Verificar que el usuario no se esté eliminando a sí mismo
            if (esUsuarioLogueado(id, session)) {
                log.warn("Intento de auto-eliminación bloqueado para usuario ID: {}", id);
                return ResponseUtil.error("No puedes eliminar tu propia cuenta");
            }
            
            Optional<Usuario> usuarioOpt = usuarioService.findById(id);
            if (usuarioOpt.isEmpty()) {
                log.warn("Intento de eliminar usuario inexistente ID: {}", id);
                return ResponseUtil.error("Usuario no encontrado");
            }
            
            usuarioService.deleteById(id);
            log.info("✅ Usuario eliminado exitosamente: ID {}", id);
            
            return ResponseUtil.success("Usuario eliminado exitosamente");
            
        } catch (Exception e) {
            log.error("Error al eliminar usuario: {}", e.getMessage(), e);
            return ResponseUtil.error("Error al eliminar el usuario. Por favor intente nuevamente.");
        }
    }

    /**
     * Activa o desactiva un usuario
     * 
     * POST /usuarios/toggle-active/{id}
     */
    @PostMapping("/toggle-active/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleActivo(
            @PathVariable Integer id,
            HttpSession session
    ) {
        log.info("Cambiando estado activo del usuario ID: {}", id);
        
        try {
            // Verificar que el usuario no se esté desactivando a sí mismo
            if (esUsuarioLogueado(id, session)) {
                log.warn("Intento de auto-desactivación bloqueado para usuario ID: {}", id);
                return ResponseUtil.error("No puedes desactivar tu propia cuenta");
            }
            
            Usuario usuario = usuarioService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            
            boolean nuevoEstado = !usuario.getActivo();
            usuario.setActivo(nuevoEstado);
            usuarioService.save(usuario);
            
            log.info("✅ Usuario {} {}", id, nuevoEstado ? "activado" : "desactivado");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("activo", nuevoEstado);
            response.put("message", "Usuario " + (nuevoEstado ? "activado" : "desactivado") + " exitosamente");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al cambiar estado del usuario: {}", e.getMessage(), e);
            return ResponseUtil.error("Error al cambiar el estado. Por favor intente nuevamente.");
        }
    }

    /**
     * Resetea la contraseña de un usuario y genera una nueva aleatoria
     * 
     * POST /usuarios/reset-password/{id}
     */
    @PostMapping("/reset-password/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resetPassword(@PathVariable Integer id) {
        log.info("Reseteando contraseña del usuario ID: {}", id);
        
        try {
            Usuario usuario = usuarioService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            
            // Generar contraseña aleatoria
            String nuevaPassword = generarPasswordAleatoria();
            
            // Encriptar y guardar
            usuario.setPassword(passwordEncoder.encode(nuevaPassword));
            usuarioService.save(usuario);
            
            log.info("✅ Contraseña reseteada exitosamente para usuario ID: {}", id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("password", nuevaPassword);
            response.put("message", "Contraseña reseteada exitosamente");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al resetear contraseña: {}", e.getMessage(), e);
            return ResponseUtil.error("Error al resetear la contraseña. Por favor intente nuevamente.");
        }
    }

    /**
     * Genera una contraseña aleatoria segura
     * 
     * GET /usuarios/generar-password (AJAX)
     */
    @GetMapping("/generar-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generarPassword() {
        String password = generarPasswordAleatoria();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("password", password);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Reenvía las credenciales de un usuario por email
     * Genera una nueva contraseña temporal y la envía por email
     * 
     * POST /usuarios/{id}/reenviar-credenciales
     */
    @PostMapping("/{id}/reenviar-credenciales")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reenviarCredenciales(@PathVariable Integer id) {
        log.info("Reenviando credenciales del usuario ID: {}", id);
        
        try {
            Usuario usuario = usuarioService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            
            // Validar que el usuario tenga email
            if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
                log.warn("El usuario {} no tiene email configurado", usuario.getNombre());
                return ResponseUtil.error("El usuario no tiene email configurado");
            }
            
            // Generar nueva contraseña temporal
            String nuevaPassword = generarPasswordAleatoria();
            
            // Encriptar y guardar
            usuario.setPassword(passwordEncoder.encode(nuevaPassword));
            usuarioService.save(usuario);
            
            // Enviar credenciales por email
            emailService.enviarCredencialesUsuario(usuario, nuevaPassword, URL_LOGIN);
            
            log.info("✅ Credenciales reenviadas exitosamente a: {}", usuario.getEmail());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Credenciales enviadas exitosamente a " + usuario.getEmail());
            
            return ResponseEntity.ok(response);
            
        } catch (MessagingException e) {
            log.error("❌ Error al enviar credenciales por email: {}", e.getMessage(), e);
            return ResponseUtil.error("Error al enviar el email. Por favor intente nuevamente.");
        } catch (Exception e) {
            log.error("❌ Error al reenviar credenciales: {}", e.getMessage(), e);
            return ResponseUtil.error("Error al reenviar credenciales. Por favor intente nuevamente.");
        }
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Aplica filtros a la lista de usuarios
     */
    private List<Usuario> aplicarFiltros(List<Usuario> usuarios, String rol, String estado, String search) {
        // Filtrar por rol
        if (rol != null && !rol.isEmpty()) {
            usuarios = usuarios.stream()
                    .filter(u -> u.getRol().equalsIgnoreCase(rol))
                    .collect(Collectors.toList());
        }
        
        // Filtrar por estado
        if (estado != null && !estado.isEmpty()) {
            boolean activo = "activo".equalsIgnoreCase(estado);
            usuarios = usuarios.stream()
                    .filter(u -> u.getActivo() == activo)
                    .collect(Collectors.toList());
        }
        
        // Filtrar por búsqueda (nombre, email, teléfono)
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            usuarios = usuarios.stream()
                    .filter(u -> {
                        String nombre = u.getNombre() != null ? u.getNombre().toLowerCase() : "";
                        String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
                        String telefono = u.getTelefono() != null ? u.getTelefono() : "";
                        
                        return nombre.contains(searchLower) 
                            || email.contains(searchLower) 
                            || telefono.contains(searchLower);
                    })
                    .collect(Collectors.toList());
        }
        
        return usuarios;
    }

    /**
     * Ordena la lista de usuarios según el campo y dirección especificados
     */
    private List<Usuario> ordenarUsuarios(List<Usuario> usuarios, String sortBy, String sortDir) {
        Comparator<Usuario> comparator;
        
        switch (sortBy) {
            case "nombre":
                comparator = Comparator.comparing(Usuario::getNombre, String.CASE_INSENSITIVE_ORDER);
                break;
            case "email":
                comparator = Comparator.comparing(u -> u.getEmail() != null ? u.getEmail() : "", 
                                                 String.CASE_INSENSITIVE_ORDER);
                break;
            case "telefono":
                comparator = Comparator.comparing(u -> u.getTelefono() != null ? u.getTelefono() : "",
                                                 String.CASE_INSENSITIVE_ORDER);
                break;
            case "rol":
                comparator = Comparator.comparing(Usuario::getRol, String.CASE_INSENSITIVE_ORDER);
                break;
            case "activo":
                comparator = Comparator.comparing(Usuario::getActivo);
                break;
            default:
                comparator = Comparator.comparing(Usuario::getId);
        }
        
        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }
        
        return usuarios.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * Aplica paginación manual a la lista de usuarios
     */
    private PaginacionDTO<Usuario> aplicarPaginacion(List<Usuario> usuarios, int page, int size) {
        int totalElementos = usuarios.size();
        int totalPaginas = (int) Math.ceil((double) totalElementos / size);
        
        // Ajustar página si está fuera de rango
        page = Math.max(0, Math.min(page, totalPaginas - 1));
        
        int inicio = page * size;
        int fin = Math.min(inicio + size, totalElementos);
        
        List<Usuario> usuariosPaginados = usuarios.subList(inicio, fin);
        
        return new PaginacionDTO<Usuario>(usuariosPaginados, page, size, totalElementos, totalPaginas);
    }

    /**
     * Calcula estadísticas de usuarios
     */
    private EstadisticasUsuariosDTO calcularEstadisticas(List<Usuario> todosUsuarios) {
        long totalUsuarios = todosUsuarios.size();
        long usuariosActivos = todosUsuarios.stream().filter(Usuario::getActivo).count();
        long usuariosInactivos = totalUsuarios - usuariosActivos;
        long administradores = todosUsuarios.stream()
                .filter(u -> "ADMIN".equalsIgnoreCase(u.getRol()))
                .count();
        long vendedores = todosUsuarios.stream()
                .filter(u -> "VENDEDOR".equalsIgnoreCase(u.getRol()))
                .count();
        
        return new EstadisticasUsuariosDTO(totalUsuarios, usuariosActivos, usuariosInactivos, 
                                       administradores, vendedores);
    }

    /**
     * Agrega atributos de paginación al modelo
     */
    private void agregarAtributosPaginacion(Model model, PaginacionDTO<Usuario> resultado) {
        model.addAttribute("usuarios", resultado.getContenido());
        model.addAttribute("currentPage", resultado.getPaginaActual());
        model.addAttribute("totalPages", resultado.getTotalPaginas());
        model.addAttribute("totalItems", resultado.getTotalElementos());
        model.addAttribute("pageSize", resultado.getTamanoPagina());
    }

    /**
     * Agrega atributos de filtros al modelo
     */
    private void agregarFiltrosAlModelo(Model model, String rol, String estado, String search, 
                                       String sortBy, String sortDir) {
        model.addAttribute("filtroRol", rol != null ? rol : "");
        model.addAttribute("filtroEstado", estado != null ? estado : "");
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
    }

    /**
     * Agrega estadísticas al modelo
     */
    private void agregarEstadisticasAlModelo(Model model, EstadisticasUsuariosDTO stats) {
        model.addAttribute("totalUsuarios", stats.total());
        model.addAttribute("usuariosActivos", stats.activos());
        model.addAttribute("usuariosInactivos", stats.inactivos());
        model.addAttribute("totalAdministradores", stats.administradores());
        model.addAttribute("totalVendedores", stats.vendedores());
    }

    /**
     * Valida que el teléfono sea único
     */
    private boolean validarTelefonoUnico(Usuario usuario, RedirectAttributes redirectAttributes) {
        Optional<Usuario> usuarioExistente = usuarioService.findByTelefono(usuario.getTelefono());
        
        if (usuarioExistente.isPresent() && !usuarioExistente.get().getId().equals(usuario.getId())) {
            redirectAttributes.addFlashAttribute("error", 
                "Ya existe un usuario con el teléfono " + usuario.getTelefono());
            log.warn("Intento de guardar usuario con teléfono duplicado: {}", usuario.getTelefono());
            return false;
        }
        
        return true;
    }

    /**
     * Valida que el email sea único (si está presente)
     */
    private boolean validarEmailUnico(Usuario usuario, RedirectAttributes redirectAttributes) {
        if (usuario.getEmail() != null && !usuario.getEmail().isBlank()) {
            Optional<Usuario> usuarioExistente = usuarioService.findByEmail(usuario.getEmail());
            
            if (usuarioExistente.isPresent() && !usuarioExistente.get().getId().equals(usuario.getId())) {
                redirectAttributes.addFlashAttribute("error", 
                    "Ya existe un usuario con el email " + usuario.getEmail());
                log.warn("Intento de guardar usuario con email duplicado: {}", usuario.getEmail());
                return false;
            }
        }
        
        return true;
    }

    /**
     * Guarda un nuevo usuario con contraseña y email
     */
    private String guardarNuevoUsuario(Usuario usuario, String password, String confirmPassword,
                                      RedirectAttributes redirectAttributes) {
        // Validar contraseña
        if (password == null || password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "La contraseña debe tener al menos 6 caracteres");
            redirectAttributes.addFlashAttribute("usuario", usuario);
            return "redirect:/usuarios/nuevo";
        }
        
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden");
            redirectAttributes.addFlashAttribute("usuario", usuario);
            return "redirect:/usuarios/nuevo";
        }
        
        // Encriptar contraseña
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setActivo(true);
        
        usuarioService.save(usuario);
        log.info("✅ Nuevo usuario creado: {} ({})", usuario.getNombre(), usuario.getRol());
        
        // Enviar credenciales por email si tiene email
        if (usuario.getEmail() != null && !usuario.getEmail().isBlank()) {
            try {
                emailService.enviarCredencialesUsuario(usuario, password, URL_LOGIN);
                log.info("📧 Credenciales enviadas a: {}", usuario.getEmail());
            } catch (MessagingException e) {
                log.error("❌ Error al enviar credenciales por email: {}", e.getMessage(), e);
                redirectAttributes.addFlashAttribute("warning", 
                    "Usuario creado pero no se pudo enviar el email de credenciales");
            }
        }
        
        redirectAttributes.addFlashAttribute("success", "Usuario creado exitosamente");
        return "redirect:/usuarios";
    }

    /**
     * Actualiza un usuario existente
     */
    private String actualizarUsuarioExistente(Usuario usuario, String password, String confirmPassword,
                                             RedirectAttributes redirectAttributes) {
        Usuario usuarioExistente = usuarioService.findById(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        // Actualizar datos básicos
        usuarioExistente.setNombre(usuario.getNombre());
        usuarioExistente.setTelefono(usuario.getTelefono());
        usuarioExistente.setEmail(usuario.getEmail());
        usuarioExistente.setRol(usuario.getRol());
        
        // Actualizar contraseña solo si se proporciona
        if (password != null && !password.isBlank()) {
            if (password.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "La contraseña debe tener al menos 6 caracteres");
                redirectAttributes.addFlashAttribute("usuario", usuario);
                return "redirect:/usuarios/editar/" + usuario.getId();
            }
            
            if (!password.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden");
                redirectAttributes.addFlashAttribute("usuario", usuario);
                return "redirect:/usuarios/editar/" + usuario.getId();
            }
            
            usuarioExistente.setPassword(passwordEncoder.encode(password));
            log.info("🔐 Contraseña actualizada para usuario: {}", usuarioExistente.getNombre());
        }
        
        usuarioService.save(usuarioExistente);
        log.info("✅ Usuario actualizado: {} ({})", usuarioExistente.getNombre(), usuarioExistente.getRol());
        
        redirectAttributes.addFlashAttribute("success", "Usuario actualizado exitosamente");
        return "redirect:/usuarios";
    }

    /**
     * Verifica si el usuario a eliminar/modificar es el usuario logueado
     */
    private boolean esUsuarioLogueado(Integer usuarioId, HttpSession session) {
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuario");
        return usuarioLogueado != null && usuarioLogueado.getId().equals(usuarioId);
    }

    /**
     * Genera una contraseña aleatoria segura
     * Delegado a PasswordUtil para reutilización
     */
    private String generarPasswordAleatoria() {
        return PasswordUtil.generarPasswordAleatoria();
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
