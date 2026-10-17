package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.RespuestaHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MensajeHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.ResultadoEnvioHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.ClasificadorEnvioHacienda;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
            String urlToken = urlToken(config);
            
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

            log.info("Respuesta de autenticación - Status: {} - Tiempo: {}ms", 
                response.getStatusCode(), responseTime);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                log.info("Autenticación exitosa - Tiempo: {}ms", responseTime);

                //log.info("Respuesta de autenticación: {}", responseBody);
                
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
            String urlToken = urlToken(config);
            
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
    
    /**
     * POST /recepcion. Nunca lanza: clasifica el resultado en origen (ADR-030 §3) en
     * {@link RespuestaHaciendaDTO#getResultadoEnvio()}:
     * <ul>
     *   <li>2xx → RECIBIDO (ENVIADO);</li>
     *   <li>sin token, sin conexión (DNS, rechazada, timeout de conexión), 401/403 o 4xx de validación →
     *       NO_TRANSMITIDO;</li>
     *   <li>timeout de lectura, conexión cortada, 5xx, 4xx de ya recibido o sin causa clasificable →
     *       ENVIO_INCIERTO;</li>
     *   <li>falla al preparar la petición, antes de conectar → ERROR_LOCAL.</li>
     * </ul>
     * Sin reintentos automáticos del POST: repetirlo a ciegas podría duplicar un documento ya recibido.
     */
    @Override
    @CircuitBreaker(name = "haciendaAPI", fallbackMethod = "enviarComprobanteFallback")
    public RespuestaHaciendaDTO enviarComprobante(String xml, String claveNumerica, Long configuracionId) {
        log.info("Enviando comprobante a Hacienda - Clave: {}", claveNumerica);

        ConfiguracionHacienda config;
        String urlRecepcion;
        Map<String, Object> body;
        try {
            config = configuracionRepository.findById(configuracionId)
                .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
            urlRecepcion = urlBase(config) + "/recepcion/v1/recepcion";

            String xmlBase64 = Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));
            String fechaEmision = OffsetDateTime.now(ZoneId.of("America/Costa_Rica"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx"));
            String tipoId = config.getEmpresa().getTipoIdentificacion() != null
                ? config.getEmpresa().getTipoIdentificacion().getCodigo()
                : "02";
            body = Map.of(
                "clave", claveNumerica,
                "fecha", fechaEmision,
                "emisor", Map.of(
                    "tipoIdentificacion", tipoId,
                    "numeroIdentificacion", config.getEmpresa().getRuc()
                ),
                "comprobanteXml", xmlBase64
            );
        } catch (Exception e) {
            log.error("Error local preparando el envío de {}: {}", claveNumerica, e.getMessage());
            return resultadoEnvio(claveNumerica, ResultadoEnvioHacienda.ERROR_LOCAL, null, null,
                "Error preparando el envío: " + e.getMessage(), 0);
        }

        // Token: si no se obtiene, la recepción nunca se intentó
        TokenResponse token;
        try {
            token = autenticar(configuracionId);
        } catch (Exception e) {
            log.error("No se obtuvo el token de Hacienda para {}: {}", claveNumerica, e.getMessage());
            return resultadoEnvio(claveNumerica, ResultadoEnvioHacienda.NO_TRANSMITIDO, null, null,
                "No se obtuvo el token de Hacienda: " + e.getMessage(), 0);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token.accessToken());
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        log.info("URL recepción: {}", urlRecepcion);
        // T-021: nunca registrar el token, ni siquiera parcial
        log.info("Auth header presente: {}", headers.getFirst(HttpHeaders.AUTHORIZATION) != null);

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(urlRecepcion, request, String.class);
            long responseTime = System.currentTimeMillis() - startTime;
            log.info("Respuesta de Hacienda - Status: {} - Tiempo: {}ms", response.getStatusCode(), responseTime);
            return resultadoEnvio(claveNumerica, ResultadoEnvioHacienda.RECIBIDO, response.getStatusCode().value(),
                null, "Comprobante recibido por Hacienda, en procesamiento", responseTime);

        } catch (HttpStatusCodeException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            String causa = e.getResponseHeaders() != null
                ? e.getResponseHeaders().getFirst(ClasificadorEnvioHacienda.HEADER_ERROR_CAUSE)
                : null;
            ResultadoEnvioHacienda resultado = ClasificadorEnvioHacienda.porRespuesta(e.getStatusCode().value(), causa);
            log.error("Recepción rechazada por Hacienda: {} - causa: {} → {}", e.getStatusCode(), causa, resultado);
            return resultadoEnvio(claveNumerica, resultado, e.getStatusCode().value(), causa,
                "Error HTTP " + e.getStatusCode().value() + (causa != null ? ": " + causa : ""), responseTime);

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            ResultadoEnvioHacienda resultado = ClasificadorEnvioHacienda.porFalloDeTransporte(e);
            log.error("Error de comunicación enviando {}: {} → {}", claveNumerica, e.getMessage(), resultado);
            return resultadoEnvio(claveNumerica, resultado, null, null,
                "Error de comunicación: " + e.getMessage(), responseTime);
        }
    }

    private RespuestaHaciendaDTO resultadoEnvio(String claveNumerica, ResultadoEnvioHacienda resultado,
                                                Integer codigoHttp, String causa, String mensaje,
                                                long responseTime) {
        return RespuestaHaciendaDTO.builder()
            .claveNumerica(claveNumerica)
            .fechaRespuesta(LocalDateTime.now())
            .codigoMensaje(resultado == ResultadoEnvioHacienda.RECIBIDO ? MensajeHacienda.PROCESANDO : MensajeHacienda.ERROR)
            .mensaje(mensaje)
            .exitoso(false)
            .codigoHttp(codigoHttp)
            .causaError(causa)
            .resultadoEnvio(resultado)
            .tiempoRespuestaMs(responseTime)
            .debeReintentar(false)
            .build();
    }

    /** URL base de la API de recepción del ambiente. Protegido para apuntar las pruebas a un servidor simulado. */
    protected String urlBase(ConfiguracionHacienda config) {
        return config.getAmbiente().getUrlBase();
    }

    /** URL del token OAuth2 del ambiente. Protegido para apuntar las pruebas a un servidor simulado. */
    protected String urlToken(ConfiguracionHacienda config) {
        return config.getAmbiente().getUrlToken();
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
            
            String urlConsulta = urlBase(config) +
                "/recepcion/v1/recepcion/" + claveNumerica;
            
            // Preparar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token.accessToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Log de request completo para depuración
            // T-021: sin headers en el log (llevan el Bearer token)
            log.debug("Consultando comprobante en Hacienda - URL: {}", urlConsulta);
            
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
                // ADR-030 §3: el 404 es una señal explícita de que Hacienda no tiene el documento
                log.info("Hacienda no tiene el comprobante (404): {}", claveNumerica);

                return RespuestaHaciendaDTO.builder()
                    .claveNumerica(claveNumerica)
                    .fechaRespuesta(LocalDateTime.now())
                    .codigoMensaje(MensajeHacienda.ERROR)
                    .mensaje("Hacienda no tiene el documento (404)")
                    .exitoso(false)
                    .codigoHttp(404)
                    .documentoNoExiste(true)
                    .debeReintentar(false)
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
                "/mensajes-receptor/v1/mensajes/" + config.getEmpresa().getRuc();
            
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
        // 202 = Hacienda aceptó el comprobante para procesamiento asíncrono (body vacío esperado)
        if (response.getBody() == null || response.getBody().isBlank()) {
            return RespuestaHaciendaDTO.builder()
                .claveNumerica(claveNumerica)
                .fechaRespuesta(LocalDateTime.now())
                .codigoMensaje(MensajeHacienda.PROCESANDO)
                .mensaje("Comprobante recibido por Hacienda, en procesamiento")
                .exitoso(false)
                .codigoHttp(response.getStatusCode().value())
                .tiempoRespuestaMs(responseTime)
                .debeReintentar(false)
                .build();
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            
            // Hacienda retorna "ind-estado" (con guion), no camelCase
            String codigoMensaje = root.has("ind-estado") ? root.get("ind-estado").asText()
                : (root.has("indEstado") ? root.get("indEstado").asText() : "");
            log.info("Hacienda ind-estado: '{}'", codigoMensaje);
            MensajeHacienda mensaje = determinarMensaje(codigoMensaje);

            String xmlRespuesta  = root.has("respuesta-xml") ? root.get("respuesta-xml").asText() : null;
            String mensajeTexto  = root.has("mensaje") ? root.get("mensaje").asText() : "";

            return RespuestaHaciendaDTO.builder()
                .claveNumerica(claveNumerica)
                .fechaRespuesta(LocalDateTime.now())
                .codigoMensaje(mensaje)
                .mensaje(mensajeTexto)
                .xmlRespuesta(xmlRespuesta)
                .indicadorEstado(codigoMensaje)
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
            case "procesando", "recibido", "0" -> MensajeHacienda.PROCESANDO;
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
        // Circuito abierto: la llamada no se hizo, así que Hacienda no recibió nada. Cualquier otra excepción
        // aquí proviene de la preparación local (enviarComprobante no lanza tras conectar).
        ResultadoEnvioHacienda resultado = e instanceof CallNotPermittedException
            ? ResultadoEnvioHacienda.NO_TRANSMITIDO
            : ResultadoEnvioHacienda.ERROR_LOCAL;
        log.error("⚠️ Fallback de enviarComprobante ({}): {} → {}", e.getClass().getSimpleName(),
            e.getMessage(), resultado);
        return resultadoEnvio(claveNumerica, resultado, null, null,
            "Servicio de Hacienda no disponible (circuito abierto). Se reintentará automáticamente.", 0);
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
