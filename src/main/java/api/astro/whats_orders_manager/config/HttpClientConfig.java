package api.astro.whats_orders_manager.config;

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
