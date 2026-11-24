package api.astro.whats_orders_manager.modules.facturacion.correo.controller;

import api.astro.whats_orders_manager.modules.facturacion.correo.dto.EntregaDTO;
import api.astro.whats_orders_manager.modules.facturacion.correo.model.EntregaComprobante;
import api.astro.whats_orders_manager.modules.facturacion.correo.model.EstadoEntrega;
import api.astro.whats_orders_manager.modules.facturacion.correo.repository.EntregaComprobanteRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.AmbienteComprobanteGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * T-055 (ADR-070, criterio 10): lista simple de las entregas FALLIDO o PENDIENTE vencidas a la que enlaza el panel de
 * Facturación. Solo lectura; el reenvío se hace desde el detalle del documento.
 */
@Controller
@RequestMapping("/facturas/entregas")
@RequiredArgsConstructor
public class EntregaComprobanteViewController {

    static final int LIMITE = 200;

    private final EntregaComprobanteRepository repository;
    private final AmbienteComprobanteGuard ambienteGuard;
    private final Clock clock;

    /** Fila de la tabla: la entrega resumida y el enlace al documento. */
    public record Fila(EntregaDTO entrega, String tipoDocumento, String documento, String enlace, String ambiente) {
    }

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_VER')")
    @Transactional(readOnly = true)
    public String listar(@RequestParam(defaultValue = "FALLIDO") String estado, Model model, Authentication authentication) {
        boolean fallidas = "FALLIDO".equals(estado);
        if (!fallidas && !"PENDIENTE".equals(estado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado no admitido: " + estado);
        }
        // Mismo ambiente que los contadores del panel (el de la configuración activa; sin ella, todos)
        AmbienteHacienda ambiente = ambienteGuard.ambienteActivo().orElse(null);
        List<EntregaComprobante> entregas = repository.listar(fallidas ? EstadoEntrega.FALLIDO : EstadoEntrega.PENDIENTE,
            !fallidas, LocalDateTime.now(clock), ambiente, PageRequest.of(0, LIMITE));
        List<Fila> filas = entregas.stream().map(e -> {
            var c = e.getComprobante();
            boolean nota = c.esDeNota();
            String documento = nota ? "Nota de crédito " + c.getConsecutivo() : "Factura " + c.getConsecutivo();
            String enlace = nota ? "/facturas/notas-credito/" + c.getNotaFactura().getId()
                : "/facturas/pdf/" + c.getFactura().getIdFactura();
            return new Fila(EntregaDTO.de(e), nota ? "NOTA_CREDITO" : "FACTURA", documento, enlace,
                c.getAmbiente() != null ? c.getAmbiente().name() : null);
        }).toList();
        model.addAttribute("filas", filas);
        model.addAttribute("estado", fallidas ? "FALLIDO" : "PENDIENTE");
        model.addAttribute("titulo", fallidas ? "Entregas por correo fallidas" : "Entregas por correo pendientes vencidas");
        model.addAttribute("ambienteContadores", ambiente != null ? ambiente.name() : null);
        return "modules/facturacion/entregas";
    }
}
