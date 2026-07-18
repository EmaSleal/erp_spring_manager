package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.modules.facturacion.dto.TipoCambioDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.FuenteTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.model.TipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.service.TipoCambioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-cambio")
@RequiredArgsConstructor
@Slf4j
public class TipoCambioRestController {

    private final TipoCambioService tipoCambioService;

    @GetMapping("/actual")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TipoCambioDTO> getActual(@RequestParam String moneda) {
        log.debug("Fetching current rate for moneda={}", moneda);
        MonedaFE monedaFE = MonedaFE.fromCodigo(moneda);
        return tipoCambioService.getTasaActual(monedaFE)
                .map(tc -> ResponseEntity.ok(toDTO(tc)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TipoCambioDTO> registrar(
            @Valid @RequestBody TipoCambioDTO dto
    ) {
        log.info("Registering exchange rate for moneda={} fecha={}", dto.getMonedaDestino(), dto.getFecha());
        try {
            TipoCambio saved = tipoCambioService.registrar(fromDTO(dto));
            return ResponseEntity.ok(toDTO(saved));
        } catch (Exception e) {
            log.error("Error registering exchange rate: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/historial")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_VER')")
    public ResponseEntity<List<TipoCambioDTO>> historial(
            @RequestParam String moneda,
            org.springframework.security.core.Authentication authentication
    ) {
        log.debug("Fetching rate history for moneda={}", moneda);
        MonedaFE monedaFE = MonedaFE.fromCodigo(moneda);
        List<TipoCambioDTO> result = tipoCambioService.listarPorMoneda(monedaFE)
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(result);
    }

    // Inline mapping — no separate mapper class needed for this simple DTO
    private TipoCambioDTO toDTO(TipoCambio tc) {
        return TipoCambioDTO.builder()
                .id(tc.getId())
                .monedaOrigen(tc.getMonedaOrigen() != null ? tc.getMonedaOrigen().getCodigo() : null)
                .monedaDestino(tc.getMonedaDestino().getCodigo())
                .simboloMoneda(tc.getMonedaDestino().getSimbolo())
                .fecha(tc.getFecha())
                .tasaCompra(tc.getTasaCompra())
                .tasaVenta(tc.getTasaVenta())
                .fuente(tc.getFuente().name())
                .build();
    }

    private TipoCambio fromDTO(TipoCambioDTO dto) {
        return TipoCambio.builder()
                .monedaOrigen(dto.getMonedaOrigen() != null
                        ? MonedaFE.fromCodigo(dto.getMonedaOrigen())
                        : MonedaFE.CRC)
                .monedaDestino(MonedaFE.fromCodigo(dto.getMonedaDestino()))
                .fecha(dto.getFecha())
                .tasaCompra(dto.getTasaCompra())
                .tasaVenta(dto.getTasaVenta())
                .fuente(dto.getFuente() != null
                        ? FuenteTipoCambio.valueOf(dto.getFuente())
                        : FuenteTipoCambio.MANUAL)
                .build();
    }
}
