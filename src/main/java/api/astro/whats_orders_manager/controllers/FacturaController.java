package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.enums.InvoiceType;
import api.astro.whats_orders_manager.models.Cliente;
import api.astro.whats_orders_manager.models.Factura;
import api.astro.whats_orders_manager.models.LineaFactura;
import api.astro.whats_orders_manager.models.Producto;
import api.astro.whats_orders_manager.services.ClienteService;
import api.astro.whats_orders_manager.services.EmailService;
import api.astro.whats_orders_manager.services.FacturaService;
import api.astro.whats_orders_manager.services.LineaFacturaService;
import api.astro.whats_orders_manager.services.ProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/facturas")
@Slf4j
public class FacturaController {
    @Autowired
    private FacturaService facturaService;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private LineaFacturaService lineaFacturaService;

    @Autowired
    private EmailService emailService;

    @GetMapping
    public String listarFacturas(Model model, Authentication authentication) {
        model.addAttribute("facturas", facturaService.findAll());
        model.addAttribute("clientes", clienteService.findAll());
        model.addAttribute("tiposFactura", InvoiceType.values());
        
        // Agregar rol del usuario para controlar permisos en la vista
        if (authentication != null && authentication.getAuthorities() != null) {
            String userRole = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                    .orElse("USER");
            model.addAttribute("userRole", userRole);
        }
        
        return "facturas/facturas";
    }

    @GetMapping("/detalle/{id}")
    @ResponseBody
    public Factura obtenerDetalleFactura(@PathVariable Integer id) {
        return facturaService.findById(id).orElse(null);
    }

    @GetMapping("/nuevo")
    public String nuevaFactura(Model model) {
        model.addAttribute("factura", new Factura());
        return "facturas/form";
    }

    @ModelAttribute("tiposFactura")
    public InvoiceType[] getTiposFactura() {
        return InvoiceType.values();
    }

    @PostMapping("/guardar")
    public ResponseEntity<Factura> guardarFactura(
            @RequestBody Factura factura) {

        factura.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
        factura.setUpdateDate(Timestamp.valueOf(LocalDateTime.now()));

        Factura newFactura = facturaService.save(factura);

        return ResponseEntity.ok(newFactura);
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarFactura(@PathVariable Integer id) {
        Boolean exists = facturaService.existsById(id);
        if (!exists) {
            log.warn("La factura con ID {} no existe y no se puede eliminar.", id);
            return "redirect:/facturas?error=notFound";
        }
        Boolean hasLineas = lineaFacturaService.existsByFacturaId(id);
        if (hasLineas) {
            log.warn("La factura con ID {} tiene líneas asociadas y no se puede eliminar.", id);
            return "redirect:/facturas?error=hasLineas";
        }

        facturaService.deleteById(id);
        return "redirect:/facturas";
    }

    @GetMapping("/editar/{idFactura}")
    public String editarFactura(@PathVariable Integer idFactura, Model model) {
        Optional<Factura> facturaOptional = facturaService.findById(idFactura);

        if (facturaOptional.isPresent()) {
            model.addAttribute("factura", facturaOptional.get());
            // También puedes agregar datos adicionales si quieres (como clientes disponibles)
            return "facturas/form"; // Vista donde editarás la factura
        } else {
            // Si no se encuentra la factura, redirigir a listado
            return "redirect:/facturas";
        }
    }

    @GetMapping("/nueva")
    public String mostrarFormularioNuevaFactura(Model model) {
        Factura nuevaFactura = new Factura();
        // Si ocupás clientes u otra data relacionada:
        List<Cliente> clientes = clienteService.findAll();
        List<Producto> productos = productoService.findAll();

        model.addAttribute("factura", nuevaFactura);
        model.addAttribute("clientes", clientes);
        model.addAttribute("productos", productos);
        return "facturas/add-form"; // o la ruta del HTML
    }

    @PutMapping("/actualizar-estado/{id}")
    @ResponseBody
    public ResponseEntity<String> actualizarEstadoFactura(
            @PathVariable Integer id,
            @RequestParam Boolean entregado) {
        
        Optional<Factura> facturaOpt = facturaService.findById(id);
        
        if (facturaOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Factura factura = facturaOpt.get();
        factura.setEntregado(entregado);
        factura.setUpdateDate(Timestamp.valueOf(LocalDateTime.now()));
        facturaService.save(factura);
        
        log.info("Estado de factura {} actualizado a: {}", id, entregado);
        return ResponseEntity.ok("Estado actualizado correctamente");
    }

    /**
     * Envía una factura por email al cliente
     * Punto 5.3.1 - Envío de facturas por email
     */
    @PostMapping("/{id}/enviar-email")
    @ResponseBody
    public ResponseEntity<?> enviarFacturaPorEmail(@PathVariable Integer id) {
        try {
            // Buscar la factura
            Optional<Factura> facturaOpt = facturaService.findById(id);
            
            if (facturaOpt.isEmpty()) {
                log.warn("Factura con ID {} no encontrada", id);
                return ResponseEntity.notFound().build();
            }
            
            Factura factura = facturaOpt.get();
            
            // Validar que el cliente tenga email
            if (factura.getCliente() == null || 
                factura.getCliente().getEmail() == null || 
                factura.getCliente().getEmail().isBlank()) {
                log.warn("El cliente de la factura {} no tiene email configurado", id);
                return ResponseEntity.badRequest()
                    .body("{\"error\": \"El cliente no tiene email configurado\"}");
            }
            
            // Enviar el email
            emailService.enviarFacturaPorEmail(factura);
            
            log.info("✅ Factura {} enviada por email a {}", 
                factura.getNumeroFactura(), 
                factura.getCliente().getEmail());
            
            return ResponseEntity.ok()
                .body("{\"message\": \"Factura enviada exitosamente a " + 
                    factura.getCliente().getEmail() + "\"}");
            
        } catch (Exception e) {
            log.error("Error al enviar factura por email: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("{\"error\": \"Error al enviar el email: " + e.getMessage() + "\"}");
        }
    }

}