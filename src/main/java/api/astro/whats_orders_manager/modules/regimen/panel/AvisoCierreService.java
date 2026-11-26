package api.astro.whats_orders_manager.modules.regimen.panel;

import api.astro.whats_orders_manager.modules.facturacion.electronica.util.HoraFiscal;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenNoDefinidoException;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

/**
 * Aviso de cierre del régimen general (T-057, ADR-076 §4). Del 20-12 al 31-12 (fecha de Costa Rica del Clock), si en lo que
 * queda del año empieza un tramo SIMPLIFICADO (normalmente el del 01-01 siguiente), el panel de Facturación cuenta las
 * facturas GENERAL del período que el ERP ya no podrá emitir porque nadie las ha emitido todavía.
 *
 * <p>Período = desde el inicio del tramo GENERAL que rige hoy hasta hoy, por fecha de emisión de la factura. Se cuentan las
 * facturas no anuladas, selladas GENERAL, sin comprobante vigente del ERP en un estado distinto de RECHAZADO (uno RECHAZADO cuenta como pendiente) y sin la clave de un
 * comprobante emitido fuera del ERP. El enlace lleva la misma definición a la lista ({@code /facturas?estadoFE=pendiente_cierre}
 * con {@code desdeCierre} y {@code hastaCierre}), de modo que lista exactamente lo contado. Nunca lanza: si no se puede
 * decidir, no hay aviso.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvisoCierreService {

    /** Primer día del rango del aviso (20 de diciembre). */
    static final int DIA_INICIO_AVISO = 20;

    private final RegimenTributarioService regimenes;
    private final FacturaRepository facturaRepository;
    private final Clock clock;

    /** El período de facturas GENERAL: {@code [desde, hasta]} cerrado. */
    public record Periodo(LocalDate desde, LocalDate hasta) {
    }

    /** Lo que muestra el panel: cuántas facturas, el período y el enlace a la lista filtrada. */
    public record AvisoCierre(long cantidad, LocalDate desde, LocalDate hasta, String enlace) {
    }

    /** El aviso de hoy, o vacío si hoy no está entre el 20-12 y el 31-12 o el tramo siguiente no es SIMPLIFICADO. */
    @Transactional(readOnly = true)
    public Optional<AvisoCierre> obtener() {
        try {
            LocalDate hoy = HoraFiscal.ahora(clock).toLocalDate();
            if (hoy.getMonth() != Month.DECEMBER || hoy.getDayOfMonth() < DIA_INICIO_AVISO) {
                return Optional.empty();
            }
            LocalDate finDelAnio = LocalDate.of(hoy.getYear(), Month.DECEMBER, 31);
            if (!regimenes.empiezaTramo(Regimen.SIMPLIFICADO, hoy, finDelAnio.plusDays(1))) {
                return Optional.empty();
            }
            Optional<Periodo> periodo = periodoGeneral(hoy);
            if (periodo.isEmpty()) {
                return Optional.empty();
            }
            Periodo p = periodo.get();
            long cantidad = facturaRepository.contarPendientesCierre(p.desde(), p.hasta());
            String enlace = "/facturas?estadoFE=pendiente_cierre&desdeCierre=" + p.desde() + "&hastaCierre=" + p.hasta();
            return Optional.of(new AvisoCierre(cantidad, p.desde(), p.hasta(), enlace));
        } catch (RuntimeException e) {
            log.warn("Panel de Facturación: no se pudo calcular el aviso de cierre ({})", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /** Inicio del tramo GENERAL que rige en {@code hoy} hasta {@code hoy}; vacío si hoy no rige GENERAL o no hay tramo. */
    private Optional<Periodo> periodoGeneral(LocalDate hoy) {
        try {
            var tramo = regimenes.vigenteEn(hoy);
            return tramo.getRegimen() == Regimen.GENERAL
                ? Optional.of(new Periodo(tramo.getVigenteDesde(), hoy)) : Optional.empty();
        } catch (RegimenNoDefinidoException e) {
            return Optional.empty();
        }
    }
}
