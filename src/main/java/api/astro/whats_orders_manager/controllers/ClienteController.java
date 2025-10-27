package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.dto.PaginacionDTO;
import api.astro.whats_orders_manager.enums.InvoiceType;
import api.astro.whats_orders_manager.models.Cliente;
import api.astro.whats_orders_manager.services.ClienteService;
import api.astro.whats_orders_manager.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Controlador para la gestión de clientes
 * Maneja las operaciones CRUD y la visualización de clientes
 * 
 * @version 2.0 - Refactorizado con DTOs y Utils
 * @since 26/10/2025
 */
@Controller
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Slf4j
public class ClienteController {
    
    private final ClienteService clienteService;

    /**
     * Lista todos los clientes con paginación y ordenamiento
     */
    @GetMapping
    public String listarClientes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "idCliente") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Model model
    ) {
        log.info("Listando clientes - Página: {}, Tamaño: {}, Ordenar por: {} {}", 
                page, size, sortBy, sortDir);
        
        try {
            // Crear objeto Sort
            Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
            
            // Crear Pageable
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Obtener página de clientes
            Page<Cliente> clientesPage = clienteService.findAll(pageable);
            
            // Convertir a DTO y agregar atributos al modelo
            PaginacionDTO<Cliente> paginacion = PaginacionUtil.fromPage(clientesPage);
            PaginacionUtil.agregarAtributosConOrdenamiento(model, paginacion, "clientes", sortBy, sortDir);
            model.addAttribute("cliente", new Cliente());
            
            log.info("Clientes cargados: {} de {} total", 
                    clientesPage.getContent().size(), clientesPage.getTotalElements());
            
            return "clientes/clientes";
            
        } catch (Exception e) {
            log.error("Error al listar clientes: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar la lista de clientes");
            return "error/error";
        }
    }

    /**
     * Muestra el formulario para crear un nuevo cliente
     */
    @GetMapping("/nuevo")
    public String nuevoCliente(Model model) {
        log.info("Mostrando formulario de nuevo cliente");
        model.addAttribute("cliente", new Cliente());
        return "clientes/form";
    }

    /**
     * Proporciona los tipos de cliente disponibles para el formulario
     */
    @ModelAttribute("tiposCliente")
    public InvoiceType[] getTiposCliente() {
        return InvoiceType.values();
    }

    /**
     * Guarda un nuevo cliente o actualiza uno existente
     * La lógica de negocio está delegada al servicio
     */
    @PostMapping("/guardar")
    public String guardarCliente(
            @ModelAttribute Cliente cliente,
            BindingResult result,
            RedirectAttributes redirectAttributes
    ) {
        log.info("Guardando cliente: {}", cliente.getNombre());
        
        // Validaciones básicas del formulario
        if (result.hasErrors()) {
            log.warn("Errores de validación al guardar cliente");
            return "clientes/form";
        }
        
        try {
            // Delegar la lógica de negocio al servicio
            Cliente clienteGuardado = clienteService.guardarClienteConUsuario(cliente);
            
            redirectAttributes.addFlashAttribute("success", 
                "Cliente guardado exitosamente: " + clienteGuardado.getNombre());
            log.info("Cliente guardado exitosamente con ID: {}", clienteGuardado.getIdCliente());
            
            return "redirect:/clientes";
            
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al guardar cliente: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/clientes/nuevo";
            
        } catch (Exception e) {
            log.error("Error inesperado al guardar cliente: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al guardar el cliente. Por favor, inténtelo de nuevo.");
            return "redirect:/clientes/nuevo";
        }
    }

    /**
     * Muestra el formulario para editar un cliente existente
     */
    @GetMapping("/editar/{id}")
    public String editarCliente(
            @PathVariable Integer id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        log.info("Editando cliente con ID: {}", id);
        
        try {
            Optional<Cliente> cliente = clienteService.findById(id);
            
            if (cliente.isEmpty()) {
                log.warn("Cliente no encontrado con ID: {}", id);
                redirectAttributes.addFlashAttribute("error", 
                    "Cliente no encontrado");
                return "redirect:/clientes";
            }
            
            model.addAttribute("cliente", cliente.get());
            return "clientes/form";
            
        } catch (Exception e) {
            log.error("Error al cargar cliente para edición: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al cargar el cliente");
            return "redirect:/clientes";
        }
    }

    /**
     * API REST: Obtiene los detalles de un cliente
     */
    @GetMapping("/detalle/{id}")
    @ResponseBody
    public ResponseEntity<Cliente> obtenerCliente(@PathVariable Integer id) {
        log.info("Obteniendo detalles del cliente con ID: {}", id);
        
        try {
            Optional<Cliente> cliente = clienteService.findById(id);
            return cliente.map(ResponseEntity::ok)
                    .orElseGet(() -> {
                        log.warn("Cliente no encontrado con ID: {}", id);
                        return ResponseEntity.notFound().build();
                    });
                    
        } catch (Exception e) {
            log.error("Error al obtener cliente: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Elimina un cliente por su ID
     */
    @GetMapping("/eliminar/{id}")
    public String eliminarCliente(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes
    ) {
        log.info("Eliminando cliente con ID: {}", id);
        
        try {
            clienteService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", 
                "Cliente eliminado exitosamente");
            log.info("Cliente eliminado exitosamente con ID: {}", id);
            
        } catch (Exception e) {
            log.error("Error al eliminar cliente: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al eliminar el cliente. Puede tener registros asociados.");
        }
        
        return "redirect:/clientes";
    }
}
