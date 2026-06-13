package api.astro.whats_orders_manager.modules.facturacion.dto.mapper;

import api.astro.whats_orders_manager.modules.facturacion.dto.ClienteSimpleDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.FacturaDetalleDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.FacturaPendienteDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.LineaFacturaDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.PagoDTO;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entidades Factura a DTOs sin referencias circulares.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since 2026-01-20
 */
@Component
public class FacturaMapper {

    private final PagoMapper pagoMapper;

    public FacturaMapper(PagoMapper pagoMapper) {
        this.pagoMapper = pagoMapper;
    }
    
    /**
     * Convierte una entidad Factura a FacturaDetalleDTO.
     * 
     * @param factura Entidad factura
     * @return DTO con toda la información de la factura
     */
    public FacturaDetalleDTO toDetalleDTO(Factura factura) {
        if (factura == null) {
            return null;
        }
        
        return FacturaDetalleDTO.builder()
                .idFactura(factura.getIdFactura())
                .numeroFactura(factura.getNumeroFactura())
                .serie(factura.getSerie())
                .cliente(toClienteSimpleDTO(factura.getCliente()))
                .subtotal(factura.getSubtotal())
                .igv(factura.getIgv())
                .total(factura.getTotal())
                .tipoFactura(factura.getTipoFactura())
                .fechaPago(factura.getFechaPago())
                .fechaEntrega(factura.getFechaEntrega())
                .fechaVencimiento(factura.getFechaVencimiento())
                .fechaEmision(factura.getFechaEmision())
                .entregado(factura.getEntregado())
                .estadoPago(factura.getEstadoPago())
                .descripcion(factura.getDescripcion())
                .lineas(toLineaFacturaDTOList(factura.getLineas()))
                .pagos(toPagoDTOList(factura.getPagos()))
                .totalPagado(factura.calcularTotalPagado())
                .saldoPendiente(factura.calcularSaldoPendiente())
                .createBy(factura.getCreateBy())
                .updateBy(factura.getUpdateBy())
                //cast factura.getUpdateDate() from Timestamp to Date
                .updateDate(factura.getUpdateDate() != null ? new java.sql.Date(factura.getUpdateDate().getTime()) : null)
                .build();
    }
    
    /**
     * Convierte una entidad Cliente a ClienteSimpleDTO.
     * 
     * @param cliente Entidad cliente
     * @return DTO simplificado del cliente
     */
    public ClienteSimpleDTO toClienteSimpleDTO(Cliente cliente) {
        if (cliente == null) {
            return null;
        }
        
        return ClienteSimpleDTO.builder()
                .idCliente(cliente.getIdCliente())
                .nombre(cliente.getNombre())
                .email(cliente.getEmail())
                .identificacion(cliente.getIdentificacion())
                .tipoCliente(cliente.getTipoCliente())
                .fechaRegistro(cliente.getFechaRegistro() != null ? new java.sql.Date(cliente.getFechaRegistro().getTime()) : null)
                .build();
    }
    
    /**
     * Convierte una lista de LineaFactura a DTOs.
     * 
     * @param lineas Lista de líneas de factura
     * @return Lista de DTOs
     */
    public List<LineaFacturaDTO> toLineaFacturaDTOList(List<LineaFactura> lineas) {
        if (lineas == null || lineas.isEmpty()) {
            return Collections.emptyList();
        }
        
        return lineas.stream()
                .map(this::toLineaFacturaDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Convierte una entidad LineaFactura a DTO.
     * 
     * @param linea Línea de factura
     * @return DTO de la línea
     */
    public LineaFacturaDTO toLineaFacturaDTO(LineaFactura linea) {
        if (linea == null) {
            return null;
        }
        
        return LineaFacturaDTO.builder()
                .idLineaFactura(linea.getIdLineaFactura())
                .numeroLinea(linea.getNumeroLinea())
                .idProducto(linea.getProducto().getIdProducto())
                .codigoProducto(linea.getProducto().getCodigo())
                .descripcionProducto(linea.getProducto().getDescripcion())
                .cantidad(linea.getCantidad())
                .precioUnitario(linea.getPrecioUnitario())
                .subtotal(linea.getSubtotal())
                .build();
    }
    
    /**
     * Converts a Factura entity to a FacturaPendienteDTO projection.
     *
     * @param f Factura entity
     * @return DTO with the four pending-balance fields, or null if the entity is null
     */
    public FacturaPendienteDTO toPendingDTO(Factura f) {
        if (f == null) return null;
        return new FacturaPendienteDTO(
            f.getIdFactura(), f.getNumeroFactura(),
            f.getTotal(), f.calcularSaldoPendiente());
    }

    /**
     * Convierte una lista de Pagos a DTOs usando PagoMapper.
     *
     * @param pagos Lista de pagos
     * @return Lista de DTOs
     */
    public List<PagoDTO> toPagoDTOList(List<Pago> pagos) {
        if (pagos == null || pagos.isEmpty()) {
            return Collections.emptyList();
        }
        
        return pagos.stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }
}
