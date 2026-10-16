package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.dto.DesgloseImpuesto;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Único punto de cálculo de los montos de una factura a partir de sus líneas (ADR-009, T-024).
 *
 * Lo usan las dos rutas que guardan líneas: FacturaServiceImpl.save (factura con líneas) y
 * LineaFacturaServiceImpl.updateLineas (PUT /lineas-factura/actualizar). Para el mismo conjunto de líneas
 * ambas producen el mismo subtotal de línea y los mismos factura.subtotal/igv/total.
 *
 * <ul>
 *   <li>Importe de línea = cantidad × precio_unitario (precio con IVA incluido), HALF_UP a 2 decimales.
 *       Se ignora cualquier subtotal recibido.</li>
 *   <li>Desglose de IVA por línea con {@link CalculadoraImpuestoService#desglosarLinea} (tarifa del producto).</li>
 *   <li>factura.subtotal = Σ bases, igv = Σ impuestos, total = Σ importes, en la moneda de la factura
 *       (sin conversión, ADR-011).</li>
 * </ul>
 */
@Service
public class TotalesFacturaService {

    private static final int DECIMALES = 2;

    private final CalculadoraImpuestoService calculadoraImpuestoService;
    private final ProductoRepository productoRepository;

    public TotalesFacturaService(CalculadoraImpuestoService calculadoraImpuestoService,
                                 ProductoRepository productoRepository) {
        this.calculadoraImpuestoService = calculadoraImpuestoService;
        this.productoRepository = productoRepository;
    }

    /**
     * Importe de una línea: cantidad × precio unitario, HALF_UP a 2 decimales.
     *
     * @throws IllegalArgumentException si la cantidad no es mayor que cero o el precio es nulo o negativo
     */
    public static BigDecimal importeLinea(Integer cantidad, BigDecimal precioUnitario) {
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad de cada línea debe ser mayor que cero");
        }
        if (precioUnitario == null || precioUnitario.signum() < 0) {
            throw new IllegalArgumentException("El precio unitario de cada línea debe ser mayor o igual a cero");
        }
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad)).setScale(DECIMALES, RoundingMode.HALF_UP);
    }

    /**
     * Recalcula el subtotal de cada línea (cantidad × precio) y fija factura.subtotal, igv y total con las sumas
     * del desglose de esas líneas. El llamador pasa el conjunto completo de líneas de la factura.
     *
     * @return el desglose total aplicado
     * @throws IllegalArgumentException si alguna línea tiene cantidad o precio inválidos
     */
    public DesgloseImpuesto aplicarTotales(Factura factura, Collection<LineaFactura> lineas) {
        DesgloseImpuesto desglose = DesgloseImpuesto.CERO;
        for (LineaFactura linea : lineas) {
            linea.setSubtotal(importeLinea(linea.getCantidad(), linea.getPrecioUnitario()));
            Producto producto = resolverProducto(linea.getProducto());
            desglose = desglose.sumar(calculadoraImpuestoService.desglosarLinea(producto, linea.getSubtotal()));
        }
        factura.setSubtotal(desglose.base());
        factura.setIgv(desglose.impuesto());
        factura.setTotal(desglose.total());
        return desglose;
    }

    /** Producto completo (gravado/tarifa/precioIncluyeIva); el recibido puede traer solo el id. */
    private Producto resolverProducto(Producto producto) {
        if (producto == null || producto.getIdProducto() == null) {
            return producto;
        }
        return productoRepository.findById(producto.getIdProducto()).orElse(producto);
    }
}
