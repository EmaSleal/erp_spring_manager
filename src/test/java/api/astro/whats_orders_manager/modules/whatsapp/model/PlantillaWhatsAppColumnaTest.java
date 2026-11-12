package api.astro.whats_orders_manager.modules.whatsapp.model;

import jakarta.persistence.Column;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-031 (auditoría 7d #9) — createDate ya no declara @Column(name = ""): usa el nombre real del esquema
 * (create_date, el que Hibernate resolvía con la estrategia por defecto de Spring Boot para el atributo
 * createDate), de modo que ddl-auto=validate no cambia.
 */
@DisplayName("T-031 — PlantillaWhatsApp.createDate: nombre de columna explícito")
class PlantillaWhatsAppColumnaTest {

    @Test
    @DisplayName("createDate declara create_date, que coincide con lo que resolvía el nombre implícito")
    void createDate_columnaExplicita() throws Exception {
        Column column = PlantillaWhatsApp.class.getDeclaredField("createDate").getAnnotation(Column.class);
        PhysicalNamingStrategy naming = new CamelCaseToUnderscoresNamingStrategy();
        String implicita = naming.toPhysicalColumnName(Identifier.toIdentifier("createDate"), null).getText();

        assertThat(column.name()).isNotEmpty().isEqualTo("create_date").isEqualTo(implicita);
        assertThat(column.updatable()).isFalse();
    }
}
