package api.astro.whats_orders_manager.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuración del cliente REST para WhatsApp Business API
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - Fase 1.3
 */
@Configuration
public class WhatsAppRestConfig {
    
    /**
     * Crea un RestTemplate configurado para llamadas a Meta WhatsApp API
     * con timeouts y manejo de errores
     * 
     * @param builder Builder de RestTemplate
     * @return RestTemplate configurado
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .additionalInterceptors(loggingInterceptor())
                .build();
    }
    
    /**
     * Interceptor para logging de requests/responses
     */
    private ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            // Puede agregarse logging más detallado aquí si se necesita
            return execution.execute(request, body);
        };
    }
}
