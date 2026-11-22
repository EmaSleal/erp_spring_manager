package api.astro.whats_orders_manager.modules.facturacion;

import api.astro.whats_orders_manager.config.MysqlZonaCostaRica;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.MetodoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoPago;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.scheduler.RecordatorioPagoScheduler;
import api.astro.whats_orders_manager.modules.facturacion.service.PagoService;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.service.NotificacionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * T-050 (criterio 11, ADR-060, P-112): con el reloj de la aplicación fijo a las 20:00 de Costa Rica (02:00 UTC del día
 * siguiente), «hoy» es el día de Costa Rica: el pago de hoy cuenta en los pagos del día y en su conteo, la factura que
 * vence hoy no sale como vencida y la que venció ayer sí. El reloj fijo cae en el pasado (2026-11-20) para que el
 * resultado no dependa de la hora real de la corrida ni de la fecha de la base.
 *
 * Base: MySQL 8 en Testcontainers (t050-it-mysql-reloj, America/Costa_Rica, tmpfs); esquema de Hibernate.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@Testcontainers
@DisplayName("T-050 — fecha de negocio desde el Clock a las 20:00 de Costa Rica (MySQL)")
class RelojFijoNegocioIT {

    static final ZoneId CR = ZoneId.of("America/Costa_Rica");
    /** 2026-11-20 20:00 en Costa Rica = 2026-11-21 02:00 UTC. */
    static final ZonedDateTime AHORA = ZonedDateTime.of(2026, 11, 20, 20, 0, 0, 0, CR);
    static final LocalDate HOY = AHORA.toLocalDate();

    @Container
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedorConRutinas("t050", "t050-it-mysql-reloj");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojFijoACostaRica2000() {
            return Clock.fixed(AHORA.toInstant(), CR);
        }
    }

    @MockitoBean private NotificacionService notificaciones;
    /** Los asientos no son el objeto de esta prueba (sin plan contable sembrado). */
    @MockitoBean private api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService asientos;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private FacturaRepository facturaRepository;
    @Autowired private PagoService pagoService;
    @Autowired private RecordatorioPagoScheduler scheduler;

    private Cliente cliente() {
        Cliente c = new Cliente();
        c.setNombre("Cliente T050 " + UUID.randomUUID());
        c.setEmail("cliente@example.invalid");
        c.setTelefono("+50688887777");
        return clienteRepository.save(c);
    }

    private Factura factura(Cliente c, String numero, LocalDate vence) {
        Factura f = new Factura();
        f.setCliente(c);
        f.setNumeroFactura(numero);
        f.setMonedaFE(MonedaFE.CRC);
        f.setTipoCambio(BigDecimal.ONE);
        f.setSubtotal(new BigDecimal("1000.00"));
        f.setIgv(BigDecimal.ZERO);
        f.setTotal(new BigDecimal("1000.00"));
        f.setAnulada(false);
        f.setEntregado(true);
        f.setFechaPago(Date.valueOf(vence));
        return facturaRepository.save(f);
    }

    @Test
    @DisplayName("a las 20:00 de CR, un pago de hoy cuenta en los pagos del día, su conteo y los ingresos del día; el de ayer no")
    void pagoDeHoy_cuentaEnPagosDelDia() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        long idHoy = tx.execute(s -> {
            Cliente c = cliente();
            Pago p = new Pago();
            p.setCliente(c);
            p.setFactura(factura(c, "T050-PH-" + UUID.randomUUID().toString().substring(0, 6), HOY.plusDays(30)));
            p.setTipoPago(TipoPago.PARCIAL);
            p.setMonto(new BigDecimal("400.00"));
            p.setMetodoPago(MetodoPago.EFECTIVO);
            p.setEstado(EstadoPago.CONFIRMADO);
            // sin fechaPago: el servicio la toma del Clock (20:00 de CR)
            return pagoService.registrarPago(p).getIdPago();
        });
        long idAyer = tx.execute(s -> {
            Cliente c = cliente();
            Pago p = new Pago();
            p.setCliente(c);
            p.setFactura(factura(c, "T050-PA-" + UUID.randomUUID().toString().substring(0, 6), HOY.plusDays(30)));
            p.setTipoPago(TipoPago.PARCIAL);
            p.setMonto(new BigDecimal("100.00"));
            p.setMetodoPago(MetodoPago.EFECTIVO);
            p.setEstado(EstadoPago.CONFIRMADO);
            p.setFechaPago(LocalDateTime.of(HOY.minusDays(1), java.time.LocalTime.of(23, 59)));
            return pagoService.registrarPago(p).getIdPago();
        });

        Pago guardado = tx.execute(s -> pagoService.findById(idHoy).orElseThrow());
        assertThat(guardado.getFechaPago()).isEqualTo(LocalDateTime.of(HOY, java.time.LocalTime.of(20, 0)));

        List<Long> delDia = tx.execute(s -> pagoService.findPagosDelDia().stream().map(Pago::getIdPago).toList());
        assertThat(delDia).contains(idHoy).doesNotContain(idAyer);
        assertThat(pagoService.countPagosDelDia()).isEqualTo(delDia.size()).isGreaterThanOrEqualTo(1);
        assertThat(pagoService.sumIngresosDiaActual()).isGreaterThanOrEqualTo(new BigDecimal("400.00"));
    }

    @Test
    @DisplayName("a las 20:00 de CR, la factura que vence hoy no recibe recordatorio de vencida y la que venció ayer sí")
    void vencimiento_hoyNoEsVencida_ayerSi() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        String sufijo = UUID.randomUUID().toString().substring(0, 6);
        String venceHoy = "T050-VH-" + sufijo;
        String venceAyer = "T050-VA-" + sufijo;
        tx.executeWithoutResult(s -> {
            factura(cliente(), venceHoy, HOY);
            factura(cliente(), venceAyer, HOY.minusDays(1));
        });

        // en sesión: el scheduler real corre sin ella y las colecciones perezosas de la factura no se leen fuera de una
        tx.executeWithoutResult(s -> scheduler.enviarRecordatoriosPago());

        ArgumentCaptor<String> mensajes = ArgumentCaptor.forClass(String.class);
        verify(notificaciones, atLeastOnce()).enviarNotificacionExterna(anyString(),
                eq(TipoNotificacion.FACTURA_VENCIDA), any(), anyString(), mensajes.capture());
        assertThat(mensajes.getAllValues()).anyMatch(m -> m.contains(venceAyer)).noneMatch(m -> m.contains(venceHoy));
    }

    @Test
    @DisplayName("findFacturasConPagoVencido(hoy) del repositorio: vence hoy no, venció ayer sí")
    void repositorioVencidas() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        String sufijo = UUID.randomUUID().toString().substring(0, 6);
        List<String> numeros = tx.execute(s -> {
            factura(cliente(), "T050-RH-" + sufijo, HOY);
            factura(cliente(), "T050-RA-" + sufijo, HOY.minusDays(1));
            return facturaRepository.findFacturasConPagoVencido(Date.valueOf(HOY)).stream()
                    .map(Factura::getNumeroFactura).toList();
        });
        assertThat(numeros).contains("T050-RA-" + sufijo).doesNotContain("T050-RH-" + sufijo);
    }
}
