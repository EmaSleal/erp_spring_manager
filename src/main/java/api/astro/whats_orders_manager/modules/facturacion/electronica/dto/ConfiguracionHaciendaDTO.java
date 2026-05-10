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
    
    @NotBlank(message = "La contraseña es obligatoria")
    private String passwordHacienda;
    
    private String rutaCertificado;
    
    private String pinCertificado;
    
    private Boolean activa;
    
    private Boolean tokenValido;
    
    private LocalDateTime tokenExpira;
    
    private String accessToken;
    
    private Long consecutivoFactura;
    
    private Long consecutivoTiquete;
    
    private Long consecutivoNotaCredito;
    
    private Long consecutivoNotaDebito;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
