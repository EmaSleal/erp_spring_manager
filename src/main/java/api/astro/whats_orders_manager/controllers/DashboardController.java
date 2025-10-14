package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.models.dto.ModuloDTO;
import api.astro.whats_orders_manager.services.ClienteService;
import api.astro.whats_orders_manager.services.FacturaService;
import api.astro.whats_orders_manager.services.ProductoService;
import api.astro.whats_orders_manager.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * ============================================================================
 */
@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private FacturaService facturaService;

    @Autowired
    private UsuarioService usuarioService;

    /**
     * Muestra el dashboard principal con estadísticas y módulos disponibles
     * 
     * @param model Modelo para pasar datos a la vista
     * @param authentication Información del usuario autenticado
     * @return Vista del dashboard
     */
    @GetMapping
    public String mostrarDashboard(Model model, Authentication authentication) {
        // Obtener usuario actual
        Usuario usuario = obtenerUsuarioActual(authentication);

        // Cargar estadísticas
        long totalClientes = clienteService.count();
        long totalProductos = productoService.count();
        long facturasHoy = facturaService.countByFechaToday();
        BigDecimal totalPendiente = facturaService.sumTotalPendiente();

        // Agregar estadísticas al modelo
        model.addAttribute("totalClientes", totalClientes);
        model.addAttribute("totalProductos", totalProductos);
        model.addAttribute("facturasHoy", facturasHoy);
        model.addAttribute("totalPendiente", totalPendiente != null ? totalPendiente : BigDecimal.ZERO);

        // Información de usuario para navbar
        model.addAttribute("userName", usuario.getNombre());
        model.addAttribute("userRole", usuario.getRol());
        model.addAttribute("userInitials", generarIniciales(usuario.getNombre()));

        // Módulos disponibles según rol
        List<ModuloDTO> modulos = cargarModulosSegunRol(usuario.getRol());
        model.addAttribute("modulos", modulos);

        // Breadcrumbs
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("title", "Dashboard");

        return "dashboard/dashboard";
    }

    /**
     * Obtiene el usuario actual desde el Authentication
     * 
     * @param authentication Objeto de autenticación de Spring Security
     * @return Usuario actual
     */
    private Usuario obtenerUsuarioActual(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            String telefono = userDetails.getUsername();
            return usuarioService.findByTelefono(telefono)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        }
        throw new RuntimeException("Usuario no autenticado");
    }

    /**
     * Genera las iniciales del nombre del usuario
     * 
     * @param nombre Nombre completo del usuario
     * @return Iniciales en mayúsculas (máximo 2 caracteres)
     */
    private String generarIniciales(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "US";
        }

        String[] partes = nombre.trim().split("\\s+");
        
        if (partes.length >= 2) {
            // Si tiene dos o más palabras, usar primera letra de las primeras dos
            return String.valueOf(partes[0].charAt(0)) + partes[1].charAt(0);
        } else {
            // Si tiene una sola palabra, usar las dos primeras letras
            String palabra = partes[0];
            if (palabra.length() >= 2) {
                return palabra.substring(0, 2);
            } else {
                return palabra + "U"; // Agregar 'U' si solo tiene una letra
            }
        }
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
        List<ModuloDTO> modulos = new ArrayList<>();
        
        boolean esAdmin = "ADMIN".equals(rol);
        boolean esUser = "USER".equals(rol);
        boolean esVendedor = "VENDEDOR".equals(rol);
        boolean esVisualizador = "VISUALIZADOR".equals(rol);

        // Clientes (ADMIN, USER, VENDEDOR, VISUALIZADOR pueden ver)
        modulos.add(new ModuloDTO(
                "Clientes",
                "Gestión de clientes",
                "fas fa-users",
                "#4CAF50",
                "/clientes",
                true,
                esAdmin || esUser || esVendedor || esVisualizador
        ));

        // Productos (ADMIN, USER, VENDEDOR, VISUALIZADOR pueden ver)
        modulos.add(new ModuloDTO(
                "Productos",
                "Catálogo de productos",
                "fas fa-box",
                "#FF9800",
                "/productos",
                true,
                esAdmin || esUser || esVendedor || esVisualizador
        ));

        // Facturas (ADMIN, USER, VENDEDOR, VISUALIZADOR pueden ver)
        modulos.add(new ModuloDTO(
                "Facturas",
                "Gestión de facturas",
                "fas fa-file-invoice-dollar",
                "#9C27B0",
                "/facturas",
                true,
                esAdmin || esUser || esVendedor || esVisualizador
        ));

        // Usuarios (solo ADMIN)
        modulos.add(new ModuloDTO(
                "Usuarios",
                "Gestión de usuarios",
                "fas fa-user-cog",
                "#3F51B5",
                "/usuarios",
                true,
                esAdmin
        ));

        // Pedidos (próximamente - visible para ADMIN, USER, VENDEDOR)
        modulos.add(new ModuloDTO(
                "Pedidos",
                "Gestión de pedidos",
                "fas fa-shopping-cart",
                "#F44336",
                "/pedidos",
                false,  // No implementado aún
                esAdmin || esUser || esVendedor
        ));

        // Reportes (ADMIN y USER pueden ver)
        modulos.add(new ModuloDTO(
                "Reportes",
                "Informes y estadísticas",
                "fas fa-chart-bar",
                "#00BCD4",
                "/reportes",
                false,  // No implementado aún
                esAdmin || esUser
        ));

        // Configuración (solo ADMIN)
        modulos.add(new ModuloDTO(
                "Configuración",
                "Ajustes del sistema",
                "fas fa-cog",
                "#607D8B",
                "/configuracion",
                true,
                esAdmin
        ));

        return modulos;
    }
}
