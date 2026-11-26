package api.astro.whats_orders_manager.modules.politicas;

import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarLineaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAceptacionComprobante;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TratamientoIvaCompra;
import api.astro.whats_orders_manager.modules.proveedores.enums.RegimenHacienda;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenNoDefinidoException;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Política de IVA de compra (T-058, ADR-074 §1, §3, §5-6): único punto de decisión del tratamiento de IVA de una compra.
 * Decide con el régimen de la empresa en la fecha del comprobante ({@link RegimenTributarioService#vigenteEn}) y con el
 * régimen que el proveedor tenía al registrar:
 * <ul>
 *   <li>Empresa SIMPLIFICADO: toda línea es NO_ACREDITABLE (no hay crédito fiscal en el RTS); cualquier otro tratamiento es
 *       un error 400 con mensaje fijo. La CxP no lleva aceptación del comprobante ({@code NO_APLICA}).</li>
 *   <li>Proveedor SIMPLIFICADO (con la empresa en cualquier régimen): el proveedor no traslada IVA, así que el IVA de cada
 *       línea debe ser 0 (400 si no), el tratamiento se fija en NO_ACREDITABLE y la tarifa del bien (codigo_tarifa) se
 *       conserva: no se exige código 10 ni 11.</li>
 *   <li>Empresa GENERAL con proveedor que no es SIMPLIFICADO: sin cambios respecto de T-054.</li>
 * </ul>
 * Una fecha sin tramo propaga {@link RegimenNoDefinidoException} (409): no hay régimen por defecto. Las reglas viven solo
 * aquí; la pantalla las recibe de {@link #reglaPara} y la compra de contado reutiliza esta misma política.
 */
@Component
@RequiredArgsConstructor
public class PoliticaIvaCompra {

    public static final String MSG_SOLO_NO_ACREDITABLE =
        "En el régimen simplificado el IVA de las compras no es acreditable: todas las líneas deben ser NO_ACREDITABLE";
    public static final String MSG_PROVEEDOR_SIN_IVA =
        "Un proveedor del régimen simplificado no traslada IVA: el IVA de cada línea debe ser 0";
    static final String TEXTO_EMPRESA_SIMPLIFICADO =
        "Régimen simplificado: el IVA de las compras no es acreditable y se registra como costo de la línea";
    static final String TEXTO_PROVEEDOR_SIMPLIFICADO =
        "Proveedor del régimen simplificado: no traslada IVA (IVA 0) y la compra no genera crédito fiscal";

    /**
     * Regla para la pantalla. {@code tratamientoFijo} es el único tratamiento posible o null si hay varios;
     * {@code ivaTrasladadoCero} exige IVA 0 en cada línea; {@code mensaje} es un texto corto para el usuario (vacío si no
     * hay restricción).
     */
    public record ReglaIvaCompra(Regimen regimen, List<TratamientoIvaCompra> tratamientosAdmitidos,
                                 TratamientoIvaCompra tratamientoFijo, boolean ivaTrasladadoCero, String mensaje) {
    }

    private final RegimenTributarioService regimenService;

    /**
     * Regla vigente para una fecha de comprobante y el régimen del proveedor (null = no es SIMPLIFICADO).
     *
     * @throws RegimenNoDefinidoException la fecha no tiene tramo (409)
     */
    public ReglaIvaCompra reglaPara(LocalDate fechaComprobante, RegimenHacienda regimenProveedor) {
        Regimen empresa = regimenService.vigenteEn(fechaComprobante).getRegimen();
        boolean proveedorSimplificado = regimenProveedor == RegimenHacienda.SIMPLIFICADO;
        if (empresa == Regimen.SIMPLIFICADO) {
            return new ReglaIvaCompra(empresa, List.of(TratamientoIvaCompra.NO_ACREDITABLE), TratamientoIvaCompra.NO_ACREDITABLE,
                proveedorSimplificado, proveedorSimplificado ? TEXTO_PROVEEDOR_SIMPLIFICADO : TEXTO_EMPRESA_SIMPLIFICADO);
        }
        if (proveedorSimplificado) {
            return new ReglaIvaCompra(empresa, List.of(TratamientoIvaCompra.NO_ACREDITABLE), TratamientoIvaCompra.NO_ACREDITABLE,
                true, TEXTO_PROVEEDOR_SIMPLIFICADO);
        }
        return new ReglaIvaCompra(empresa, List.of(TratamientoIvaCompra.values()), null, false, "");
    }

    /**
     * Valida las líneas contra la regla y fija el tratamiento cuando la regla lo fija (modifica el tratamiento de cada
     * línea). Con la empresa en SIMPLIFICADO una línea con otro tratamiento es error; un tratamiento nulo se completa.
     *
     * @throws IllegalArgumentException con mensaje fijo (400)
     * @throws RegimenNoDefinidoException la fecha no tiene tramo (409)
     */
    public void aplicar(LocalDate fechaComprobante, RegimenHacienda regimenProveedor, List<CuentaPorPagarLineaDTO> lineas) {
        ReglaIvaCompra regla = reglaPara(fechaComprobante, regimenProveedor);
        if (regla.regimen() == Regimen.SIMPLIFICADO) {
            for (CuentaPorPagarLineaDTO l : lineas) {
                if (l.getTratamiento() != null && l.getTratamiento() != TratamientoIvaCompra.NO_ACREDITABLE) {
                    throw new IllegalArgumentException(MSG_SOLO_NO_ACREDITABLE);
                }
            }
        }
        if (regla.ivaTrasladadoCero()) {
            for (CuentaPorPagarLineaDTO l : lineas) {
                if (l.getIva() != null && l.getIva().signum() > 0) {
                    throw new IllegalArgumentException(MSG_PROVEEDOR_SIN_IVA);
                }
            }
        }
        if (regla.tratamientoFijo() != null) {
            lineas.forEach(l -> l.setTratamiento(regla.tratamientoFijo()));
        }
    }

    /** Estado de aceptación con que nace una CxP de esa fecha: NO_APLICA en SIMPLIFICADO, PENDIENTE en GENERAL. */
    public EstadoAceptacionComprobante estadoAceptacionInicial(LocalDate fechaComprobante) {
        return regimenService.vigenteEn(fechaComprobante).getRegimen() == Regimen.SIMPLIFICADO
            ? EstadoAceptacionComprobante.NO_APLICA : EstadoAceptacionComprobante.PENDIENTE;
    }
}
