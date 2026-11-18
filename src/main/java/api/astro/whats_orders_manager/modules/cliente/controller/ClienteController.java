package api.astro.whats_orders_manager.modules.cliente.controller;

import api.astro.whats_orders_manager.shared.dto.PaginacionDTO;
import api.astro.whats_orders_manager.modules.facturacion.enums.InvoiceType;
import api.astro.whats_orders_manager.modules.cliente.dto.ClienteConProblemasFe;
import api.astro.whats_orders_manager.modules.cliente.exception.DatosClienteInvalidosException;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.ProblemaDatoFe;
import api.astro.whats_orders_manager.modules.cliente.service.ClienteService;
import api.astro.whats_orders_manager.shared.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.Map;
import org.springframework.http.HttpStatus;

@Controller
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Slf4j
public class ClienteController {

    /** Valor de ?filtro= que muestra solo los clientes con datos FE inválidos (T-038). */
    public static final String FILTRO_FE_INVALIDOS = "feInvalidos";
    public static final String MSG_DATOS_INVALIDOS = "Los datos del cliente no son válidos.";

    private final ClienteService clienteService;

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CLIENTE_VER')")
    public String listarClientes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "idCliente") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String filtro,
            Model model,
            Authentication authentication
    ) {
        try {
            Pageable pageable = PaginacionUtil.buildPageable(page, size, sortBy, sortDir,
                    Set.of("idCliente", "nombre", "email", "telefono"), "idCliente");
            boolean soloFeInvalidos = FILTRO_FE_INVALIDOS.equals(filtro);
            Page<Cliente> clientesPage;
            if (soloFeInvalidos) {
                // Reporte en memoria (pocos clientes); se pagina la lista ya filtrada, ordenada por id.
                List<ClienteConProblemasFe> reporte = clienteService.clientesConDatosFeInvalidos();
                int desde = (int) Math.min(pageable.getOffset(), reporte.size());
                int hasta = Math.min(desde + pageable.getPageSize(), reporte.size());
                List<ClienteConProblemasFe> pagina = reporte.subList(desde, hasta);
                Map<Integer, List<ProblemaDatoFe>> problemas = new LinkedHashMap<>();
                pagina.forEach(r -> problemas.put(r.cliente().getIdCliente(), r.problemas()));
                model.addAttribute("problemasPorCliente", problemas);
                clientesPage = new PageImpl<>(pagina.stream().map(ClienteConProblemasFe::cliente).toList(),
                        pageable, reporte.size());
            } else {
                clientesPage = clienteService.findAll(pageable);
            }
            model.addAttribute("filtroFeInvalidos", soloFeInvalidos);
            model.addAttribute("contadorFeInvalidos", clienteService.contarClientesConDatosFeInvalidos());

            PaginacionDTO<Cliente> paginacion = PaginacionUtil.fromPage(clientesPage);
            PaginacionUtil.agregarAtributosConOrdenamiento(model, paginacion, "clientes", sortBy, sortDir);
            model.addAttribute("cliente", new Cliente());

            return "modules/cliente/clientes";

        } catch (Exception e) {
            log.error("Error al listar clientes: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar la lista de clientes");
            return "shared/error/error";
        }
    }

    @ModelAttribute("tiposCliente")
    public InvoiceType[] getTiposCliente() {
        return InvoiceType.values();
    }

    @PostMapping("/guardar")
    @PreAuthorize("@permisoService.tieneAlgunPermisoPorCodigo(#authentication.name, 'CLIENTE_CREAR', 'CLIENTE_EDITAR')")
    public ResponseEntity<Map<String, Object>> guardarCliente(
            @RequestBody Cliente cliente,
            Authentication authentication
    ) {
        try {
            Cliente clienteGuardado = clienteService.save(cliente);
            return ResponseEntity.ok(Map.of("success", "Cliente guardado exitosamente: " + clienteGuardado.getNombre()));

        } catch (DatosClienteInvalidosException e) {
            List<Map<String, String>> errores = e.getProblemas().stream()
                    .map(p -> Map.of("campo", p.campo(), "mensaje", p.mensaje()))
                    .toList();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", DatosClienteInvalidosException.RESUMEN, "errores", errores));

        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al guardar cliente: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", MSG_DATOS_INVALIDOS));

        } catch (Exception e) {
            log.error("Error inesperado al guardar cliente: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar el cliente. Por favor, inténtelo de nuevo."));
        }
    }

    @GetMapping("/editar/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CLIENTE_EDITAR')")
    public String editarCliente(
            @PathVariable Integer id,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        try {
            Optional<Cliente> cliente = clienteService.findById(id);

            if (cliente.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Cliente no encontrado");
                return "redirect:/clientes";
            }

            model.addAttribute("cliente", cliente.get());
            return "modules/cliente/form";

        } catch (Exception e) {
            log.error("Error al cargar cliente para edición: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al cargar el cliente");
            return "redirect:/clientes";
        }
    }

    @GetMapping("/todos")
    @ResponseBody
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CLIENTE_VER')")
    public ResponseEntity<List<Cliente>> listarTodos(Authentication authentication) {
        return ResponseEntity.ok(clienteService.findAll());
    }

    @GetMapping("/detalle/{id}")
    @ResponseBody
    public ResponseEntity<Cliente> obtenerCliente(@PathVariable Integer id) {
        try {
            Optional<Cliente> cliente = clienteService.findById(id);
            return cliente.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());

        } catch (Exception e) {
            log.error("Error al obtener cliente: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarCliente(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            clienteService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Cliente eliminado exitosamente");

        } catch (Exception e) {
            log.error("Error al eliminar cliente: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el cliente. Puede tener registros asociados.");
        }

        return "redirect:/clientes";
    }
}
