package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarIvaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TarifaIva;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Validaciones de los datos del comprobante del proveedor y de su desglose de IVA (T-052, ADR-066 §1-2). Es el único
 * lugar de estas reglas: la creación de la CxP la usa y una futura edición debe reutilizarla.
 *
 * Los errores son {@link IllegalArgumentException} con mensaje fijo (400). Que el IVA de una fila difiera de la base por
 * la tarifa (más de la tolerancia de 0,01) no es un error: {@link #advertencias(CuentaPorPagarDTO)} lo informa.
 */
@Component
public class ComprobanteProveedorValidator {

    public static final String MSG_NUMERO = "El número del comprobante del proveedor es obligatorio";
    public static final String MSG_NUMERO_LARGO = "El número del comprobante del proveedor no puede exceder 50 caracteres";
    public static final String MSG_FECHA = "La fecha de emisión del comprobante es obligatoria";
    public static final String MSG_FECHA_FUTURA = "La fecha de emisión del comprobante no puede ser futura";
    public static final String MSG_CLAVE = "La clave del comprobante debe tener 50 dígitos";
    public static final String MSG_DESGLOSE = "El desglose de IVA debe tener al menos una fila";
    public static final String MSG_FILA = "Cada fila del desglose requiere base e IVA no negativos";
    public static final String MSG_TARIFA = "La tarifa de IVA del desglose no es válida";
    public static final String MSG_SUMA_BASES = "La suma de las bases del desglose no coincide con el subtotal";
    public static final String MSG_SUMA_IVA = "La suma del IVA del desglose no coincide con el impuesto";

    /** Tolerancia de redondeo entre sumas y entre el IVA de una fila y base × tarifa. */
    public static final BigDecimal TOLERANCIA = new BigDecimal("0.01");
    private static final int LARGO_MAXIMO_NUMERO = 50;

    private final Clock clock;

    public ComprobanteProveedorValidator(Clock clock) {
        this.clock = clock;
    }

    /**
     * Valida número, fecha, clave y desglose. Si subtotal o impuesto vienen en el DTO, deben coincidir con las sumas del
     * desglose (tolerancia 0,01).
     *
     * @throws IllegalArgumentException con el mensaje fijo de la primera regla que falla
     */
    public void validar(CuentaPorPagarDTO dto) {
        String numero = dto.getNumeroComprobanteProveedor();
        if (numero == null || numero.isBlank()) {
            throw new IllegalArgumentException(MSG_NUMERO);
        }
        if (numero.trim().length() > LARGO_MAXIMO_NUMERO) {
            throw new IllegalArgumentException(MSG_NUMERO_LARGO);
        }
        LocalDate fecha = dto.getFechaComprobante();
        if (fecha == null) {
            throw new IllegalArgumentException(MSG_FECHA);
        }
        if (fecha.isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException(MSG_FECHA_FUTURA);
        }
        String clave = dto.getClaveComprobante();
        if (clave != null && !clave.isBlank() && !clave.trim().matches("\\d{50}")) {
            throw new IllegalArgumentException(MSG_CLAVE);
        }
        List<CuentaPorPagarIvaDTO> desglose = dto.getDesglose();
        if (desglose == null || desglose.isEmpty()) {
            throw new IllegalArgumentException(MSG_DESGLOSE);
        }
        BigDecimal sumaBases = BigDecimal.ZERO;
        BigDecimal sumaIva = BigDecimal.ZERO;
        for (CuentaPorPagarIvaDTO fila : desglose) {
            if (fila == null || fila.getBase() == null || fila.getIva() == null
                || fila.getBase().signum() < 0 || fila.getIva().signum() < 0) {
                throw new IllegalArgumentException(MSG_FILA);
            }
            if (TarifaIva.deCodigo(fila.getCodigoTarifa()).isEmpty()) {
                throw new IllegalArgumentException(MSG_TARIFA);
            }
            sumaBases = sumaBases.add(fila.getBase());
            sumaIva = sumaIva.add(fila.getIva());
        }
        if (dto.getSubtotal() != null && sumaBases.subtract(dto.getSubtotal()).abs().compareTo(TOLERANCIA) > 0) {
            throw new IllegalArgumentException(MSG_SUMA_BASES);
        }
        if (dto.getImpuesto() != null && sumaIva.subtract(dto.getImpuesto()).abs().compareTo(TOLERANCIA) > 0) {
            throw new IllegalArgumentException(MSG_SUMA_IVA);
        }
    }

    /** Avisos (no errores): filas cuyo IVA difiere de base × tarifa por más de 0,01. Vacío si todo cuadra. */
    public List<String> advertencias(CuentaPorPagarDTO dto) {
        List<String> avisos = new ArrayList<>();
        List<CuentaPorPagarIvaDTO> desglose = dto.getDesglose();
        if (desglose == null) {
            return avisos;
        }
        for (int i = 0; i < desglose.size(); i++) {
            CuentaPorPagarIvaDTO fila = desglose.get(i);
            if (fila == null || fila.getBase() == null || fila.getIva() == null) {
                continue;
            }
            TarifaIva tarifa = TarifaIva.deCodigo(fila.getCodigoTarifa()).orElse(null);
            if (tarifa == null) {
                continue;
            }
            BigDecimal esperado = fila.getBase().multiply(tarifa.getPorcentaje())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (fila.getIva().subtract(esperado).abs().compareTo(TOLERANCIA) > 0) {
                avisos.add(String.format("La fila %d del desglose: el IVA %s difiere de la base por la tarifa (%s)",
                    i + 1, fila.getIva().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    esperado.toPlainString()));
            }
        }
        return avisos;
    }
}
