package api.astro.whats_orders_manager.config;

import api.astro.whats_orders_manager.modules.configuracion.dto.ConfiguracionEmpresaDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-017 (ADR-020) — la aplicación usa el ObjectMapper autoconfigurado por Spring Boot:
 * ignora campos desconocidos y trabaja las fechas java.time como cadenas ISO-8601.
 */
@JsonTest
@DisplayName("T-017 — ObjectMapper autoconfigurado por Spring Boot")
class JacksonAutoConfigTest {

    record Fechas(LocalDate fecha, LocalDateTime momento) {}

    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("(a) un campo desconocido (faviconUrl) no impide deserializar ConfiguracionEmpresaDTO")
    void campoDesconocido_seIgnora() throws Exception {
        String json = """
                {"razonSocial": "EMANUEL SOTO LEAL", "codigoActividad": "4773.0",
                 "faviconUrl": "https://ejemplo.com/favicon.ico"}
                """;

        ConfiguracionEmpresaDTO dto = objectMapper.readValue(json, ConfiguracionEmpresaDTO.class);

        assertThat(dto.getRazonSocial()).isEqualTo("EMANUEL SOTO LEAL");
        assertThat(dto.getCodigoActividad()).isEqualTo("4773.0");
    }

    @Test
    @DisplayName("(b) LocalDate y LocalDateTime se serializan como cadenas ISO-8601")
    void fechas_seSerializanIso() throws Exception {
        Fechas fechas = new Fechas(LocalDate.of(2026, 10, 14), LocalDateTime.of(2026, 10, 14, 9, 30, 15));

        JsonNode nodo = objectMapper.readTree(objectMapper.writeValueAsString(fechas));

        assertThat(nodo.get("fecha").isTextual()).isTrue();
        assertThat(nodo.get("fecha").asText()).isEqualTo("2026-10-14");
        assertThat(nodo.get("momento").isTextual()).isTrue();
        assertThat(nodo.get("momento").asText()).isEqualTo("2026-10-14T09:30:15");
    }

    @Test
    @DisplayName("(c) LocalDate y LocalDateTime se deserializan desde ISO-8601")
    void fechas_seDeserializanDesdeIso() throws Exception {
        Fechas fechas = objectMapper.readValue(
                "{\"fecha\": \"2026-10-14\", \"momento\": \"2026-10-14T09:30:15\"}", Fechas.class);

        assertThat(fechas.fecha()).isEqualTo(LocalDate.of(2026, 10, 14));
        assertThat(fechas.momento()).isEqualTo(LocalDateTime.of(2026, 10, 14, 9, 30, 15));
    }

    @Test
    @DisplayName("ADR-020: ninguna clase @Configuration de la aplicación declara un @Bean ObjectMapper")
    void ningunBeanObjectMapperPropio() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Configuration.class));

        List<String> infractores = new ArrayList<>();
        for (var candidato : scanner.findCandidateComponents("api.astro.whats_orders_manager")) {
            Class<?> clase = Class.forName(candidato.getBeanClassName());
            for (Method metodo : clase.getDeclaredMethods()) {
                if (metodo.isAnnotationPresent(Bean.class) && ObjectMapper.class.isAssignableFrom(metodo.getReturnType())) {
                    infractores.add(clase.getName() + "#" + metodo.getName());
                }
            }
        }

        assertThat(infractores).isEmpty();
    }
}
