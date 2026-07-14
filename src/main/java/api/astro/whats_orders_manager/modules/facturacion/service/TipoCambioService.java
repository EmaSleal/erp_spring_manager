package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.model.TipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.repository.TipoCambioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TipoCambioService {

    private final TipoCambioRepository repo;

    /** Returns the most recent active rate for the given destination currency. */
    @Transactional(readOnly = true)
    public Optional<TipoCambio> getTasaActual(MonedaFE moneda) {
        return repo.findFirstByMonedaDestinoAndActivoTrueOrderByFechaDesc(moneda);
    }

    /**
     * Saves a new exchange rate. If an active entry already exists for the same
     * fecha + monedaDestino, that entry is deactivated before saving the new one.
     */
    public TipoCambio registrar(TipoCambio tipoCambio) {
        log.info("Deactivating any existing active rate for {}/{}", tipoCambio.getFecha(), tipoCambio.getMonedaDestino());
        repo.deactivateExisting(tipoCambio.getFecha(), tipoCambio.getMonedaDestino());
        tipoCambio.setActivo(true);
        return repo.save(tipoCambio);
    }

    /** Returns the last 30 entries for a currency ordered most-recent first. */
    @Transactional(readOnly = true)
    public List<TipoCambio> listarPorMoneda(MonedaFE moneda) {
        return repo.findByMonedaDestinoOrderByFechaDesc(moneda)
                .stream()
                .limit(30)
                .toList();
    }
}
