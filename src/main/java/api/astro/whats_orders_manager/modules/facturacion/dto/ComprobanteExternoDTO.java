package api.astro.whats_orders_manager.modules.facturacion.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/** Cuerpo de PUT /api/facturas/{id}/comprobante-externo (T-057): clave de 50 dígitos y fecha de emisión del comprobante. */
@Data
public class ComprobanteExternoDTO {
    private String clave;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fecha;
}
