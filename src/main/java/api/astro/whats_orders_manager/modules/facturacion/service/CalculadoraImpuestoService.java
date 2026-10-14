package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.configuracion.service.ParametroSistemaService;
import api.astro.whats_orders_manager.modules.facturacion.dto.DesgloseImpuesto;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Único punto de cálculo del IVA de ventas (ADR-009).
 *
 * Lo usan la factura (LineaFacturaServiceImpl / FacturaServiceImpl), y por tanto el asiento
 * de venta (ADR-008), y el XML de facturación electrónica (XmlGeneratorServiceImpl), de modo
 * que los tres coinciden al céntimo.
 *
 * Regla por línea:
 * <ul>
 *   <li>Precio con IVA incluido: base = importe / (1 + tarifa/100) HALF_UP 2 decimales;
 *       impuesto = importe − base (base + impuesto = importe exacto).</li>
 *   <li>Precio sin IVA incluido: base = importe; impuesto = base × tarifa/100 HALF_UP 2 decimales.</li>
 *   <li>Exento / no gravado (tarifa 0): impuesto = 0.</li>
 * </ul>
 *
 * Tarifa por producto: gravado y porcentaje se resuelven desde el ArticuloMaestro con
 * respaldo en los campos (deprecados) del Producto. Si el producto es gravado y no define
 * porcentaje, se usa la tarifa general del parámetro de sistema {@value #CLAVE_TARIFA_GENERAL}.
 */
@Slf4j
@Service
public class CalculadoraImpuestoService {

    /** Parámetro de sistema con la tarifa general de IVA (Costa Rica: 13 %, código Hacienda 08). */
    public static final String CLAVE_TARIFA_GENERAL = "FACTURA_IVA_PORCENTAJE";

    /** Respaldo si el parámetro no existe o no es válido (ADR-009: tarifa general 13 %). */
    static final BigDecimal TARIFA_GENERAL_POR_DEFECTO = new BigDecimal("13");

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final int DECIMALES = 2;

    private final ParametroSistemaService parametroSistemaService;

    public CalculadoraImpuestoService(ParametroSistemaService parametroSistemaService) {
        this.parametroSistemaService = parametroSistemaService;
    }

    /**
     * Desglosa un importe en base e impuesto.
     *
     * @param importe          Importe de la línea (se normaliza a 2 decimales HALF_UP)
     * @param tarifa           Tarifa en porcentaje (null o 0 = exento)
     * @param precioIncluyeIva true si el importe ya incluye el IVA
     */
    public DesgloseImpuesto desglosar(BigDecimal importe, BigDecimal tarifa, boolean precioIncluyeIva) {
        BigDecimal monto = (importe != null ? importe : BigDecimal.ZERO).setScale(DECIMALES, RoundingMode.HALF_UP);
        if (tarifa == null || tarifa.signum() <= 0) {
            return new DesgloseImpuesto(monto, BigDecimal.ZERO.setScale(DECIMALES), BigDecimal.ZERO);
        }
        if (precioIncluyeIva) {
            BigDecimal divisor = BigDecimal.ONE.add(tarifa.divide(CIEN));
            BigDecimal base = monto.divide(divisor, DECIMALES, RoundingMode.HALF_UP);
            return new DesgloseImpuesto(base, monto.subtract(base), tarifa);
        }
        BigDecimal impuesto = monto.multiply(tarifa).divide(CIEN, DECIMALES, RoundingMode.HALF_UP);
        return new DesgloseImpuesto(monto, impuesto, tarifa);
    }

    /**
     * Desglosa el importe de una línea de venta según el producto: tarifa resuelta por
     * producto y su indicador precioIncluyeIva (null se trata como true, valor por defecto ADR-009).
     */
    public DesgloseImpuesto desglosarLinea(Producto producto, BigDecimal importe) {
        boolean incluyeIva = producto == null || !Boolean.FALSE.equals(producto.getPrecioIncluyeIva());
        return desglosar(importe, resolverTarifa(producto), incluyeIva);
    }

    /**
     * Tarifa de IVA del producto: 0 si no es gravado; su porcentaje si lo define;
     * la tarifa general en otro caso. Un producto sin indicador de gravado (null) o
     * inexistente se considera gravado con la tarifa general (ADR-009).
     */
    public BigDecimal resolverTarifa(Producto producto) {
        if (producto == null) {
            return getTarifaGeneral();
        }
        Boolean gravado = resolveGravado(producto);
        if (Boolean.FALSE.equals(gravado)) {
            return BigDecimal.ZERO;
        }
        return Optional.ofNullable(resolvePorcentajeImpuesto(producto)).orElseGet(this::getTarifaGeneral);
    }

    /** Tarifa general de IVA (parámetro de sistema, respaldo 13 %). */
    public BigDecimal getTarifaGeneral() {
        BigDecimal tarifa = parametroSistemaService != null
                ? parametroSistemaService.obtenerValorDecimal(CLAVE_TARIFA_GENERAL, null)
                : null;
        if (tarifa == null || tarifa.signum() < 0 || tarifa.compareTo(CIEN) > 0) {
            log.warn("Parámetro {} ausente o inválido ({}); se usa la tarifa general {} %",
                    CLAVE_TARIFA_GENERAL, tarifa, TARIFA_GENERAL_POR_DEFECTO);
            return TARIFA_GENERAL_POR_DEFECTO;
        }
        return tarifa;
    }

    /** Gravado: se lee del maestro y, si no lo define, del campo deprecado del Producto. */
    @SuppressWarnings("deprecation")
    public static Boolean resolveGravado(Producto producto) {
        return Optional.ofNullable(producto.getArticuloMaestro())
                .map(ArticuloMaestro::getGravado)
                .orElse(producto.getGravado());
    }

    /** Porcentaje de impuesto: se lee del maestro y, si no lo define, del campo deprecado del Producto. */
    @SuppressWarnings("deprecation")
    public static BigDecimal resolvePorcentajeImpuesto(Producto producto) {
        return Optional.ofNullable(producto.getArticuloMaestro())
                .map(ArticuloMaestro::getPorcentajeImpuesto)
                .orElse(producto.getPorcentajeImpuesto());
    }
}
