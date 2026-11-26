package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarLineaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TratamientoIvaCompra;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TarifaIva;
import api.astro.whats_orders_manager.modules.proveedores.enums.RegimenHacienda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Validaciones de los datos del comprobante del proveedor y de las líneas de la CxP manual (T-052, ADR-066 §1-2; T-054,
 * ADR-067 §3). Es el único lugar de estas reglas: la creación de la CxP la usa y una futura edición debe reutilizarla.
 *
 * Los errores son {@link IllegalArgumentException} con mensaje fijo (400). Que el IVA de una línea difiera de la base por
 * la tarifa (más de la tolerancia de 0,01) no es un error: {@link #advertencias(CuentaPorPagarDTO)} lo informa. Que la
 * cuenta de cada línea sea una hoja válida lo valida {@code AsientoContableService.validarContrapartidaCxpManual}, y que
 * exista el porcentaje de prorrata del año lo valida el servicio (409).
 */
@Component
public class ComprobanteProveedorValidator {

    public static final String MSG_NUMERO = "El número del comprobante del proveedor es obligatorio";
    public static final String MSG_NUMERO_LARGO = "El número del comprobante del proveedor no puede exceder 50 caracteres";
    public static final String MSG_FECHA = "La fecha de emisión del comprobante es obligatoria";
    public static final String MSG_FECHA_FUTURA = "La fecha de emisión del comprobante no puede ser futura";
    public static final String MSG_CLAVE = "La clave del comprobante debe tener 50 dígitos";
    public static final String MSG_LINEAS = "La cuenta por pagar debe tener al menos una línea";
    public static final String MSG_LINEA = "Cada línea requiere una base mayor a 0 y un IVA no negativo";
    public static final String MSG_LINEA_CUENTA = "Cada línea requiere una cuenta contable";
    public static final String MSG_TARIFA = "La tarifa de IVA de una línea no es válida";
    public static final String MSG_TRATAMIENTO = "Cada línea requiere un tratamiento de IVA";
    public static final String MSG_VENCIMIENTO = "La fecha de vencimiento no puede ser anterior a la fecha del comprobante";
    public static final String MSG_LIMITE = "La tarifa límite de una línea limitada debe ser mayor a 0 y menor que su tarifa";

    /** Tolerancia de redondeo entre el IVA de una línea y base × tarifa. */
    public static final BigDecimal TOLERANCIA = new BigDecimal("0.01");
    private static final int LARGO_MAXIMO_NUMERO = 50;

    private final Clock clock;

    public ComprobanteProveedorValidator(Clock clock) {
        this.clock = clock;
    }

    /**
     * Valida número, fecha, clave y líneas (al menos una; cuenta, base mayor a 0, IVA no negativo, tarifa del catálogo,
     * tratamiento y, con LIMITADO, una tarifa límite mayor a 0 y menor que la tarifa).
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
        List<CuentaPorPagarLineaDTO> lineas = dto.getLineas();
        if (lineas == null || lineas.isEmpty()) {
            throw new IllegalArgumentException(MSG_LINEAS);
        }
        for (CuentaPorPagarLineaDTO linea : lineas) {
            if (linea == null || linea.getBase() == null || linea.getIva() == null
                || linea.getBase().signum() <= 0 || linea.getIva().signum() < 0) {
                throw new IllegalArgumentException(MSG_LINEA);
            }
            if (linea.getCuentaContableId() == null) {
                throw new IllegalArgumentException(MSG_LINEA_CUENTA);
            }
            TarifaIva tarifa = TarifaIva.deCodigo(linea.getCodigoTarifa()).orElse(null);
            if (tarifa == null) {
                throw new IllegalArgumentException(MSG_TARIFA);
            }
            if (linea.getTratamiento() == null) {
                throw new IllegalArgumentException(MSG_TRATAMIENTO);
            }
            if (linea.getTratamiento() == TratamientoIvaCompra.LIMITADO) {
                BigDecimal limite = linea.getTarifaLimite();
                if (limite == null || limite.signum() <= 0 || limite.compareTo(tarifa.getPorcentaje()) >= 0) {
                    throw new IllegalArgumentException(MSG_LIMITE);
                }
            }
        }
    }

    /**
     * Fecha de vencimiento de la CxP (T-053, criterio 15): la enviada se respeta siempre que no sea anterior a la fecha del
     * comprobante; si no viene, es la fecha del comprobante más los días de crédito del proveedor (sin días, o 0: de
     * contado, la fecha del comprobante).
     *
     * @throws IllegalArgumentException con {@link #MSG_VENCIMIENTO} si la enviada es anterior a la del comprobante
     */
    public LocalDate resolverVencimiento(LocalDate fechaComprobante, LocalDate enviada, Integer diasCredito) {
        if (enviada != null) {
            if (enviada.isBefore(fechaComprobante)) {
                throw new IllegalArgumentException(MSG_VENCIMIENTO);
            }
            return enviada;
        }
        return fechaComprobante.plusDays(diasCredito != null && diasCredito > 0 ? diasCredito : 0);
    }

    /** Avisos (no errores): líneas cuyo IVA difiere de base × tarifa por más de 0,01. Vacío si todo cuadra. */
    public List<String> advertencias(CuentaPorPagarDTO dto) {
        return advertencias(dto, null);
    }

    /**
     * Como {@link #advertencias(CuentaPorPagarDTO)} pero, con un proveedor SIMPLIFICADO (T-058, ADR-074 §5-6), no avisa de la
     * diferencia entre el IVA y base × tarifa: el proveedor no traslada IVA, así que IVA 0 con tarifa del bien 13 % es lo correcto.
     */
    public List<String> advertencias(CuentaPorPagarDTO dto, RegimenHacienda regimenProveedor) {
        List<String> avisos = new ArrayList<>();
        List<CuentaPorPagarLineaDTO> lineas = dto.getLineas();
        if (lineas == null || regimenProveedor == RegimenHacienda.SIMPLIFICADO) {
            return avisos;
        }
        for (int i = 0; i < lineas.size(); i++) {
            CuentaPorPagarLineaDTO linea = lineas.get(i);
            if (linea == null || linea.getBase() == null || linea.getIva() == null) {
                continue;
            }
            TarifaIva tarifa = TarifaIva.deCodigo(linea.getCodigoTarifa()).orElse(null);
            if (tarifa == null) {
                continue;
            }
            BigDecimal esperado = linea.getBase().multiply(tarifa.getPorcentaje())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (linea.getIva().subtract(esperado).abs().compareTo(TOLERANCIA) > 0) {
                avisos.add(String.format("La línea %d: el IVA %s difiere de la base por la tarifa (%s)",
                    i + 1, linea.getIva().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    esperado.toPlainString()));
            }
        }
        return avisos;
    }
}
