package api.astro.whats_orders_manager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuración de clientes HTTP para comunicación con APIs externas.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Configuration
public class HttpClientConfig {
    
    /**
     * RestTemplate configurado para comunicación con API de Hacienda.
     * 
     * Timeouts:
     * - Conexión: 10 segundos
     * - Lectura: 30 segundos
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(30))
            .requestFactory(this::clientHttpRequestFactory)
            .defaultHeader("User-Agent", "curl/7.81.0")
            .defaultHeader("Accept", "*/*")
            .build();
    }
    
    /**
     * Cliente HTTP exclusivo de la API de Hacienda: token (IdP), recepción y consulta (T-037, ADR-043 §2). Los timeouts
     * vienen de propiedades con valores explícitos y acotan el peor caso de la petición del usuario al emitir
     * (token + POST de recepción: 2 × (conexión + lectura)); el cálculo está en docs/diseno/ENVIO_HACIENDA_ETAPAS.md.
     * Bean propio para no compartir el {@code restTemplate} genérico, que otra configuración sobrescribe.
     */
    @Bean(name = "haciendaRestTemplate")
    public RestTemplate haciendaRestTemplate(
            RestTemplateBuilder builder,
            @Value("${facturacion.hacienda.http.connect-timeout:5s}") Duration connectTimeout,
            @Value("${facturacion.hacienda.http.read-timeout:15s}") Duration readTimeout) {
        return builder
            .requestFactory(() -> {
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout((int) connectTimeout.toMillis());
                factory.setReadTimeout((int) readTimeout.toMillis());
                return factory;
            })
            .defaultHeader("User-Agent", "curl/7.81.0")
            .defaultHeader("Accept", "*/*")
            .build();
    }

    /**
     * Factory para configuración adicional de requests HTTP.
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 segundos
        factory.setReadTimeout(30000);    // 30 segundos
        return factory;
    }
}
