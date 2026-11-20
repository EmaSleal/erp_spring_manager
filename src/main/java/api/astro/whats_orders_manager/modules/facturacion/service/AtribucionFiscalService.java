package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.enums.CodigoReferenciaNota;
import api.astro.whats_orders_manager.modules.facturacion.enums.PeriodoAtribucion;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Regla única de la fecha contable y del período fiscal de atribución de una nota de crédito o débito o de una factura
 * sustituta (ADR-050, que reemplaza el §4 de ADR-037 y de ADR-045).
 *
 * <ul>
 *   <li>Fecha contable: la fecha del documento para todos los códigos ({@link #fechaContable}).</li>
 *   <li>Período fiscal de atribución (mes del IVA): por defecto el de {@link CodigoReferenciaNota#getPeriodoAtribucion()}
 *       (01, 02, 06, 12 y 07 → emisión; 13 y 14 → factura original). El del código 15 es configuración:
 *       {@code facturacion.notas.atribucion.codigo-15} = ORIGINAL | EMISION (por defecto ORIGINAL).</li>
 * </ul>
 *
 * El período se calcula al crear el documento y se guarda: cambiar la propiedad no toca los documentos ya creados.
 */
@Service
public class AtribucionFiscalService {

    public static final String PROPIEDAD_CODIGO_15 = "facturacion.notas.atribucion.codigo-15";
    private static final DateTimeFormatter PERIODO = DateTimeFormatter.ofPattern("yyyy-MM");

    private final PeriodoAtribucion atribucionCodigo15;

    public AtribucionFiscalService(
            @Value("${" + PROPIEDAD_CODIGO_15 + ":ORIGINAL}") String atribucionCodigo15) {
        this.atribucionCodigo15 = parsear(atribucionCodigo15);
    }

    private static PeriodoAtribucion parsear(String valor) {
        try {
            return PeriodoAtribucion.valueOf(valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Valor inválido «" + valor + "» en " + PROPIEDAD_CODIGO_15
                + ": se esperaba ORIGINAL o EMISION");
        }
    }

    /**
     * Único punto de la fecha contable (ADR-050 §1): hoy es la fecha del documento. Cuando exista el cierre de
     * períodos (P-018) este método la reemplazará por el primer período abierto; ningún otro código decide la fecha.
     */
    public LocalDate fechaContable(LocalDate fechaDocumento) {
        if (fechaDocumento == null) {
            throw new IllegalArgumentException("La fecha del documento es obligatoria");
        }
        return fechaDocumento;
    }

    /** Atribución que rige para un código: el valor por defecto del código, salvo el 15 (configurable). */
    public PeriodoAtribucion atribucionDe(CodigoReferenciaNota codigo) {
        if (codigo == CodigoReferenciaNota.SUSTITUYE_COMPROBANTE_ERROR_MATERIAL) {
            return atribucionCodigo15;
        }
        PeriodoAtribucion atribucion = codigo.getPeriodoAtribucion();
        if (atribucion == null) {
            throw new IllegalStateException("El código de referencia " + codigo.getCodigo()
                + " no define el período fiscal de atribución");
        }
        return atribucion;
    }

    /**
     * Mes al que se atribuye el IVA del documento.
     *
     * @param fechaEmisionDocumento    fecha de emisión de la nota o de la sustituta
     * @param fechaEmisionOriginalAceptada fecha de emisión del comprobante ACEPTADO de la factura original
     */
    public YearMonth periodo(CodigoReferenciaNota codigo, LocalDate fechaEmisionDocumento,
                             LocalDateTime fechaEmisionOriginalAceptada) {
        if (atribucionDe(codigo) == PeriodoAtribucion.ORIGINAL) {
            if (fechaEmisionOriginalAceptada == null) {
                throw new IllegalStateException("El código " + codigo.getCodigo() + " se atribuye al mes de la factura "
                    + "original y su comprobante aceptado no tiene fecha de emisión");
            }
            return YearMonth.from(fechaEmisionOriginalAceptada);
        }
        if (fechaEmisionDocumento == null) {
            throw new IllegalArgumentException("La fecha de emisión del documento es obligatoria");
        }
        return YearMonth.from(fechaEmisionDocumento);
    }

    /** {@link #periodo} en el formato guardado, AAAA-MM. */
    public String periodoTexto(CodigoReferenciaNota codigo, LocalDate fechaEmisionDocumento,
                               LocalDateTime fechaEmisionOriginalAceptada) {
        return PERIODO.format(periodo(codigo, fechaEmisionDocumento, fechaEmisionOriginalAceptada));
    }

    /** Formato AAAA-MM de un mes. */
    public static String formatear(YearMonth mes) {
        return PERIODO.format(mes);
    }

    /** Una factura sustituta sin fecha contable o sin período de atribución no se guarda ni se contabiliza. */
    public static void exigirAtribucion(Factura sustituta) {
        if (sustituta.getFacturaSustituida() == null) {
            return;
        }
        if (sustituta.getFechaContable() == null || sustituta.getPeriodoFiscalAtribucion() == null
            || sustituta.getPeriodoFiscalAtribucion().isBlank()) {
            throw new IllegalStateException("La factura sustituta " + sustituta.getNumeroFactura()
                + " debe tener fecha contable y período fiscal de atribución");
        }
    }
}
