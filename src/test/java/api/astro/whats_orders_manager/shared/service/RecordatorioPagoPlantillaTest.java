package api.astro.whats_orders_manager.shared.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** T-047 (review 1, criterio 32): el correo de recordatorio muestra el saldo pendiente, no el total. */
@DisplayName("T-047 — plantilla email/recordatorio-pago: saldo pendiente")
class RecordatorioPagoPlantillaTest {

    @Test
    @DisplayName("factura de 11500 con 1334,56 acreditados: muestra 10165.44 como saldo y no 11500.00 como monto a pagar")
    void muestraElSaldo() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/modules/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        // La plantilla lee numero/iva/montoIva (no existen en la entidad): se usa un mapa y el saldo va aparte
        java.util.Map<String, Object> f = new java.util.HashMap<>();
        f.put("numero", "F-9");
        f.put("subtotal", new BigDecimal("10176.99"));
        f.put("iva", 13);
        f.put("montoIva", new BigDecimal("1323.01"));
        f.put("total", new BigDecimal("11500.00"));
        f.put("fechaEmision", java.time.LocalDate.now());
        f.put("fechaVencimiento", java.time.LocalDate.now());
        Context ctx = new Context(java.util.Locale.US);
        ctx.setVariable("factura", f);
        ctx.setVariable("saldoPendiente", new BigDecimal("10165.44"));
        java.util.Map<String, Object> cliente = java.util.Map.of("nombre", "Cliente de prueba");
        ctx.setVariable("cliente", cliente);
        ctx.setVariable("lineas", java.util.List.of());
        ctx.setVariable("empresa", new java.util.HashMap<String, Object>(java.util.Map.of("nombre", "Empresa",
                "aceptaEfectivo", true, "aceptaTarjeta", true, "banco", "Banco", "clabe", "1", "direccion", "d",
                "email", "e@example.invalid", "numeroCuenta", "1", "telefono", "1")));
        ctx.setVariable("simboloMoneda", "₡");
        ctx.setVariable("diasRetraso", 3L);
        ctx.setVariable("fechaActual", java.time.LocalDate.now());

        String html = engine.process("email/recordatorio-pago", ctx);

        assertThat(html).contains("₡ 10165.44").contains("Saldo pendiente");
        assertThat(html).doesNotContain("Monto Total a Pagar");
    }
}
