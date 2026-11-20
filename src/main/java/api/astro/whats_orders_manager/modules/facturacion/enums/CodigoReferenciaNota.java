package api.astro.whats_orders_manager.modules.facturacion.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * Códigos de referencia de la nota 9 del Anexo «Anexos y Estructuras v4.4» (bitácora al 22/04/2026), campo
 * InformacionReferencia/Codigo. El código 03 no existe.
 *
 * <ul>
 *   <li>{@code validoEnNotaCredito}: 07, 15 y 16 son códigos del comprobante sustituto y 17 es exclusivo del recibo
 *       electrónico de pago; ninguno se usa al emitir una nota de crédito (T-032, criterio 24).</li>
 *   <li>{@code periodoAtribucion}: atribución por defecto del IVA (ADR-050 §2, que reemplaza el §4 de ADR-037).
 *       01, 02, 06, 12 y la sustituta 07 → mes de emisión; 13, 14 y la sustituta 15 → mes de la factura original. No
 *       decide la fecha del asiento, que es siempre la fecha del documento. El 15 puede cambiarse por configuración
 *       ({@code AtribucionFiscalService}). El resto no tiene regla (null).</li>
 *   <li>{@code habilitadoEnT032}: se emiten la nota total (01 y 13, T-032) y la parcial (02 y 14, T-047, ADR-051);
 *       el resto llega en fases posteriores.</li>
 * </ul>
 */
@Getter
public enum CodigoReferenciaNota {

    ANULA_DOCUMENTO("01", "Anula documento de referencia", true, PeriodoAtribucion.EMISION, true),
    CORRIGE_MONTO("02", "Corrige monto", true, PeriodoAtribucion.EMISION, true),
    REFERENCIA_OTRO_DOCUMENTO("04", "Referencia a otro documento", true, null, false),
    SUSTITUYE_PROVISIONAL_CONTINGENCIA("05", "Sustituye comprobante provisional por contingencia", true, null, false),
    DEVOLUCION_MERCANCIA("06", "Devolución de mercancía", true, PeriodoAtribucion.EMISION, false),
    SUSTITUYE_COMPROBANTE("07", "Sustituye comprobante electrónico", false, PeriodoAtribucion.EMISION, false),
    FACTURA_ENDOSADA("08", "Factura endosada", true, null, false),
    NOTA_CREDITO_FINANCIERA("09", "Nota de crédito financiera", true, null, false),
    NOTA_DEBITO_FINANCIERA("10", "Nota de débito financiera", true, null, false),
    PROVEEDOR_NO_DOMICILIADO("11", "Proveedor no domiciliado", true, null, false),
    CREDITO_EXONERACION_POSTERIOR("12", "Nota de crédito financiera por exoneración posterior a la facturación", true,
            PeriodoAtribucion.EMISION, false),
    ANULA_DOCUMENTO_ERROR_MATERIAL("13", "Anula documento de referencia por error material", true,
            PeriodoAtribucion.ORIGINAL, true),
    CORRIGE_MONTO_ERROR_MATERIAL("14", "Corrige monto por error material", true,
            PeriodoAtribucion.ORIGINAL, true),
    SUSTITUYE_COMPROBANTE_ERROR_MATERIAL("15", "Sustituye comprobante electrónico por error material", false,
            PeriodoAtribucion.ORIGINAL, false),
    SUSTITUYE_COMPROBANTE_RECHAZADO("16", "Sustituye comprobante electrónico rechazado", false, null, false),
    PAGO_A_COMPROBANTE("17", "Pago a comprobante electrónico", false, null, false),
    OTROS("99", "Otros", true, null, false);

    private final String codigo;
    private final String nombre;
    private final boolean validoEnNotaCredito;
    private final PeriodoAtribucion periodoAtribucion;
    private final boolean habilitadoEnT032;

    CodigoReferenciaNota(String codigo, String nombre, boolean validoEnNotaCredito, PeriodoAtribucion periodoAtribucion,
                         boolean habilitadoEnT032) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.validoEnNotaCredito = validoEnNotaCredito;
        this.periodoAtribucion = periodoAtribucion;
        this.habilitadoEnT032 = habilitadoEnT032;
    }

    /** Nota total: 01 y 13 (acreditan toda la factura y la anulan). */
    public boolean esTotal() {
        return this == ANULA_DOCUMENTO || this == ANULA_DOCUMENTO_ERROR_MATERIAL;
    }

    /** Nota parcial: 02 y 14 (acreditan un monto por línea con IVA incluido; la factura no se anula). */
    public boolean esParcial() {
        return this == CORRIGE_MONTO || this == CORRIGE_MONTO_ERROR_MATERIAL;
    }

    /** Código de la nota 9, o vacío si no existe (p. ej. "03"). */
    public static Optional<CodigoReferenciaNota> deCodigo(String codigo) {
        return Arrays.stream(values()).filter(c -> c.codigo.equals(codigo)).findFirst();
    }
}
