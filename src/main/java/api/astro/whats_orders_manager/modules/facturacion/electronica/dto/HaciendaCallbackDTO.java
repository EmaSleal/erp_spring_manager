package api.astro.whats_orders_manager.modules.facturacion.electronica.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO para recibir callbacks de Hacienda de Costa Rica.
 * 
 * <p>Este DTO se utiliza cuando Hacienda envía una notificación asíncrona
 * sobre el cambio de estado de un comprobante electrónico enviado.</p>
 * 
 * <p><strong>Ejemplo de uso:</strong></p>
 * <pre>
 * POST /api/hacienda/callback
 * {
 *   "claveNumerica": "50612202600100111234567890123456789012345678901234",
 *   "estado": "aceptado",
 *   "codigoRespuesta": "1",
 *   "mensaje": "Comprobante aceptado",
 *   "xmlRespuesta": "PD94bWwgdmVyc2lvbj0iMS4wIi...",
 *   "fechaRespuesta": "2026-01-24T15:30:00"
 * }
 * </pre>
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HaciendaCallbackDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Clave numérica única del comprobante (50 dígitos).
     * Formato: [País][Día][Mes][Año][Cédula][Consecutivo][Situación][CódigoSeguridad]
     */
    @NotBlank(message = "La clave numérica es obligatoria")
    @Size(min = 50, max = 50, message = "La clave numérica debe tener exactamente 50 dígitos")
    private String claveNumerica;
    
    /**
     * Estado del comprobante según Hacienda.
     * Valores posibles: "aceptado", "rechazado", "procesando", "error"
     */
    @NotBlank(message = "El estado es obligatorio")
    private String estado;
    
    /**
     * Código de respuesta de Hacienda.
     * 
     * <p>Códigos comunes:</p>
     * <ul>
     *   <li>1 = Aceptado</li>
     *   <li>2 = Aceptado parcialmente</li>
     *   <li>3 = Rechazado</li>
     * </ul>
     */
    @NotBlank(message = "El código de respuesta es obligatorio")
    private String codigoRespuesta;
    
    /**
     * Mensaje descriptivo de la respuesta de Hacienda.
     * Puede incluir detalles sobre errores o advertencias.
     */
    private String mensaje;
    
    /**
     * XML de respuesta firmado por Hacienda (codificado en Base64).
     * Contiene el mensaje de confirmación firmado digitalmente.
     */
    private String xmlRespuesta;
    
    /**
     * Fecha y hora de la respuesta en formato ISO 8601.
     * Ejemplo: "2026-01-24T15:30:00"
     */
    @NotNull(message = "La fecha de respuesta es obligatoria")
    private String fechaRespuesta;
    
    /**
     * Token de autenticación compartido entre Hacienda y el sistema.
     * Se utiliza para validar que el callback proviene de Hacienda.
     */
    private String token;
    
    /**
     * Información adicional o metadatos del callback (opcional).
     */
    private String detalles;
}
