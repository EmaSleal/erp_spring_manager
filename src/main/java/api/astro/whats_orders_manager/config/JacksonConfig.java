package api.astro.whats_orders_manager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson configuration to support java.time types (LocalDateTime, LocalDate, etc.).
 *
 * Registers {@link JavaTimeModule} so that LocalDateTime fields in entities and DTOs
 * serialize/deserialize as ISO-8601 strings (e.g., "2025-07-16T10:30:00") rather
 * than as numeric arrays or epoch timestamps.
 *
 * This replaces the need for @JsonSerialize/@JsonDeserialize annotations on every field.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
