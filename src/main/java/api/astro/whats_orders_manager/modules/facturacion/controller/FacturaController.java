package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.shared.dto.PaginacionDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.FacturaDetalleDTO;
import api.astro.whats_orders_manager.modules.facturacion.exception.FacturaBloqueadaException;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaSustitutaService;
import api.astro.whats_orders_manager.modules.facturacion.service.NotaCreditoElegibilidad;
import api.astro.whats_orders_manager.modules.facturacion.service.PoliticaMutabilidadFactura;
import api.astro.whats_orders_manager.modules.facturacion.dto.FacturaPendienteDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.PagoDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.FacturaMapper;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.PagoMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.AmbienteComprobanteGuard;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.enums.InvoiceType;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.cliente.service.ClienteService;
import api.astro.whats_orders_manager.shared.service.MonedaService;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaPdfService;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaService;
import api.astro.whats_orders_manager.modules.facturacion.service.PagoService;
import api.astro.whats_orders_manager.modules.facturacion.service.SaldoFavorService;
import api.astro.whats_orders_manager.modules.facturacion.dto.SaldoFavorDTO;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoPago;
import api.astro.whats_orders_manager.shared.util.ResponseUtil;
import api.astro.whats_orders_manager.shared.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/**
 * Controlador para la gestión de Facturas
 * Maneja las operaciones CRUD y funcionalidades relacionadas con facturas
 * 
 * @version 3.2 - Agregada integración con módulo de pagos
 * @since 26/10/2025
 */
@Controller
@RequestMapping("/facturas")
@RequiredArgsConstructor
@Slf4j
public class FacturaController {
    
    private final FacturaService facturaService;
    private final ClienteService clienteService;
    private final PagoService pagoService;
    private final MonedaService monedaService;
    private final FacturaMapper facturaMapper;
    private final FacturaPdfService facturaPdfService;
    private final PagoMapper pagoMapper;
    private final PoliticaMutabilidadFactura politicaMutabilidad;
    private final FacturaSustitutaService sustitutaService;
    private final NotaCreditoElegibilidad notaCreditoElegibilidad;
    private final AmbienteComprobanteGuard ambienteGuard;
    private final SaldoFavorService saldoFavorService;
    /** T-055: por campo para no cambiar el constructor del que dependen las pruebas; sin él no hay campo correo. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private api.astro.whats_orders_manager.modules.facturacion.correo.service.EntregaComprobanteService entregaCorreoService;

    /**
     * Lista todas las facturas con paginación y ordenamiento
     */
    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_VER')")
    public String listarFacturas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "idFactura") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isPaid,
            @RequestParam(required = false) String estadoFE,
            @RequestParam(required = false) Boolean sustitutaPendiente,
            Model model,
            Authentication authentication
    ) {
        log.info("Listando facturas - Página: {}, Tamaño: {}, Ordenar por: {} {}",
                page, size, sortBy, sortDir);

        try {
            Pageable pageable = PaginacionUtil.buildPageable(page, size, sortBy, sortDir,
                    Set.of("idFactura", "fechaEmision", "total", "estado", "estadoPago", "fechaEntrega"), "idFactura");

            // Derivar el filtro de entrega a partir del valor plano del select
            Boolean entregado = "entregado".equals(status) ? Boolean.TRUE
                    : "pendiente".equals(status) ? Boolean.FALSE
                    : null;

            // sinFE y estadoFE son mutuamente excluyentes: "sin_fe" busca facturas sin
            // comprobante electrónico; cualquier otro valor es un nombre de EstadoComprobante
            Boolean sinFE = null;
            EstadoComprobante estadoFEFiltro = null;
            if (estadoFE != null && !estadoFE.isBlank()) {
                if ("sin_fe".equals(estadoFE)) {
                    sinFE = Boolean.TRUE;
                } else {
                    try {
                        estadoFEFiltro = EstadoComprobante.valueOf(estadoFE);
                    } catch (IllegalArgumentException e) {
                        log.warn("Valor de estadoFE inválido recibido: {}", estadoFE);
                    }
                }
            }

            // T-043: «anuladas que admiten sustituta» del panel; el ambiente es el de la configuración activa (T-044) y,
            // sin configuración legible, no se filtra por ambiente (igual que el contador del panel)
            Boolean sustitutaFiltro = Boolean.TRUE.equals(sustitutaPendiente) ? Boolean.TRUE : null;
            AmbienteHacienda ambienteSustituta = sustitutaFiltro != null
                    ? ambienteGuard.ambienteActivo().orElse(null) : null;

            boolean sinFiltros = sustitutaFiltro == null && startDate == null && endDate == null
                    && (status == null || status.isBlank())
                    && isPaid == null
                    && (estadoFE == null || estadoFE.isBlank());

            // Obtener página de facturas (con o sin filtros)
            Page<Factura> facturasPage = sinFiltros
                    ? facturaService.findAll(pageable)
                    : facturaService.buscarConFiltros(pageable, startDate, endDate, entregado, isPaid, sinFE, estadoFEFiltro,
                            sustitutaFiltro, ambienteSustituta);

            // Convertir a DTO y agregar atributos de paginación usando PaginacionUtil
            PaginacionDTO<Factura> paginacion = PaginacionUtil.fromPage(facturasPage);
            PaginacionUtil.agregarAtributosConOrdenamiento(model, paginacion, "facturas", sortBy, sortDir);

            // Preservar los filtros activos para el formulario y los enlaces de paginación
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("status", status);
            model.addAttribute("isPaid", isPaid);
            model.addAttribute("estadoFE", estadoFE);
            model.addAttribute("sustitutaPendiente", sustitutaFiltro);
            model.addAttribute("ambienteSustituta", ambienteSustituta != null ? ambienteSustituta.name() : null);

            // Agregar datos adicionales para la vista
            model.addAttribute("clientes", clienteService.findAll());
            model.addAttribute("tiposFactura", InvoiceType.values());
            model.addAttribute("simboloMoneda", monedaService.obtenerSimboloMoneda());
            
            // Enums de Facturación Electrónica Costa Rica
            model.addAttribute("condicionesVenta", api.astro.whats_orders_manager.modules.facturacion.electronica.enums.CondicionVentaFE.values());
            model.addAttribute("mediosPago", api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MedioPagoFE.values());
            model.addAttribute("monedas", api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE.values());
            
            log.info("Facturas cargadas: {} de {} total",
                    facturasPage.getContent().size(), facturasPage.getTotalElements());
            
            return "modules/facturacion/facturas";
            
        } catch (Exception e) {
            log.error("Error al listar facturas: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar las facturas");
            return "shared/error/error";
        }
    }

    /**
     * Obtiene el detalle de una factura (API REST)
     */
    @GetMapping("/detalle/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_VER')")
    @ResponseBody
    public ResponseEntity<FacturaDetalleDTO> obtenerDetalleFactura(@PathVariable Integer id, Authentication authentication) {
        log.info("Obteniendo detalle de factura ID: {}", id);
        
        try {
            Optional<Factura> facturaOpt = facturaService.findById(id);
            
            if (facturaOpt.isEmpty()) {
                log.warn("Factura no encontrada con ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            Factura factura = facturaOpt.get();
            FacturaDetalleDTO detalle = facturaMapper.toDetalleDTO(factura);

            // Estado FE del comprobante vigente para el modal (ADR-031): proyección JPQL, sin la entidad
            PoliticaMutabilidadFactura.EstadoFe estadoFe = politicaMutabilidad.estadoFe(id);
            if (estadoFe != null) {
                detalle.setIdComprobante(estadoFe.idComprobante());
                detalle.setEstadoComprobante(estadoFe.estado());
                detalle.setEstadoComprobanteDescripcion(estadoFe.estado().getCodigo());
                detalle.setEstadoComprobanteColor(estadoFe.estado().getColor());
                detalle.setEstadoComprobanteIcono(estadoFe.estado().getIcono());
                detalle.setClaveComprobante(estadoFe.claveNumerica());
                detalle.setRevisarEnAtv(estadoFe.revisarEnAtv());
            } else {
                detalle.setRevisarEnAtv(false);
            }
            if (entregaCorreoService != null) {
                // T-055: reenvío por correo del comprobante vigente (null si no hay)
                detalle.setCorreo(entregaCorreoService.resumenCorreo(detalle.getIdComprobante(), authentication.getName()));
            }
            sustitutaService.completarDetalle(factura, detalle);
            completarPagosPorSaldoFavor(detalle);
            NotaCreditoElegibilidad.Resultado notaCredito = notaCreditoElegibilidad.evaluar(factura,
                authentication.getName());
            detalle.setPuedeEmitirNotaCredito(notaCredito.puedeEmitir());
            detalle.setMotivoNotaCreditoNoDisponible(notaCredito.motivoNoDisponible());
            detalle.setPuedeEmitirNotaTotal(notaCredito.puedeTotal());
            detalle.setMotivoNotaTotalNoDisponible(notaCredito.motivoTotal());
            detalle.setPuedeEmitirNotaParcial(notaCredito.puedeParcial());
            if (notaCredito.puedeEmitir()) {
                notaCreditoElegibilidad.periodoFacturaOriginal(factura).ifPresent(detalle::setPeriodoFacturaOriginal);
            }
            if (notaCredito.puedeParcial()) {
                // T-047: total, acreditado y disponible por línea y por factura salen del servidor (criterio 12)
                notaCreditoElegibilidad.disponible(factura).ifPresent(d -> {
                    detalle.setNotaCreditoTotalComprobante(d.totalComprobante());
                    detalle.setNotaCreditoTotalAcreditado(d.totalAcreditado());
                    detalle.setNotaCreditoDisponible(d.disponible());
                    detalle.setNotaCreditoLineas(d.lineas().stream()
                        .map(l -> new api.astro.whats_orders_manager.modules.facturacion.dto.LineaNotaDisponibleDTO(
                            l.idLineaFactura(), l.numeroLinea(), l.descripcion(), l.total(), l.acreditado(),
                            l.disponible()))
                        .toList());
                });
            }
            return ResponseEntity.ok(detalle);
                    
        } catch (Exception e) {
            log.error("Error al obtener factura: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Proporciona los tipos de factura disponibles para el formulario
     */
    @ModelAttribute("tiposFactura")
    public InvoiceType[] getTiposFactura() {
        return InvoiceType.values();
    }

    /**
     * Guarda una nueva factura (API REST)
     * Nota: Las fechas se manejan automáticamente por @EntityListeners en la entidad
     */
    @PostMapping("/guardar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_CREAR')")
    @ResponseBody
    public ResponseEntity<Factura> guardarFactura(@RequestBody Factura factura, Authentication authentication) {
        log.info("Guardando nueva factura");
        
        try {
            Factura nuevaFactura = facturaService.save(factura);
            log.info("Factura guardada exitosamente con ID: {}", nuevaFactura.getIdFactura());
            return ResponseEntity.ok(nuevaFactura);
            
        } catch (FacturaBloqueadaException e) {
            throw e; // 409 (FacturaBloqueadaAdvice)
        } catch (api.astro.whats_orders_manager.modules.facturacion.exception.FacturaRegimenConflictoException
                 | api.astro.whats_orders_manager.modules.regimen.service.RegimenNoDefinidoException e) {
            throw e; // 409 (FacturaRegimenAdvice)
        } catch (Exception e) {
            log.error("Error al guardar factura: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Elimina una factura por su ID
     */
    @GetMapping("/eliminar/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_ELIMINAR')")
    public String eliminarFactura(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        log.info("Intentando eliminar factura ID: {}", id);
        try {
            facturaService.eliminarConValidacion(id);
            redirectAttributes.addFlashAttribute("success", "Factura eliminada exitosamente");
            log.info("Factura eliminada exitosamente ID: {}", id);
        } catch (FacturaBloqueadaException e) {
            throw e; // 409 (FacturaBloqueadaAdvice)
        } catch (NoSuchElementException | IllegalStateException e) {
            log.warn("No se pudo eliminar factura {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error al eliminar factura: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al eliminar la factura");
        }
        return "redirect:/facturas";
    }

    /**
     * Anula una factura (ADR-008): revierte su asiento de venta y la marca como anulada.
     * No se permite si la factura tiene pagos confirmados o conciliados.
     */
    @PostMapping("/anular/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_ANULAR')")
    public String anularFactura(
            @PathVariable Integer id,
            @RequestParam(required = false) String motivo,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        log.info("Intentando anular factura ID: {}", id);
        try {
            facturaService.anular(id, motivo);
            redirectAttributes.addFlashAttribute("success", "Factura anulada exitosamente");
        } catch (FacturaBloqueadaException e) {
            throw e; // 409 (FacturaBloqueadaAdvice)
        } catch (NoSuchElementException | IllegalStateException | IllegalArgumentException e) {
            log.warn("No se pudo anular factura {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error al anular factura: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al anular la factura");
        }
        return "redirect:/facturas";
    }

    /**
     * Muestra el formulario para editar una factura existente
     */
    @GetMapping("/editar/{idFactura}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_EDITAR')")
    public String editarFactura(
            @PathVariable Integer idFactura,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        log.info("Editando factura ID: {}", idFactura);
        
        try {
            Optional<Factura> facturaOptional = facturaService.findById(idFactura);

            if (facturaOptional.isEmpty()) {
                log.warn("Factura no encontrada con ID: {}", idFactura);
                redirectAttributes.addFlashAttribute("error", 
                    "Factura no encontrada");
                return "redirect:/facturas";
            }
            
            model.addAttribute("factura", facturaOptional.get());
            // ADR-030: la UI deshabilita los campos fiscales y muestra el motivo; el servidor sigue siendo la autoridad
            model.addAttribute("bloqueoFiscal", politicaMutabilidad.evaluar(idFactura));
            // T-039 review 1: la sustituta no mueve inventario; la UI lo avisa (texto fijo en la plantilla)
            model.addAttribute("esSustituta", facturaOptional.get().getFacturaSustituida() != null);
            return "modules/facturacion/form";
            
        } catch (Exception e) {
            log.error("Error al cargar factura para edición: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al cargar la factura");
            return "redirect:/facturas";
        }
    }

    /**
     * Actualiza el estado de entrega de una factura (API REST)
     */
    @PutMapping("/actualizar-estado/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> actualizarEstadoFactura(
            @PathVariable Integer id,
            @RequestParam Boolean entregado,
            @RequestParam(required = false) String descripcion,
            @RequestParam(name = "fechaEntrega", required = false) String fechaEntregaStr
    ) {
        log.info("Actualizando estado de factura ID: {} a entregado={}", id, entregado);
        try {
            facturaService.actualizarEstado(id, entregado, descripcion, fechaEntregaStr);
            log.info("Estado de factura {} actualizado exitosamente", id);
            return ResponseEntity.ok(Map.of("mensaje", "Estado actualizado correctamente"));
        } catch (NoSuchElementException e) {
            log.warn("Factura no encontrada con ID: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al actualizar estado de factura: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("mensaje", "Error al actualizar el estado"));
        }
    }

    @GetMapping("/pdf/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_VER')")
    public ResponseEntity<byte[]> descargarPdfFactura(@PathVariable Integer id, Authentication authentication) {
        try {
            byte[] pdf = facturaPdfService.generarPdfFactura(id);
            return ResponseUtil.pdf(pdf, "factura-" + id + ".pdf");
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al generar PDF de factura {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== ENDPOINTS DE PAGOS ====================
    
    /**
     * Obtiene los pagos de una factura específica (API REST).
     */
    @GetMapping("/{idFactura}/pagos")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_VER')")
    @ResponseBody
    public ResponseEntity<List<PagoDTO>> obtenerPagosPorFactura(
            @PathVariable Integer idFactura,
            Authentication authentication
    ) {
        log.info("Obteniendo pagos de la factura ID: {}", idFactura);
        
        try {
            List<Pago> pagos = pagoService.findByFacturaId(idFactura);
            List<PagoDTO> pagosDTOs = pagoMapper.toDTOList(pagos);
            
            return ResponseEntity.ok(pagosDTOs);
            
        } catch (Exception e) {
            log.error("Error al obtener pagos de factura: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Calcula el total pagado de una factura (API REST).
     */
    @GetMapping("/{idFactura}/total-pagado")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_VER')")
    @ResponseBody
    public ResponseEntity<BigDecimal> obtenerTotalPagado(
            @PathVariable Integer idFactura,
            Authentication authentication
    ) {
        log.info("Calculando total pagado de la factura ID: {}", idFactura);
        
        try {
            BigDecimal totalPagado = pagoService.calcularTotalPagadoPorFactura(idFactura);
            return ResponseEntity.ok(totalPagado);
            
        } catch (Exception e) {
            log.error("Error al calcular total pagado: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Calcula el saldo pendiente de una factura (API REST).
     */
    @GetMapping("/{idFactura}/saldo-pendiente")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_VER')")
    @ResponseBody
    public ResponseEntity<BigDecimal> obtenerSaldoPendiente(
            @PathVariable Integer idFactura,
            Authentication authentication
    ) {
        log.info("Calculando saldo pendiente de la factura ID: {}", idFactura);
        
        try {
            Optional<Factura> facturaOpt = facturaService.findById(idFactura);
            
            if (facturaOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Factura factura = facturaOpt.get();
            BigDecimal saldoPendiente = factura.calcularSaldoPendiente();
            
            return ResponseEntity.ok(saldoPendiente);
            
        } catch (Exception e) {
            log.error("Error al calcular saldo pendiente: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * API REST: Returns pending invoices for a given client.
     *
     * @param idCliente client ID
     * @return list of invoices with a pending balance greater than zero
     */
    @GetMapping("/api/cliente/{idCliente}")
    @ResponseBody
    public ResponseEntity<List<FacturaPendienteDTO>> obtenerFacturasPorCliente(@PathVariable Integer idCliente) {
        try {
            log.info("Obteniendo facturas del cliente ID: {}", idCliente);
            List<FacturaPendienteDTO> result = facturaService.obtenerPendientesPorCliente(idCliente);
            log.info("Encontradas {} facturas con saldo pendiente para cliente {}", result.size(), idCliente);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error al obtener facturas del cliente {}: {}", idCliente, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }


    /**
     * T-048: a cada pago NOTA_CREDITO del detalle le agrega la aplicación del saldo a favor de la que nace y su nota de
     * origen (el detalle de la factura destino muestra «Nota de origen»).
     */
    private void completarPagosPorSaldoFavor(FacturaDetalleDTO detalle) {
        if (detalle.getPagos() == null) {
            return;
        }
        List<Long> ids = detalle.getPagos().stream().filter(p -> p.getTipoPago() == TipoPago.NOTA_CREDITO)
            .map(PagoDTO::getIdPago).toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, SaldoFavorDTO.Aplicacion> aplicaciones = saldoFavorService.aplicacionesPorPago(ids);
        for (PagoDTO pago : detalle.getPagos()) {
            SaldoFavorDTO.Aplicacion a = aplicaciones.get(pago.getIdPago());
            if (a != null) {
                pago.setIdAplicacionSaldoFavor(a.id());
                pago.setIdNotaOrigen(a.idNota());
                pago.setClaveNotaOrigen(a.claveNota());
                pago.setNumeroNotaOrigen(a.numeroNota());
            }
        }
    }
}
