package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.models.ConfiguracionFacturacion;
import api.astro.whats_orders_manager.models.ConfiguracionNotificaciones;
import api.astro.whats_orders_manager.models.Empresa;
import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.schedulers.RecordatorioPagoScheduler;
import api.astro.whats_orders_manager.services.ConfiguracionFacturacionService;
import api.astro.whats_orders_manager.services.ConfiguracionNotificacionesService;
import api.astro.whats_orders_manager.services.EmailService;
import api.astro.whats_orders_manager.services.EmpresaService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Optional;

/**
 * Controlador para la gestión de Configuración del Sistema
 * Maneja toda la configuración de la empresa, facturación, notificaciones, etc.
 * Solo accesible por usuarios con rol ADMIN.
 * 
 * @author Astro Dev Team
 * @version 2.0 - Sprint 2 - Fase 2
 * @since Sprint 2
 */
@Controller
@RequestMapping("/configuracion")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class ConfiguracionController {

    @Autowired
    private EmpresaService empresaService;
    
    @Autowired
    private ConfiguracionFacturacionService configuracionFacturacionService;

    @Autowired
    private RecordatorioPagoScheduler recordatorioPagoScheduler;

    @Autowired
    private ConfiguracionNotificacionesService configuracionNotificacionesService;

    @Autowired
    private EmailService emailService;

    /**
     * Página principal de configuración
     * Muestra tabs para: Empresa, Facturación, Usuarios, Notificaciones
     * 
     * GET /configuracion
     */
    @GetMapping
    public String index(@RequestParam(required = false) String tab, Model model, HttpSession session) {
        log.info("Accediendo a página de configuración - Tab: {}", tab != null ? tab : "empresa");
        
        // Obtener datos del usuario logueado
        agregarDatosUsuarioAlModelo(model, session);
        
        // Obtener empresa principal
        Empresa empresa = empresaService.getEmpresaPrincipal();
        model.addAttribute("empresa", empresa);
        
        // Obtener o crear configuración de facturación (para que esté disponible)
        ConfiguracionFacturacion configuracion = configuracionFacturacionService.getOrCreateConfiguracion();
        model.addAttribute("configuracion", configuracion);
        
        // Preview del número de factura
        String previewNumero = configuracion.generarNumeroFactura();
        model.addAttribute("previewNumero", previewNumero);
        
        // Obtener configuración de notificaciones (necesaria para el tab notificaciones)
        ConfiguracionNotificaciones configuracionNotif = configuracionNotificacionesService.getOrCreateConfiguracion();
        model.addAttribute("configuracionNotif", configuracionNotif);
        
        // Tab activo (por defecto: empresa, o el que se pase por parámetro)
        model.addAttribute("activeTab", tab != null ? tab : "empresa");
        
        return "configuracion/index";
    }

    /**
     * Vista de configuración de empresa (dentro del tab)
     * 
     * GET /configuracion/empresa
     */
    @GetMapping("/empresa")
    public String empresaTab(Model model, HttpSession session) {
        log.info("Accediendo a tab de configuración de empresa");
        
        agregarDatosUsuarioAlModelo(model, session);
        
        Empresa empresa = empresaService.getEmpresaPrincipal();
        model.addAttribute("empresa", empresa);
        
        // Obtener o crear configuración de facturación (para que esté disponible)
        ConfiguracionFacturacion configuracion = configuracionFacturacionService.getOrCreateConfiguracion();
        model.addAttribute("configuracion", configuracion);
        
        // Preview del número de factura
        String previewNumero = configuracion.generarNumeroFactura();
        model.addAttribute("previewNumero", previewNumero);
        
        model.addAttribute("activeTab", "empresa");
        
        return "configuracion/index";
    }

    /**
     * Guardar o actualizar datos de la empresa
     * 
     * POST /configuracion/empresa/guardar
     */
    @PostMapping("/empresa/guardar")
    public String guardarEmpresa(
            @Valid @ModelAttribute("empresa") Empresa empresa,
            BindingResult result,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        log.info("Guardando datos de empresa: {}", empresa.getNombreEmpresa());
        
        if (result.hasErrors()) {
            log.error("Errores de validación al guardar empresa");
            redirectAttributes.addFlashAttribute("error", "Por favor corrige los errores en el formulario");
            redirectAttributes.addFlashAttribute("activeTab", "empresa");
            return "redirect:/configuracion";
        }
        
        try {
            // Obtener ID del usuario logueado
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            Integer usuarioId = (usuario != null) ? usuario.getIdUsuario() : null;
            
            // Si la empresa ya existe (tiene ID), actualizar; sino, crear
            Empresa empresaGuardada;
            if (empresa.getIdEmpresa() != null && empresa.getIdEmpresa() > 0) {
                empresaGuardada = empresaService.update(empresa, usuarioId);
                log.info("Empresa actualizada exitosamente: ID {}", empresaGuardada.getIdEmpresa());
                redirectAttributes.addFlashAttribute("success", "Datos de la empresa actualizados exitosamente");
            } else {
                empresaGuardada = empresaService.save(empresa, usuarioId);
                log.info("Empresa creada exitosamente: ID {}", empresaGuardada.getIdEmpresa());
                redirectAttributes.addFlashAttribute("success", "Empresa creada exitosamente");
            }
            
            redirectAttributes.addFlashAttribute("activeTab", "empresa");
            
        } catch (IllegalArgumentException e) {
            log.error("Error al guardar empresa: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("activeTab", "empresa");
        } catch (Exception e) {
            log.error("Error inesperado al guardar empresa", e);
            redirectAttributes.addFlashAttribute("error", "Error al guardar los datos de la empresa");
            redirectAttributes.addFlashAttribute("activeTab", "empresa");
        }
        
        return "redirect:/configuracion";
    }

    /**
     * Subir logo de la empresa
     * 
     * POST /configuracion/empresa/subir-logo
     */
    @PostMapping("/empresa/subir-logo")
    public String subirLogo(
            @RequestParam("logo") MultipartFile file,
            @RequestParam("empresaId") Integer empresaId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        log.info("Subiendo logo para empresa ID: {}", empresaId);
        
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Por favor selecciona un archivo");
            redirectAttributes.addFlashAttribute("activeTab", "empresa");
            return "redirect:/configuracion";
        }
        
        try {
            // Obtener ID del usuario logueado
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            Integer usuarioId = (usuario != null) ? usuario.getIdUsuario() : null;
            
            empresaService.guardarLogo(empresaId, file, usuarioId);
            
            log.info("Logo subido exitosamente para empresa ID: {}", empresaId);
            redirectAttributes.addFlashAttribute("success", "Logo actualizado exitosamente");
            redirectAttributes.addFlashAttribute("activeTab", "empresa");
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación al subir logo: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("activeTab", "empresa");
        } catch (IOException e) {
            log.error("Error de I/O al subir logo", e);
            redirectAttributes.addFlashAttribute("error", "Error al guardar el archivo del logo");
            redirectAttributes.addFlashAttribute("activeTab", "empresa");
        } catch (Exception e) {
            log.error("Error inesperado al subir logo", e);
            redirectAttributes.addFlashAttribute("error", "Error inesperado al subir el logo");
            redirectAttributes.addFlashAttribute("activeTab", "empresa");
        }
        
        return "redirect:/configuracion";
    }

    /**
     * Subir favicon de la empresa
     * 
     * POST /configuracion/empresa/subir-favicon
     */
    @PostMapping("/empresa/subir-favicon")
    public String subirFavicon(
            @RequestParam("favicon") MultipartFile file,
            @RequestParam("empresaId") Integer empresaId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        log.info("Subiendo favicon para empresa ID: {}", empresaId);
        
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Por favor selecciona un archivo");
            redirectAttributes.addFlashAttribute("activeTab", "empresa");
            return "redirect:/configuracion";
        }
        
        try {
            // Obtener ID del usuario logueado
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            Integer usuarioId = (usuario != null) ? usuario.getIdUsuario() : null;
            
            empresaService.guardarFavicon(empresaId, file, usuarioId);
            
            log.info("Favicon subido exitosamente para empresa ID: {}", empresaId);
            redirectAttributes.addFlashAttribute("success", "Favicon actualizado exitosamente");
            redirectAttributes.addFlashAttribute("activeTab", "empresa");
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación al subir favicon: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("activeTab", "empresa");
        } catch (IOException e) {
            log.error("Error de I/O al subir favicon", e);
            redirectAttributes.addFlashAttribute("error", "Error al guardar el archivo del favicon");
            redirectAttributes.addFlashAttribute("activeTab", "empresa");
        } catch (Exception e) {
            log.error("Error inesperado al subir favicon", e);
            redirectAttributes.addFlashAttribute("error", "Error inesperado al subir el favicon");
            redirectAttributes.addFlashAttribute("activeTab", "empresa");
        }
        
        return "redirect:/configuracion";
    }

    /**
     * Eliminar logo de la empresa
     * 
     * POST /configuracion/empresa/eliminar-logo
     */
    @PostMapping("/empresa/eliminar-logo")
    public String eliminarLogo(
            @RequestParam("empresaId") Integer empresaId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        log.info("Eliminando logo de empresa ID: {}", empresaId);
        
        try {
            // Obtener ID del usuario logueado
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            Integer usuarioId = (usuario != null) ? usuario.getIdUsuario() : null;
            
            empresaService.eliminarLogo(empresaId, usuarioId);
            
            log.info("Logo eliminado exitosamente para empresa ID: {}", empresaId);
            redirectAttributes.addFlashAttribute("success", "Logo eliminado exitosamente");
            redirectAttributes.addFlashAttribute("activeTab", "empresa");
            
        } catch (Exception e) {
            log.error("Error al eliminar logo", e);
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el logo");
            redirectAttributes.addFlashAttribute("activeTab", "empresa");
        }
        
        return "redirect:/configuracion";
    }

    /**
     * Eliminar favicon de la empresa
     * 
     * POST /configuracion/empresa/eliminar-favicon
     */
    @PostMapping("/empresa/eliminar-favicon")
    public String eliminarFavicon(
            @RequestParam("empresaId") Integer empresaId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        log.info("Eliminando favicon de empresa ID: {}", empresaId);
        
        try {
            // Obtener ID del usuario logueado
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            Integer usuarioId = (usuario != null) ? usuario.getIdUsuario() : null;
            
            empresaService.eliminarFavicon(empresaId, usuarioId);
            
            log.info("Favicon eliminado exitosamente para empresa ID: {}", empresaId);
            redirectAttributes.addFlashAttribute("success", "Favicon eliminado exitosamente");
            redirectAttributes.addFlashAttribute("activeTab", "empresa");
            
        } catch (Exception e) {
            log.error("Error al eliminar favicon", e);
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el favicon");
            redirectAttributes.addFlashAttribute("activeTab", "empresa");
        }
        
        return "redirect:/configuracion";
    }

    // ==================== FACTURACIÓN ====================

    /**
     * Vista de configuración de facturación (dentro del tab)
     * 
     * GET /configuracion/facturacion
     */
    @GetMapping("/facturacion")
    public String facturacionTab(Model model, HttpSession session) {
        log.info("Accediendo a tab de configuración de facturación");
        
        agregarDatosUsuarioAlModelo(model, session);
        
        // Obtener empresa principal
        Empresa empresa = empresaService.getEmpresaPrincipal();
        model.addAttribute("empresa", empresa);
        
        // Obtener o crear configuración de facturación
        ConfiguracionFacturacion configuracion = configuracionFacturacionService.getOrCreateConfiguracion();
        model.addAttribute("configuracion", configuracion);
        
        // Preview del número de factura
        String previewNumero = configuracion.generarNumeroFactura();
        model.addAttribute("previewNumero", previewNumero);
        
        model.addAttribute("activeTab", "facturacion");
        
        return "configuracion/index";
    }

    /**
     * Guardar o actualizar configuración de facturación
     * 
     * POST /configuracion/facturacion/guardar
     */
    @PostMapping("/facturacion/guardar")
    public String guardarConfiguracionFacturacion(
            @Valid @ModelAttribute("configuracion") ConfiguracionFacturacion configuracion,
            BindingResult result,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        log.info("Guardando configuración de facturación: Serie={}, IGV={}%", 
                configuracion.getSerieFactura(), configuracion.getIgv());
        
        if (result.hasErrors()) {
            log.error("Errores de validación al guardar configuración de facturación");
            redirectAttributes.addFlashAttribute("error", "Por favor corrige los errores en el formulario");
            redirectAttributes.addFlashAttribute("activeTab", "facturacion");
            return "redirect:/configuracion/facturacion";
        }
        
        try {
            // Validar configuración
            configuracionFacturacionService.validarConfiguracion(configuracion);
            
            // Si la configuración ya existe (tiene ID), actualizar; sino, crear
            ConfiguracionFacturacion configuracionGuardada;
            if (configuracion.getId() != null && configuracion.getId() > 0) {
                configuracionGuardada = configuracionFacturacionService.update(configuracion);
                log.info("Configuración de facturación actualizada exitosamente: ID {}", configuracionGuardada.getId());
                redirectAttributes.addFlashAttribute("success", "Configuración de facturación actualizada exitosamente");
            } else {
                configuracionGuardada = configuracionFacturacionService.save(configuracion);
                log.info("Configuración de facturación creada exitosamente: ID {}", configuracionGuardada.getId());
                redirectAttributes.addFlashAttribute("success", "Configuración de facturación creada exitosamente");
            }
            
            redirectAttributes.addFlashAttribute("activeTab", "facturacion");
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Error al guardar configuración de facturación: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("activeTab", "facturacion");
        } catch (Exception e) {
            log.error("Error inesperado al guardar configuración de facturación", e);
            redirectAttributes.addFlashAttribute("error", "Error al guardar la configuración de facturación");
            redirectAttributes.addFlashAttribute("activeTab", "facturacion");
        }
        
        return "redirect:/configuracion/facturacion";
    }

    /**
     * Ejecuta manualmente el scheduler de recordatorios de pago
     * Útil para testing sin esperar a las 9:00 AM
     * 
     * POST /configuracion/ejecutar-recordatorios
     */
    @PostMapping("/ejecutar-recordatorios")
    @ResponseBody
    public String ejecutarRecordatorios() {
        try {
            log.info("🔧 Ejecutando scheduler de recordatorios manualmente desde admin");
            recordatorioPagoScheduler.ejecutarManualmente();
            return "OK";
        } catch (Exception e) {
            log.error("❌ Error al ejecutar recordatorios manualmente: {}", e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }

    // ==================== NOTIFICACIONES ====================

    /**
     * Vista de configuración de notificaciones
     * 
     * GET /configuracion/notificaciones
     */
    @GetMapping("/notificaciones")
    public String notificaciones(Model model, HttpSession session) {
        log.info("Accediendo a configuración de notificaciones");
        
        // Obtener datos del usuario
        agregarDatosUsuarioAlModelo(model, session);
        
        // Obtener o crear configuración de notificaciones
        ConfiguracionNotificaciones configuracion = configuracionNotificacionesService.getOrCreateConfiguracion();
        model.addAttribute("configuracionNotif", configuracion);
        
        // Tab activo
        model.addAttribute("activeTab", "notificaciones");
        
        return "configuracion/index";
    }

    /**
     * Guarda la configuración de notificaciones
     * 
     * POST /configuracion/notificaciones/guardar
     */
    @PostMapping("/notificaciones/guardar")
    public String guardarNotificaciones(
            @Valid @ModelAttribute("configuracionNotif") ConfiguracionNotificaciones configuracion,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        
        log.info("Guardando configuración de notificaciones");
        
        try {
            // Validar errores
            if (result.hasErrors()) {
                log.warn("Errores de validación en configuración de notificaciones");
                redirectAttributes.addFlashAttribute("error", "Por favor, corrija los errores en el formulario");
                redirectAttributes.addFlashAttribute("activeTab", "notificaciones");
                return "redirect:/configuracion?tab=notificaciones";
            }
            
            // Obtener usuario actual
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario != null) {
                configuracion.setUpdateBy(usuario.getIdUsuario());
            }
            
            // Guardar o actualizar
            ConfiguracionNotificaciones guardada;
            if (configuracion.getIdConfiguracion() != null) {
                guardada = configuracionNotificacionesService.update(configuracion);
            } else {
                guardada = configuracionNotificacionesService.save(configuracion);
            }
            
            log.info("✅ Configuración de notificaciones guardada exitosamente: {}", guardada);
            redirectAttributes.addFlashAttribute("success", "Configuración de notificaciones guardada correctamente");
            redirectAttributes.addFlashAttribute("activeTab", "notificaciones");
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Error al guardar configuración de notificaciones: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("activeTab", "notificaciones");
        } catch (Exception e) {
            log.error("Error inesperado al guardar configuración de notificaciones", e);
            redirectAttributes.addFlashAttribute("error", "Error al guardar la configuración de notificaciones");
            redirectAttributes.addFlashAttribute("activeTab", "notificaciones");
        }
        
        return "redirect:/configuracion?tab=notificaciones";
    }

    /**
     * Envía un email de prueba al administrador
     * 
     * POST /configuracion/notificaciones/probar-email
     */
    @PostMapping("/notificaciones/probar-email")
    @ResponseBody
    public String probarEmail(@RequestParam String emailDestino) {
        try {
            log.info("📧 Enviando email de prueba a: {}", emailDestino);
            
            boolean enviado = emailService.enviarEmailPrueba(emailDestino);
            
            if (enviado) {
                return "OK";
            } else {
                return "ERROR: No se pudo enviar el email";
            }
            
        } catch (Exception e) {
            log.error("❌ Error al enviar email de prueba: {}", e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Agrega los datos del usuario logueado al modelo
     * Esto es necesario para el navbar
     */
    private void agregarDatosUsuarioAlModelo(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        
        if (usuario != null) {
            model.addAttribute("userName", usuario.getNombre());
            model.addAttribute("userRole", usuario.getRol()); // rol es String
            
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
