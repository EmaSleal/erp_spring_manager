package api.astro.whats_orders_manager.modules.facturacion.electronica.model;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Configuración para integración con Ministerio de Hacienda de Costa Rica.
 * Almacena credenciales OAuth2, certificados digitales y URLs de API.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Entity
@Table(name = "configuracion_hacienda")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionHacienda {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Empresa a la que pertenece esta configuración.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    @NotNull(message = "La empresa es obligatoria")
    private ConfiguracionEmpresa empresa;
    
    /**
     * Ambiente de operación (SANDBOX o PRODUCCION).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "El ambiente es obligatorio")
    @Builder.Default
    private AmbienteHacienda ambiente = AmbienteHacienda.SANDBOX;
    
    /**
     * Usuario OAuth2 para autenticación con Hacienda.
     */
    @Column(name = "usuario_hacienda", nullable = false, length = 100)
    @NotBlank(message = "El usuario de Hacienda es obligatorio")
    private String usuarioHacienda;
    
    /**
     * Contraseña OAuth2 (encriptada).
     */
    @Column(name = "password_hacienda", nullable = false, length = 255)
    @NotBlank(message = "La contraseña es obligatoria")
    private String passwordHacienda;
    
    /**
     * Ruta al archivo .p12 del certificado digital.
     */
    @Column(name = "ruta_certificado", length = 500)
    private String rutaCertificado;
    
    /**
     * PIN del certificado digital (encriptado).
     */
    @Column(name = "pin_certificado", length = 255)
    private String pinCertificado;
    
    /**
     * Token OAuth2 actual (se renueva automáticamente).
     */
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;
    
    /**
     * Fecha de expiración del token actual.
     */
    @Column(name = "token_expira")
    private LocalDateTime tokenExpira;
    
    /**
     * Refresh token para renovar el access token.
     */
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;
    
    /**
     * Indica si la configuración está activa y lista para usar.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = false;
    
    /**
     * Consecutivo actual para facturas electrónicas.
     */
    @Column(name = "consecutivo_factura")
    @Builder.Default
    private Long consecutivoFactura = 1L;
    
    /**
     * Consecutivo actual para tiquetes electrónicos.
     */
    @Column(name = "consecutivo_tiquete")
    @Builder.Default
    private Long consecutivoTiquete = 1L;
    
    /**
     * Consecutivo actual para notas de crédito.
     */
    @Column(name = "consecutivo_nota_credito")
    @Builder.Default
    private Long consecutivoNotaCredito = 1L;
    
    /**
     * Consecutivo actual para notas de débito.
     */
    @Column(name = "consecutivo_nota_debito")
    @Builder.Default
    private Long consecutivoNotaDebito = 1L;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Verifica si el token está vigente.
     */
    public boolean isTokenValido() {
        return accessToken != null && 
               tokenExpira != null && 
               tokenExpira.isAfter(LocalDateTime.now());
    }
    
    /**
     * Incrementa y obtiene el siguiente consecutivo según el tipo.
     */
    public synchronized Long siguienteConsecutivo(String tipo) {
        switch (tipo.toUpperCase()) {
            case "01": // Factura
            case "08": // Factura de compra
            case "09": // Factura de exportación
                return consecutivoFactura++;
            case "04": // Tiquete
                return consecutivoTiquete++;
            case "03": // Nota de crédito
                return consecutivoNotaCredito++;
            case "02": // Nota de débito
                return consecutivoNotaDebito++;
            default:
                throw new IllegalArgumentException("Tipo de comprobante no soportado: " + tipo);
        }
    }
}
