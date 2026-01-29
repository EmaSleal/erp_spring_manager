package api.astro.whats_orders_manager.modules.facturacion.dto;

import java.sql.Date;

import api.astro.whats_orders_manager.modules.facturacion.enums.InvoiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO simplificado de Cliente para evitar referencias circulares.
 * Contiene solo los datos esenciales para mostrar en el detalle de factura.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since 2026-01-20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteSimpleDTO {
    
    private Integer idCliente;
    private String nombre;
    private String email;
    private String identificacion;
    private InvoiceType tipoCliente;
    private Date fechaRegistro;
}
