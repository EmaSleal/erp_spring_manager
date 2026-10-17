package api.astro.whats_orders_manager.modules.facturacion.service.impl;

import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.dto.DesgloseImpuesto;
import api.astro.whats_orders_manager.modules.facturacion.dto.TotalesFacturaDTO;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFacturaR;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.LineaFacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.LineaFacturaService;
import api.astro.whats_orders_manager.modules.facturacion.service.TotalesFacturaService;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.impl.UserDetailsServiceImpl;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LineaFacturaServiceImpl implements LineaFacturaService {
    @Autowired
    private LineaFacturaRepository lineaFacturaRepository;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AsientoContableService asientoContableService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private TotalesFacturaService totalesFacturaService;

    @Override
    public List<LineaFactura> findAll() { return lineaFacturaRepository.findAll(); }

    @Override
    public Optional<LineaFactura> findById(Integer id) { return lineaFacturaRepository.findById(id); }

    @Override
    public LineaFactura save(LineaFactura lineaFactura) { return lineaFacturaRepository.save(lineaFactura); }

    @Override
    public Boolean deleteById(Integer id) { 
        lineaFacturaRepository.deleteById(id); 
        return true;
    }

    @Override
    public List<LineaFacturaR> findLineasByFacturaId(Integer idFactura) {
        return lineaFacturaRepository.findEntitiesByFacturaId(idFactura)
                .stream()
                .map(LineaFacturaR::new)
                .toList();
    }

    /**
     * Sincroniza las líneas de una factura con el conjunto completo recibido (T-024, ADR-028 etapa 1).
     *
     * En una transacción y vía JPA: inserta las líneas nuevas (id nulo o no positivo), actualiza las existentes
     * (por id, solo si pertenecen a la factura) y elimina las que están en BD y no vienen en el cuerpo. Los montos
     * se calculan en el servidor con {@link TotalesFacturaService} sobre todas las líneas resultantes; el subtotal
     * recibido se ignora. Luego se contabiliza la venta (ADR-008).
     *
     * @throws IllegalArgumentException si el cuerpo está vacío, mezcla facturas, la factura no existe, una línea
     *                                  pertenece a otra factura, se repite, su producto no existe o sus montos son
     *                                  inválidos; en ese caso no se persiste nada
     * @throws IllegalStateException    si la factura está anulada
     */
    @Override
    @Transactional
    public TotalesFacturaDTO updateLineas(List<LineaFacturaR> lineas) {
        if (lineas == null || lineas.isEmpty()) {
            throw new IllegalArgumentException("Debe enviar al menos una línea de la factura");
        }
        Set<Integer> idsFactura = lineas.stream().map(LineaFacturaR::id_factura).collect(Collectors.toSet());
        if (idsFactura.size() != 1 || idsFactura.contains(null)) {
            throw new IllegalArgumentException("Todas las líneas deben pertenecer a una misma factura");
        }
        Integer idFactura = idsFactura.iterator().next();

        var userId = userDetailsService.getCurrentUserID()
                .orElseThrow(() -> new RuntimeException("No se pudo obtener el ID del usuario actual"));

        Factura factura = facturaRepository.findByIdFactura(idFactura)
                .orElseThrow(() -> new IllegalArgumentException("No existe la factura " + idFactura));
        if (factura.estaAnulada()) {
            throw new IllegalStateException("No se pueden modificar las líneas de una factura anulada");
        }

        log.info("Sincronizando {} líneas de la factura {}", lineas.size(), idFactura);

        // Factura sin líneas (flujo de creación): su asiento de venta se genera al guardar las líneas
        boolean primeraCargaDeLineas = factura.getLineas().isEmpty();

        Map<Integer, LineaFactura> existentes = new HashMap<>();
        factura.getLineas().forEach(linea -> existentes.put(linea.getIdLineaFactura(), linea));

        // Validar todo antes de modificar nada
        Set<Integer> idsRecibidos = new HashSet<>();
        Map<LineaFacturaR, Producto> productos = new HashMap<>();
        for (LineaFacturaR linea : lineas) {
            if (esExistente(linea)) {
                if (!existentes.containsKey(linea.id_linea_factura())) {
                    throw new IllegalArgumentException("La línea " + linea.id_linea_factura()
                            + " no pertenece a la factura " + idFactura);
                }
                if (!idsRecibidos.add(linea.id_linea_factura())) {
                    throw new IllegalArgumentException("La línea " + linea.id_linea_factura() + " está repetida");
                }
            }
            TotalesFacturaService.importeLinea(linea.cantidad(), linea.precioUnitario());
            if (linea.id_producto() == null) {
                throw new IllegalArgumentException("Cada línea debe indicar un producto");
            }
            productos.put(linea, productoRepository.findById(linea.id_producto())
                    .orElseThrow(() -> new IllegalArgumentException("No existe el producto " + linea.id_producto())));
        }

        // Eliminar las líneas que ya no vienen (orphanRemoval) y aplicar altas y cambios
        factura.getLineas().removeIf(linea -> !idsRecibidos.contains(linea.getIdLineaFactura()));
        LocalDateTime ahora = LocalDateTime.now();
        for (LineaFacturaR recibida : lineas) {
            LineaFactura linea;
            if (esExistente(recibida)) {
                linea = existentes.get(recibida.id_linea_factura());
            } else {
                linea = new LineaFactura();
                linea.setFactura(factura);
                linea.setCreateBy(userId);
                factura.getLineas().add(linea);
            }
            linea.setNumeroLinea(recibida.numero_linea());
            linea.setProducto(productos.get(recibida));
            linea.setCantidad(recibida.cantidad());
            linea.setPrecioUnitario(recibida.precioUnitario());
            linea.setUpdateBy(userId);
            linea.setUpdateDate(ahora);
        }

        // ADR-009: subtotal de línea y totales de la factura desde TODAS sus líneas, con el único cálculo
        DesgloseImpuesto desglose = totalesFacturaService.aplicarTotales(factura, factura.getLineas());
        facturaRepository.save(factura);

        // ADR-008: primera carga de líneas → generar el asiento de venta;
        // si ya estaba contabilizada → revertir y regenerar su asiento de venta
        asientoContableService.contabilizarVentaTrasGuardarLineas(factura, primeraCargaDeLineas);
        log.info("Totales actualizados para factura {}: subtotal={}, igv={}, total={}",
                idFactura, desglose.base(), desglose.impuesto(), desglose.total());

        return TotalesFacturaDTO.of(factura);
    }

    /** Línea ya persistida: id positivo. La UI numera las filas nuevas con ids negativos. */
    private static boolean esExistente(LineaFacturaR linea) {
        return linea.id_linea_factura() != null && linea.id_linea_factura() > 0;
    }
}
