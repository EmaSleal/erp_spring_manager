package api.astro.whats_orders_manager.modules.facturacion.electronica.repository;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;

import java.time.LocalDateTime;

/**
 * T-041: lo que se puede leer de una configuración de Hacienda sin tocar las columnas cifradas (contraseña, PIN y
 * tokens). Se usa cuando la fila completa no se puede cargar porque sus credenciales no se descifran con la clave
 * actual (CredencialCifradaConverter).
 */
public interface ConfiguracionHaciendaSinCredenciales {

    Long getId();

    ConfiguracionEmpresa getEmpresa();

    AmbienteHacienda getAmbiente();

    String getUsuarioHacienda();

    String getRutaCertificado();

    Boolean getActiva();

    Long getConsecutivoFactura();

    Long getConsecutivoTiquete();

    Long getConsecutivoNotaCredito();

    Long getConsecutivoNotaDebito();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();
}
