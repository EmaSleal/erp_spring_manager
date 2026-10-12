package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.dto.TasaDelDia;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoSincronizacionTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.enums.FuenteTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.exception.TipoCambioNoDisponibleException;
import api.astro.whats_orders_manager.modules.facturacion.model.TipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.repository.TipoCambioRepository;
import api.astro.whats_orders_manager.modules.facturacion.util.ConversionMoneda;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TipoCambioService {

    /** Valor de creadoPor de las tasas registradas por la sincronización automática (T-008). */
    public static final String CREADO_POR_SINCRONIZACION = "SINCRONIZACION";

    private final TipoCambioRepository repo;

    /** Returns the most recent active rate for the given destination currency. */
    @Transactional(readOnly = true)
    public Optional<TipoCambio> getTasaActual(MonedaFE moneda) {
        return repo.findFirstByMonedaDestinoAndActivoTrueOrderByFechaDesc(moneda);
    }

    /**
     * Tasa de venta (CRC por unidad) vigente para la moneda en la fecha dada (ADR-011):
     * la del registro ACTIVO más reciente con fecha &lt;= {@code fecha}.
     *
     * @param moneda moneda extranjera; CRC (o null) devuelve 1
     * @param fecha  fecha de la operación; null se interpreta como hoy
     * @return tasa de venta
     * @throws TipoCambioNoDisponibleException si no hay un tipo de cambio activo registrado
     */
    @Transactional(readOnly = true)
    public BigDecimal obtenerTasaVenta(MonedaFE moneda, LocalDate fecha) {
        if (moneda == null || moneda == MonedaFE.CRC) {
            return BigDecimal.ONE;
        }
        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        return repo.findFirstByMonedaDestinoAndActivoTrueAndFechaLessThanEqualOrderByFechaDescIdDesc(moneda, dia)
                .map(TipoCambio::getTasaVenta)
                .orElseThrow(() -> new TipoCambioNoDisponibleException(moneda, dia));
    }

    /**
     * Tipo de cambio de una operación (pago) entre la moneda del pago y la del documento (ADR-011).
     * Si ambas son CRC devuelve 1. Si el usuario indicó un tipo de cambio se respeta (debe ser &gt; 0);
     * si no, se toma la tasa de venta de la moneda extranjera involucrada en la fecha dada.
     *
     * @throws IllegalArgumentException si las monedas son extranjeras distintas o el tipo de cambio es &lt;= 0
     * @throws TipoCambioNoDisponibleException si no hay tasa registrada para sugerir
     */
    @Transactional(readOnly = true)
    public BigDecimal resolverTipoCambioOperacion(MonedaFE monedaPago, MonedaFE monedaDocumento,
                                                  BigDecimal tipoCambioIndicado, LocalDate fecha) {
        MonedaFE extranjera = ConversionMoneda.monedaExtranjera(monedaPago, monedaDocumento);
        if (extranjera == MonedaFE.CRC) {
            return BigDecimal.ONE;
        }
        if (tipoCambioIndicado != null) {
            if (tipoCambioIndicado.signum() <= 0) {
                throw new IllegalArgumentException("El tipo de cambio debe ser mayor a 0");
            }
            return tipoCambioIndicado;
        }
        return obtenerTasaVenta(extranjera, fecha);
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

    /**
     * Guarda la tasa obtenida de un proveedor automático (T-008), en la convención de filas de
     * T-007: moneda_origen = CRC, moneda_destino = moneda extranjera, fecha = la del proveedor.
     *
     * <ul>
     *   <li>Sin tasa activa para ese día y moneda → crea una fila activa con la fuente del proveedor.</li>
     *   <li>Tasa activa de otra fuente (MANUAL, o BCCR para la API) → se conserva intacta.</li>
     *   <li>Tasa activa de la misma fuente → se actualiza solo si cambió compra o venta.</li>
     * </ul>
     */
    public EstadoSincronizacionTipoCambio registrarDesdeProveedor(MonedaFE moneda, TasaDelDia tasa,
                                                                  FuenteTipoCambio fuente) {
        Optional<TipoCambio> existente = repo.findByFechaAndMonedaDestinoAndActivoTrue(tasa.fecha(), moneda);
        if (existente.isPresent()) {
            TipoCambio actual = existente.get();
            if (actual.getFuente() != fuente) {
                log.info("Se conserva el tipo de cambio {} activo de {} para {} (id={}); no se reemplaza con {}",
                        actual.getFuente(), moneda, tasa.fecha(), actual.getId(), fuente);
                return EstadoSincronizacionTipoCambio.CONSERVADO;
            }
            if (actual.getTasaCompra().compareTo(tasa.compra()) == 0
                    && actual.getTasaVenta().compareTo(tasa.venta()) == 0) {
                log.debug("Tipo de cambio {} de {} para {} sin cambios", fuente, moneda, tasa.fecha());
                return EstadoSincronizacionTipoCambio.SIN_CAMBIOS;
            }
            log.info("Actualizando tipo de cambio {} de {} para {}: compra {} → {}, venta {} → {}",
                    fuente, moneda, tasa.fecha(), actual.getTasaCompra(), tasa.compra(),
                    actual.getTasaVenta(), tasa.venta());
            actual.setTasaCompra(tasa.compra());
            actual.setTasaVenta(tasa.venta());
            repo.save(actual);
            return EstadoSincronizacionTipoCambio.ACTUALIZADO;
        }
        log.info("Registrando tipo de cambio {} de {} para {}: compra {}, venta {}",
                fuente, moneda, tasa.fecha(), tasa.compra(), tasa.venta());
        repo.save(TipoCambio.builder()
                .monedaOrigen(MonedaFE.CRC)
                .monedaDestino(moneda)
                .fecha(tasa.fecha())
                .tasaCompra(tasa.compra())
                .tasaVenta(tasa.venta())
                .fuente(fuente)
                .activo(true)
                .creadoPor(CREADO_POR_SINCRONIZACION)
                .build());
        return EstadoSincronizacionTipoCambio.CREADO;
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
