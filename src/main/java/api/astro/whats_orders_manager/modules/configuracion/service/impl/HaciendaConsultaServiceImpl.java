package api.astro.whats_orders_manager.modules.configuracion.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.dto.CabysBusquedaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO;
import api.astro.whats_orders_manager.modules.configuracion.service.HaciendaApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Implementación del servicio para consultar API pública de Hacienda Costa Rica.
 * Utiliza RestClient de Spring Framework 6.x para llamadas HTTP.
 * Consulta datos de contribuyentes para autocompletar formularios.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 4
 */
@Service
@Slf4j
public class HaciendaConsultaServiceImpl implements HaciendaApiService {
    
    private final RestClient restClient;
    
    /**
     * URL base de la API de Hacienda
     * Configurable desde application.yml
     */
    @Value("${hacienda.api.url:https://api.hacienda.go.cr/fe/ae}")
    private String haciendaApiUrl;
    
    /**
     * URL base para búsqueda de CABYS
     */
    @Value("${hacienda.api.cabys.url:https://api.hacienda.go.cr/fe/cabys}")
    private String haciendaCabysUrl;
    
    /**
     * Timeout para llamadas a la API (en segundos)
     */
    @Value("${hacienda.api.timeout:10}")
    private Integer timeout;
    
    public HaciendaConsultaServiceImpl() {
        // Crear RestClient con configuración básica
        this.restClient = RestClient.builder()
            .build();
    }
    
    @Override
    public HaciendaConsultaDTO consultarContribuyente(String numeroIdentificacion) {
        // Validar entrada
        if (numeroIdentificacion == null || numeroIdentificacion.trim().isEmpty()) {
            log.warn("Número de identificación vacío");
            return HaciendaConsultaDTO.builder()
                .exitosa(false)
                .mensajeError("Número de identificación requerido")
                .build();
        }
        
        // Limpiar número (remover guiones, espacios)
        String numeroLimpio = numeroIdentificacion.replaceAll("[\\s-]", "");
        
        log.info("Consultando API Hacienda para identificación: {}", numeroLimpio);
        
        try {
            // Construir URL con query parameter
            String url = haciendaApiUrl + "?identificacion=" + numeroLimpio;
            
            .log.debug("URL construida para consulta: {}", url);

            // Realizar petición GET
            ResponseEntity<HaciendaConsultaDTO> response = restClient.get()
                .uri(url)
                .retrieve()
                .toEntity(HaciendaConsultaDTO.class);
            
            // Verificar respuesta exitosa
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                HaciendaConsultaDTO resultado = response.getBody();
                resultado.setExitosa(true);
                
                log.info("Consulta exitosa para {}: {} (Tipo: {})", 
                    numeroLimpio, 
                    resultado.getNombre(), 
                    resultado.getTipoIdentificacion());
                
                return resultado;
            } else {
                log.warn("Respuesta no exitosa de Hacienda: {}", response.getStatusCode());
                return HaciendaConsultaDTO.builder()
                    .exitosa(false)
                    .mensajeError("Identificación no encontrada en Hacienda")
                    .build();
            }
            
        } catch (RestClientException e) {
            log.error("Error al consultar API de Hacienda para {}: {}", numeroLimpio, e.getMessage());
            
            // Verificar si es error 404 (no encontrado)
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                return HaciendaConsultaDTO.builder()
                    .exitosa(false)
                    .mensajeError("Identificación no encontrada en el registro de Hacienda")
                    .build();
            }
            
            return HaciendaConsultaDTO.builder()
                .exitosa(false)
                .mensajeError("Error al conectar con Hacienda: " + e.getMessage())
                .build();
        } catch (Exception e) {
            log.error("Error inesperado al consultar Hacienda: {}", e.getMessage(), e);
            return HaciendaConsultaDTO.builder()
                .exitosa(false)
                .mensajeError("Error inesperado: " + e.getMessage())
                .build();
        }
    }
    
    @Override
    public boolean validarIdentificacion(String numeroIdentificacion) {
        HaciendaConsultaDTO resultado = consultarContribuyente(numeroIdentificacion);
                // Validar que existe, está inscrito y al día
        return resultado != null && 
               resultado.getExitosa() != null && 
               resultado.getExitosa() && 
               resultado.estaInscrito();
    }
        
    
    @Override
    public CabysBusquedaDTO buscarCabys(String busqueda, Integer top) {
        // Validar entrada
        if (busqueda == null || busqueda.trim().isEmpty()) {
            log.warn("Búsqueda CABYS vacía");
            return CabysBusquedaDTO.builder()
                .exitosa(false)
                .mensajeError("Término de búsqueda requerido")
                .build();
        }
        
        // Establecer top por defecto
        if (top == null || top <= 0) {
            top = 10;
        }
        
        log.info("Buscando códigos CABYS para: '{}' (top: {})", busqueda, top);
        
        try {
            // Construir URL con query parameters
            String url = UriComponentsBuilder.fromHttpUrl(haciendaCabysUrl)
                .queryParam("q", busqueda)
                .queryParam("top", top)
                .encode()
                .toUriString();
            
            // Realizar petición GET
            ResponseEntity<CabysBusquedaDTO> response = restClient.get()
                .uri(url)
                .retrieve()
                .toEntity(CabysBusquedaDTO.class);
            
            // Verificar respuesta exitosa
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                CabysBusquedaDTO resultado = response.getBody();
                resultado.setExitosa(true);
                
                log.info("Búsqueda CABYS exitosa: {} resultados (total: {})", 
                    resultado.getCantidad(), 
                    resultado.getTotal());
                
                return resultado;
            } else {
                log.warn("Respuesta no exitosa de API CABYS: {}", response.getStatusCode());
                return CabysBusquedaDTO.builder()
                    .exitosa(false)
                    .mensajeError("No se encontraron códigos CABYS")
                    .build();
            }
            
        } catch (RestClientException e) {
            log.error("Error al buscar CABYS para '{}': {}", busqueda, e.getMessage());
            
            return CabysBusquedaDTO.builder()
                .exitosa(false)
                .mensajeError("Error al conectar con API CABYS: " + e.getMessage())
                .build();
        } catch (Exception e) {
            log.error("Error inesperado al buscar CABYS: {}", e.getMessage(), e);
            return CabysBusquedaDTO.builder()
                .exitosa(false)
                .mensajeError("Error inesperado: " + e.getMessage())
                .build();
        }
    }

}
