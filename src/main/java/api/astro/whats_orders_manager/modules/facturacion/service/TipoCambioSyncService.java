package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.dto.ResultadoSincronizacionTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.dto.TasaDelDia;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoSincronizacionTipoCambio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Sincronización del tipo de cambio del día desde el proveedor automático (T-008, ADR-004 / ADR-011).
 *
 * Para cada moneda de {@code tipo-cambio.sync.monedas} (por defecto solo USD) consulta el proveedor y
 * guarda la tasa con la fecha que este informa, respetando la prioridad de las tasas MANUAL
 * ({@link TipoCambioService#registrarDesdeProveedor}). Un fallo del proveedor se registra como WARN,
 * no escribe nada y no detiene las demás monedas; este método nunca propaga excepciones.
 * La usa el job diario ({@code TipoCambioSyncScheduler}) y la sincronización manual del administrador.
 */
@Service
@Slf4j
public class TipoCambioSyncService {

    private final ProveedorTipoCambio proveedor;
    private final TipoCambioService tipoCambioService;
    private final List<MonedaFE> monedas;

    public TipoCambioSyncService(ProveedorTipoCambio proveedor,
                                 TipoCambioService tipoCambioService,
                                 @Value("${tipo-cambio.sync.monedas:USD}") String monedas) {
        this.proveedor = proveedor;
        this.tipoCambioService = tipoCambioService;
        this.monedas = parsearMonedas(monedas);
    }

    public List<MonedaFE> getMonedas() {
        return monedas;
    }

    public List<ResultadoSincronizacionTipoCambio> sincronizar() {
        log.info("Sincronizando tipo de cambio ({}) para {}", proveedor.getFuente(), monedas);
        List<ResultadoSincronizacionTipoCambio> resultados = new ArrayList<>();
        for (MonedaFE moneda : monedas) {
            resultados.add(sincronizar(moneda));
        }
        return resultados;
    }

    private ResultadoSincronizacionTipoCambio sincronizar(MonedaFE moneda) {
        TasaDelDia tasa;
        try {
            tasa = proveedor.obtenerTasaDelDia(moneda);
        } catch (RuntimeException e) {
            return error(moneda, e);
        }
        try {
            EstadoSincronizacionTipoCambio estado =
                    tipoCambioService.registrarDesdeProveedor(moneda, tasa, proveedor.getFuente());
            return new ResultadoSincronizacionTipoCambio(moneda.getCodigo(), estado, tasa.fecha(),
                    tasa.compra(), tasa.venta(), mensaje(estado));
        } catch (RuntimeException e) {
            return error(moneda, e);
        }
    }

    private ResultadoSincronizacionTipoCambio error(MonedaFE moneda, RuntimeException e) {
        log.warn("No se pudo sincronizar el tipo de cambio de {}: {}", moneda.getCodigo(), e.getMessage());
        log.debug("Detalle del fallo de sincronización de {}", moneda.getCodigo(), e);
        return new ResultadoSincronizacionTipoCambio(moneda.getCodigo(), EstadoSincronizacionTipoCambio.ERROR,
                null, null, null, "No se pudo obtener el tipo de cambio: " + e.getMessage());
    }

    private static String mensaje(EstadoSincronizacionTipoCambio estado) {
        return switch (estado) {
            case CREADO -> "Tipo de cambio registrado";
            case ACTUALIZADO -> "Tipo de cambio actualizado";
            case SIN_CAMBIOS -> "El tipo de cambio ya estaba registrado con el mismo valor";
            case CONSERVADO -> "Ya existe un tipo de cambio de otra fuente (p. ej. manual) para ese día; se conserva";
            case ERROR -> "No se pudo obtener el tipo de cambio";
        };
    }

    /** "USD,EUR" → [USD, EUR]; ignora vacíos, duplicados, CRC y códigos desconocidos; vacío → [USD]. */
    private static List<MonedaFE> parsearMonedas(String configuradas) {
        Set<MonedaFE> resultado = new LinkedHashSet<>();
        if (configuradas != null) {
            Arrays.stream(configuradas.split(","))
                    .map(String::trim)
                    .filter(codigo -> !codigo.isEmpty())
                    .forEach(codigo -> {
                        MonedaFE moneda = MonedaFE.fromCodigo(codigo.toUpperCase());
                        if (moneda == null || moneda == MonedaFE.CRC) {
                            log.warn("Moneda '{}' ignorada en tipo-cambio.sync.monedas", codigo);
                        } else {
                            resultado.add(moneda);
                        }
                    });
        }
        if (resultado.isEmpty()) {
            resultado.add(MonedaFE.USD);
        }
        return List.copyOf(resultado);
    }
}
