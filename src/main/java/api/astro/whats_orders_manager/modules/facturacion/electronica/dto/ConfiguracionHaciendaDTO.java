package api.astro.whats_orders_manager.modules.facturacion.electronica.dto;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO para configuración de Hacienda.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionHaciendaDTO {
    
    private Long id;
    
    @NotNull(message = "El ID de empresa es obligatorio")
    private Long empresaId;
    
    private String empresaNombre;
    
    @NotNull(message = "El ambiente es obligatorio")
    private AmbienteHacienda ambiente;
    
    @NotBlank(message = "El usuario de Hacienda es obligatorio")
    private String usuarioHacienda;
    
    /** En respuestas siempre "********"; en un PUT, "********" conserva el valor guardado. */
    @NotBlank(message = "La contraseña es obligatoria")
    @ToString.Exclude
    private String passwordHacienda;
    
    private String rutaCertificado;
    
    /** En respuestas "********" (o null si no hay PIN); en un PUT, "********" o null conserva el valor guardado. */
    @ToString.Exclude
    private String pinCertificado;
    
    private Boolean activa;
    
    private Boolean tokenValido;
    
    private LocalDateTime tokenExpira;
    
    /** Hay un access token guardado (el token nunca sale en el DTO; T-021). */
    private Boolean tieneToken;
    
    private Long consecutivoFactura;
    
    private Long consecutivoTiquete;
    
    private Long consecutivoNotaCredito;
    
    private Long consecutivoNotaDebito;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
