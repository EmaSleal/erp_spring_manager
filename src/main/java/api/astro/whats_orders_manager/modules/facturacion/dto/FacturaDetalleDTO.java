package api.astro.whats_orders_manager.modules.facturacion.dto;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPagoFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.InvoiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para el detalle completo de una factura.
 * Incluye cliente, líneas de factura y pagos sin referencias circulares.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since 2026-01-20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaDetalleDTO {
    
    private Integer idFactura;
    private String numeroFactura;
    private String serie;
    
    // Información del cliente
    private ClienteSimpleDTO cliente;
    
    // Moneda
    private MonedaFE monedaFE;
    private BigDecimal tipoCambio;

    // Montos
    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal total;
    
    // Tipo y fechas
    private InvoiceType tipoFactura;
    private Date fechaPago;
    private Date fechaEntrega;
    private Date fechaVencimiento;
    private LocalDate fechaEmision;
    
    // Estado y entregas
    private Boolean entregado;
    private EstadoPagoFactura estadoPago;
    
    // Descripción
    private String descripcion;
    
    // Líneas de la factura
    @Builder.Default
    private List<LineaFacturaDTO> lineas = new ArrayList<>();
    
    // Pagos aplicados
    @Builder.Default
    private List<PagoDTO> pagos = new ArrayList<>();
    
    // Cálculos
    private BigDecimal totalPagado;
    private BigDecimal saldoPendiente;
    
    // Auditoría
    private Integer createBy;
    private Integer updateBy;
    private Date updateDate;
}
