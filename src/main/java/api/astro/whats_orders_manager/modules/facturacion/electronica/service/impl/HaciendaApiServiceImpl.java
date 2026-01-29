package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.RespuestaHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MensajeHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Implementación del servicio de integración con API de Hacienda Costa Rica.
 * Versión: 4.4
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HaciendaApiServiceImpl implements HaciendaApiService {
    
    private final ConfiguracionHaciendaRepository configuracionRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String GRANT_TYPE_PASSWORD = "password";
    private static final String GRANT_TYPE_REFRESH = "refresh_token";
    
    @Override
    @Cacheable(value = "haciendaToken", key = "#configuracionId", unless = "#result == null")
    public TokenResponse autenticar(Long configuracionId) {
        log.info("Autenticando con Hacienda OAuth2 - Config ID: {}", configuracionId);
        
        ConfiguracionHacienda config = configuracionRepository.findById(configuracionId)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        
        try {
            String urlToken = config.getAmbiente().getUrlToken();
            
            // Preparar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            // Preparar body con credenciales
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", GRANT_TYPE_PASSWORD);
            body.add("client_id", "api-stag"); // Cliente por defecto para sandbox
            body.add("client_secret", ""); // Sin secret para sandbox
            body.add("username", config.getUsuarioHacienda());
            body.add("password", config.getPasswordHacienda());
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<Map> response = restTemplate.postForEntity(urlToken, request, Map.class);
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                log.info("Autenticación exitosa - Tiempo: {}ms", responseTime);
                
                return new TokenResponse(
                    (String) responseBody.get("access_token"),
                    (String) responseBody.get("refresh_token"),
                    (Integer) responseBody.get("expires_in"),
                    (String) responseBody.get("token_type")
                );
            } else {
                throw new RuntimeException("Respuesta inesperada de Hacienda: " + response.getStatusCode());
            }
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Error HTTP al autenticar: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Error de autenticación con Hacienda: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error al autenticar con Hacienda: {}", e.getMessage(), e);
            throw new RuntimeException("Error al autenticar: " + e.getMessage(), e);
        }
    }
    
    @Override
    public TokenResponse renovarToken(String refreshToken, Long configuracionId) {
        log.info("Renovando token OAuth2 - Config ID: {}", configuracionId);
        
        ConfiguracionHacienda config = configuracionRepository.findById(configuracionId)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        
        try {
            String urlToken = config.getAmbiente().getUrlToken();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", GRANT_TYPE_REFRESH);
            body.add("client_id", "api-stag");
            body.add("client_secret", "");
            body.add("refresh_token", refreshToken);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(urlToken, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                log.info("Token renovado exitosamente");
                
                return new TokenResponse(
                    (String) responseBody.get("access_token"),
                    (String) responseBody.get("refresh_token"),
                    (Integer) responseBody.get("expires_in"),
                    (String) responseBody.get("token_type")
                );
            } else {
                // Si falla, intentar autenticación completa
                log.warn("Renovación falló, intentando autenticación completa");
                return autenticar(configuracionId);
            }
            
        } catch (Exception e) {
            log.warn("Error renovando token, intentando autenticación: {}", e.getMessage());
            return autenticar(configuracionId);
        }
    }
    
    @Override
    @CircuitBreaker(name = "haciendaAPI", fallbackMethod = "enviarComprobanteFallback")
    @Retry(name = "haciendaAPI")
    public RespuestaHaciendaDTO enviarComprobante(String xml, String claveNumerica, Long configuracionId) {
        log.info("Enviando comprobante a Hacienda - Clave: {}", claveNumerica);
        
        ConfiguracionHacienda config = configuracionRepository.findById(configuracionId)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        
        try {
            // Obtener token
            TokenResponse token = autenticar(configuracionId);
            
            // URL de recepción
            String urlRecepcion = config.getAmbiente().getUrlBase() + "/recepcion/v4.4/recepcion";
            
            // Codificar XML en Base64
            String xmlBase64 = Base64.getEncoder().encodeToString(xml.getBytes("UTF-8"));
            
            // Preparar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token.accessToken());
            
            // Preparar body JSON
            Map<String, Object> body = Map.of(
                "clave", claveNumerica,
                "fecha", LocalDateTime.now().toString(),
                "emisor", Map.of(
                    "tipoIdentificacion", "02", // Persona Jurídica por defecto
                    "numeroIdentificacion", config.getEmpresa().getRuc()
                ),
                "comprobanteXml", xmlBase64
            );
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.postForEntity(urlRecepcion, request, String.class);
            long responseTime = System.currentTimeMillis() - startTime;
            
            log.info("Respuesta de Hacienda - Status: {} - Tiempo: {}ms", 
                response.getStatusCode(), responseTime);
            
            // Parsear respuesta
            return parsearRespuesta(response, claveNumerica, responseTime);
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Error HTTP al enviar comprobante: {} - {}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            
            return RespuestaHaciendaDTO.builder()
                .claveNumerica(claveNumerica)
                .fechaRespuesta(LocalDateTime.now())
                .codigoMensaje(MensajeHacienda.ERROR)
                .mensaje("Error HTTP: " + e.getStatusCode())
                .exitoso(false)
                .detalles(e.getResponseBodyAsString())
                .codigoHttp(e.getStatusCode().value())
                .debeReintentar(e.getStatusCode().is5xxServerError())
                .build();
                
        } catch (Exception e) {
            log.error("Error al enviar comprobante: {}", e.getMessage(), e);
            
            return RespuestaHaciendaDTO.builder()
                .claveNumerica(claveNumerica)
                .fechaRespuesta(LocalDateTime.now())
                .codigoMensaje(MensajeHacienda.ERROR)
                .mensaje("Error de comunicación: " + e.getMessage())
                .exitoso(false)
                .debeReintentar(true)
                .build();
        }
    }
    
    @Override
    @CircuitBreaker(name = "haciendaAPI", fallbackMethod = "consultarComprobanteFallback")
    @Retry(name = "haciendaAPI")
    public RespuestaHaciendaDTO consultarComprobante(String claveNumerica, Long configuracionId) {
        log.info("Consultando comprobante en Hacienda - Clave: {}", claveNumerica);
        
        ConfiguracionHacienda config = configuracionRepository.findById(configuracionId)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        
        try {
            // Obtener token
            TokenResponse token = autenticar(configuracionId);
            
            // URL de consulta
            String urlConsulta = config.getAmbiente().getUrlBase() + 
                "/consulta-documento/v4.4/documentos/" + claveNumerica;
            
            // Preparar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token.accessToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<?> request = new HttpEntity<>(headers);
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.exchange(
                urlConsulta, 
                HttpMethod.GET, 
                request, 
                String.class
            );
            long responseTime = System.currentTimeMillis() - startTime;
            
            log.info("Consulta exitosa - Status: {} - Tiempo: {}ms", 
                response.getStatusCode(), responseTime);
            
            return parsearRespuesta(response, claveNumerica, responseTime);
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.info("Comprobante no encontrado en Hacienda: {}", claveNumerica);
                
                return RespuestaHaciendaDTO.builder()
                    .claveNumerica(claveNumerica)
                    .fechaRespuesta(LocalDateTime.now())
                    .codigoMensaje(MensajeHacienda.PROCESANDO)
                    .mensaje("Comprobante no encontrado - aún en procesamiento")
                    .exitoso(false)
                    .codigoHttp(404)
                    .debeReintentar(true)
                    .build();
            }
            
            log.error("Error HTTP al consultar: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Error consultando comprobante", e);
            
        } catch (Exception e) {
            log.error("Error al consultar comprobante: {}", e.getMessage(), e);
            throw new RuntimeException("Error en consulta: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<MensajeHaciendaResponse> consultarMensajes(Long configuracionId) {
        log.info("Consultando mensajes de Hacienda - Config ID: {}", configuracionId);
        
        ConfiguracionHacienda config = configuracionRepository.findById(configuracionId)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        
        try {
            // Obtener token
            TokenResponse token = autenticar(configuracionId);
            
            // URL de mensajes
            String urlMensajes = config.getAmbiente().getUrlBase() + 
                "/mensajes-receptor/v4.4/mensajes/" + config.getEmpresa().getRuc();
            
            // Preparar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token.accessToken());
            
            HttpEntity<?> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                urlMensajes,
                HttpMethod.GET,
                request,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Parsear respuesta JSON
                JsonNode root = objectMapper.readTree(response.getBody());
                List<MensajeHaciendaResponse> mensajes = new ArrayList<>();
                
                if (root.has("mensajes") && root.get("mensajes").isArray()) {
                    for (JsonNode mensaje : root.get("mensajes")) {
                        mensajes.add(new MensajeHaciendaResponse(
                            mensaje.get("clave").asText(),
                            mensaje.get("mensaje").asText(),
                            mensaje.has("detalle") ? mensaje.get("detalle").asText() : "",
                            mensaje.get("estado").asText()
                        ));
                    }
                }
                
                log.info("Encontrados {} mensajes de Hacienda", mensajes.size());
                return mensajes;
            }
            
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("Error consultando mensajes: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    // ========== MÉTODOS AUXILIARES ==========
    
    /**
     * Parsea la respuesta de Hacienda y crea DTO.
     */
    private RespuestaHaciendaDTO parsearRespuesta(ResponseEntity<String> response, 
                                                   String claveNumerica, 
                                                   long responseTime) {
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            
            // Determinar estado según código de Hacienda
            String codigoMensaje = root.has("indEstado") ? root.get("indEstado").asText() : "";
            MensajeHacienda mensaje = determinarMensaje(codigoMensaje);
            
            String mensajeTexto = root.has("respuesta-xml") 
                ? root.get("respuesta-xml").asText() 
                : (root.has("mensaje") ? root.get("mensaje").asText() : "");
            
            return RespuestaHaciendaDTO.builder()
                .claveNumerica(claveNumerica)
                .fechaRespuesta(LocalDateTime.now())
                .codigoMensaje(mensaje)
                .mensaje(mensajeTexto)
                .exitoso(mensaje == MensajeHacienda.ACEPTADO)
                .detalles(response.getBody())
                .codigoHttp(response.getStatusCode().value())
                .tiempoRespuestaMs(responseTime)
                .debeReintentar(mensaje == MensajeHacienda.PROCESANDO)
                .build();
                
        } catch (Exception e) {
            log.error("Error parseando respuesta de Hacienda: {}", e.getMessage());
            
            return RespuestaHaciendaDTO.builder()
                .claveNumerica(claveNumerica)
                .fechaRespuesta(LocalDateTime.now())
                .codigoMensaje(MensajeHacienda.ERROR)
                .mensaje("Error parseando respuesta")
                .exitoso(false)
                .detalles(response.getBody())
                .codigoHttp(response.getStatusCode().value())
                .tiempoRespuestaMs(responseTime)
                .build();
        }
    }
    
    /**
     * Determina el MensajeHacienda según código de respuesta.
     */
    private MensajeHacienda determinarMensaje(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            return MensajeHacienda.PROCESANDO;
        }
        
        return switch (codigo.toLowerCase()) {
            case "aceptado", "1" -> MensajeHacienda.ACEPTADO;
            case "rechazado", "2" -> MensajeHacienda.RECHAZADO;
            case "procesando", "0" -> MensajeHacienda.PROCESANDO;
            default -> MensajeHacienda.ERROR;
        };
    }
    
    // ========================================
    // MÉTODOS FALLBACK PARA CIRCUIT BREAKER
    // ========================================
    
    /**
     * Método fallback para enviarComprobante cuando el Circuit Breaker se abre.
     * Retorna una respuesta indicando que el servicio no está disponible.
     */
    private RespuestaHaciendaDTO enviarComprobanteFallback(
            String xml, 
            String claveNumerica, 
            Long configuracionId, 
            Exception e) {
        
        log.error("⚠️ Circuit Breaker ABIERTO - Fallback activado para enviarComprobante: {}", 
            e.getMessage());
        
        return RespuestaHaciendaDTO.builder()
            .claveNumerica(claveNumerica)
            .fechaRespuesta(LocalDateTime.now())
            .codigoMensaje(MensajeHacienda.ERROR)
            .mensaje("Servicio de Hacienda temporalmente no disponible. Se reintentará automáticamente.")
            .indicadorEstado("circuit-breaker-open")
            .exitoso(false)
            .detalles("Circuit Breaker activado debido a múltiples fallos. " +
                     "Causa: " + e.getClass().getSimpleName() + " - " + e.getMessage())
            .codigoHttp(503) // Service Unavailable
            .debeReintentar(true)
            .build();
    }
    
    /**
     * Método fallback para consultarComprobante cuando el Circuit Breaker se abre.
     */
    private RespuestaHaciendaDTO consultarComprobanteFallback(
            String claveNumerica, 
            Long configuracionId, 
            Exception e) {
        
        log.error("⚠️ Circuit Breaker ABIERTO - Fallback activado para consultarComprobante: {}", 
            e.getMessage());
        
        return RespuestaHaciendaDTO.builder()
            .claveNumerica(claveNumerica)
            .fechaRespuesta(LocalDateTime.now())
            .codigoMensaje(MensajeHacienda.PROCESANDO)
            .mensaje("No se pudo consultar estado. El comprobante se encuentra en procesamiento.")
            .indicadorEstado("circuit-breaker-open")
            .exitoso(false)
            .detalles("Circuit Breaker activado. Consulta automática se realizará posteriormente.")
            .codigoHttp(503)
            .debeReintentar(true)
            .build();
    }
}
