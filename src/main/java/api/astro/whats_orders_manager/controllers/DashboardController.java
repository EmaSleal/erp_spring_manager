package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.models.dto.ModuloDTO;
import api.astro.whats_orders_manager.services.ClienteService;
import api.astro.whats_orders_manager.services.FacturaService;
import api.astro.whats_orders_manager.services.ProductoService;
import api.astro.whats_orders_manager.services.UsuarioService;
import api.astro.whats_orders_manager.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * DASHBOARD CONTROLLER
 * WhatsApp Orders Manager
 * ============================================================================
 * Controlador principal para la página de dashboard/inicio.
 * 
 * Funcionalidades:
 * - Mostrar estadísticas generales del sistema
 * - Mostrar módulos disponibles según rol del usuario
 * - Cargar información del usuario actual
 * - Generar breadcrumbs y datos de navegación
 * 
 * @version 3.1 - Aplicado StringUtil
 * @since 26/10/2025
 * ============================================================================
 */
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final ClienteService clienteService;
    private final ProductoService productoService;
    private final FacturaService facturaService;
    private final UsuarioService usuarioService;

    /**
     * Muestra el dashboard principal con estadísticas y módulos disponibles
     * 
     * @param model Modelo para pasar datos a la vista
     * @param authentication Información del usuario autenticado
     * @return Vista del dashboard
     */
    @GetMapping
    public String mostrarDashboard(Model model, Authentication authentication) {
        log.info("Accediendo al dashboard");
        
        try {
            // Obtener usuario actual
            Usuario usuario = obtenerUsuarioActual(authentication);
            log.debug("Usuario actual: {} ({})", usuario.getNombre(), usuario.getRol());

            // Cargar estadísticas del sistema
            cargarEstadisticas(model);

            // Cargar información del usuario para el navbar
            cargarInformacionUsuario(model, usuario);

            // Cargar módulos disponibles según rol
            List<ModuloDTO> modulos = cargarModulosSegunRol(usuario.getRol());
            model.addAttribute("modulos", modulos);

            // Breadcrumbs
            model.addAttribute("currentPage", "dashboard");
            model.addAttribute("title", "Dashboard");

            log.info("Dashboard cargado exitosamente para usuario: {}", usuario.getNombre());
            return "dashboard/dashboard";
            
        } catch (Exception e) {
            log.error("Error al cargar dashboard: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar el dashboard");
            return "error/error";
        }
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Carga las estadísticas del sistema en el modelo
     */
    private void cargarEstadisticas(Model model) {
        long totalClientes = clienteService.count();
        long totalProductos = productoService.count();
        long facturasHoy = facturaService.countByFechaToday();
        BigDecimal totalPendiente = facturaService.sumTotalPendiente();

        model.addAttribute("totalClientes", totalClientes);
        model.addAttribute("totalProductos", totalProductos);
        model.addAttribute("facturasHoy", facturasHoy);
        model.addAttribute("totalPendiente", totalPendiente != null ? totalPendiente : BigDecimal.ZERO);
        
        log.debug("Estadísticas cargadas - Clientes: {}, Productos: {}, Facturas hoy: {}", 
                totalClientes, totalProductos, facturasHoy);
    }

    /**
     * Carga la información del usuario en el modelo para el navbar
     */
    private void cargarInformacionUsuario(Model model, Usuario usuario) {
        model.addAttribute("userName", usuario.getNombre());
        model.addAttribute("userRole", usuario.getRol());
        model.addAttribute("userInitials", StringUtil.generarIniciales(usuario.getNombre()));
        
        // Avatar si existe
        if (usuario.getAvatar() != null && !usuario.getAvatar().isEmpty()) {
            model.addAttribute("userAvatar", usuario.getAvatar());
        }
    }

    /**
     * Obtiene el usuario actual desde el Authentication
     * 
     * @param authentication Objeto de autenticación de Spring Security
     * @return Usuario actual
     * @throws RuntimeException si el usuario no está autenticado o no se encuentra
     */
    private Usuario obtenerUsuarioActual(Authentication authentication) {
        if (authentication == null) {
            log.error("Authentication es null");
            throw new RuntimeException("Usuario no autenticado");
        }
        
        if (!(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            log.error("Principal no es UserDetails: {}", authentication.getPrincipal().getClass());
            throw new RuntimeException("Usuario no autenticado correctamente");
        }
        
        String telefono = userDetails.getUsername();
        return usuarioService.findByTelefono(telefono)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado con teléfono: {}", telefono);
                    return new RuntimeException("Usuario no encontrado");
                });
    }

    /**
     * Carga los módulos disponibles según el rol del usuario
     * 
     * Permisos por rol:
     * - ADMIN: Acceso total (todos los módulos)
     * - USER: Módulos operativos + reportes (sin configuración/usuarios)
     * - VENDEDOR: Solo crear facturas + consultar catálogos
     * - VISUALIZADOR: Solo lectura de información
     * 
     * @param rol Rol del usuario (ADMIN, USER, VENDEDOR, VISUALIZADOR)
     * @return Lista de módulos con permisos y estado
     */
    private List<ModuloDTO> cargarModulosSegunRol(String rol) {
        log.debug("Cargando módulos para rol: {}", rol);
        
        List<ModuloDTO> modulos = new ArrayList<>();
        
        boolean esAdmin = "ADMIN".equals(rol);
        boolean esUser = "USER".equals(rol);
        boolean esVendedor = "VENDEDOR".equals(rol);
        boolean esVisualizador = "VISUALIZADOR".equals(rol);

        // Clientes (todos pueden ver)
        modulos.add(crearModulo(
                "Clientes",
                "Gestión de clientes",
                "fas fa-users",
                "#4CAF50",
                "/clientes",
                true,
                esAdmin || esUser || esVendedor || esVisualizador
        ));

        // Productos (todos pueden ver)
        modulos.add(crearModulo(
                "Productos",
                "Catálogo de productos",
                "fas fa-box",
                "#FF9800",
                "/productos",
                true,
                esAdmin || esUser || esVendedor || esVisualizador
        ));

        // Facturas (todos pueden ver)
        modulos.add(crearModulo(
                "Facturas",
                "Gestión de facturas",
                "fas fa-file-invoice-dollar",
                "#9C27B0",
                "/facturas",
                true,
                esAdmin || esUser || esVendedor || esVisualizador
        ));

        // Usuarios (solo ADMIN)
        modulos.add(crearModulo(
                "Usuarios",
                "Gestión de usuarios",
                "fas fa-user-cog",
                "#3F51B5",
                "/usuarios",
                true,
                esAdmin
        ));

        // Pedidos (próximamente - ADMIN, USER, VENDEDOR)
        modulos.add(crearModulo(
                "Pedidos",
                "Gestión de pedidos",
                "fas fa-shopping-cart",
                "#F44336",
                "/pedidos",
                false,
                esAdmin || esUser || esVendedor
        ));

        // Reportes (ADMIN y USER)
        modulos.add(crearModulo(
                "Reportes",
                "Informes y estadísticas",
                "fas fa-chart-bar",
                "#00BCD4",
                "/reportes",
                true,
                esAdmin || esUser
        ));

        // Configuración (solo ADMIN)
        modulos.add(crearModulo(
                "Configuración",
                "Ajustes del sistema",
                "fas fa-cog",
                "#607D8B",
                "/configuracion",
                true,
                esAdmin
        ));

        long modulosVisibles = modulos.stream().filter(ModuloDTO::isVisible).count();
        log.debug("Módulos cargados: {} visibles de {}", modulosVisibles, modulos.size());

        return modulos;
    }

    /**
     * Método auxiliar para crear un ModuloDTO
     * Reduce duplicación de código
     */
    private ModuloDTO crearModulo(
            String titulo,
            String descripcion,
            String icono,
            String color,
            String url,
            boolean implementado,
            boolean visible
    ) {
        return new ModuloDTO(titulo, descripcion, icono, color, url, implementado, visible);
    }
}
