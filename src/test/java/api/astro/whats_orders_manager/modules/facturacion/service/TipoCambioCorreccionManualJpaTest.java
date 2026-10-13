package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.dto.TasaDelDia;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoSincronizacionTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.enums.FuenteTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.model.TipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.repository.TipoCambioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-008 revisión 1 — Corrección manual de un día ya sincronizado contra la restricción única real (H2).
 *
 * El esquema H2 se genera desde la entidad, que declara la misma restricción que V26:
 * única (fecha, moneda_origen, moneda_destino, activo_unico), con activo_unico = 1 si la fila está activa
 * y NULL si no. Así se ejercita en la base el orden "desactivar la fila activa → insertar la nueva".
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@DisplayName("TipoCambioService — corrección manual con la restricción de una sola tasa activa por día (H2)")
class TipoCambioCorreccionManualJpaTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 11);

    @Autowired
    private TipoCambioRepository repo;

    @Autowired
    private TestEntityManager em;

    private TipoCambioService service;

    @BeforeEach
    void setUp() {
        service = new TipoCambioService(repo);
    }

    @Test
    @DisplayName("la base rechaza dos filas activas del mismo día y par de monedas, y acepta varias inactivas")
    void restriccion_unaActivaPorDia_variasInactivas() {
        persistir(FuenteTipoCambio.API_EXTERNA, "449.86", "455.60", false);
        persistir(FuenteTipoCambio.MANUAL, "450.00", "456.00", false);
        persistir(FuenteTipoCambio.MANUAL, "451.00", "457.00", true);

        assertThat(repo.count()).isEqualTo(3);
        assertThatThrownBy(() -> persistir(FuenteTipoCambio.MANUAL, "452.00", "458.00", true))
                .hasMessageContaining("UK_TIPOS_CAMBIO_FECHA_ORIGEN_DESTINO_ACTIVO");
    }

    @Test
    @DisplayName("criterio 10: con API_EXTERNA activa, registrar MANUAL del mismo día funciona; API_EXTERNA queda inactiva y obtenerTasaVenta devuelve la MANUAL")
    void manualSobreSincronizada_reemplazaLaActiva() {
        TipoCambio api = persistir(FuenteTipoCambio.API_EXTERNA, "449.86", "455.60", true);
        // La fila activa queda administrada en el contexto de persistencia, como en una petición real
        // que la consultó antes: la desactivación debe llegar a la base antes del INSERT de la nueva.
        assertThat(repo.findByFechaAndMonedaDestinoAndActivoTrue(DIA, MonedaFE.USD)).isPresent();

        TipoCambio manual = service.registrar(manual("450.00", "460.00"));
        sincronizarConBd();

        assertThat(repo.findById(api.getId())).get().satisfies(tc -> {
            assertThat(tc.isActivo()).isFalse();
            assertThat(tc.getFuente()).isEqualTo(FuenteTipoCambio.API_EXTERNA);
        });
        assertThat(repo.findById(manual.getId())).get().satisfies(tc -> {
            assertThat(tc.isActivo()).isTrue();
            assertThat(tc.getFuente()).isEqualTo(FuenteTipoCambio.MANUAL);
        });
        assertThat(service.obtenerTasaVenta(MonedaFE.USD, DIA)).isEqualByComparingTo("460.00");
    }

    @Test
    @DisplayName("criterio 11: dos correcciones manuales sucesivas del mismo día funcionan; solo la última queda activa")
    void dosCorreccionesManuales_soloLaUltimaActiva() {
        persistir(FuenteTipoCambio.API_EXTERNA, "449.86", "455.60", true);

        TipoCambio primera = service.registrar(manual("450.00", "460.00"));
        sincronizarConBd();
        TipoCambio segunda = service.registrar(manual("451.00", "461.00"));
        sincronizarConBd();

        List<TipoCambio> filas = repo.findAll().stream().sorted(Comparator.comparing(TipoCambio::getId)).toList();
        assertThat(filas).hasSize(3);
        assertThat(filas).filteredOn(TipoCambio::isActivo).singleElement()
                .extracting(TipoCambio::getId).isEqualTo(segunda.getId());
        assertThat(filas).filteredOn(tc -> !tc.isActivo()).extracting(TipoCambio::getId)
                .contains(primera.getId()).hasSize(2);
        assertThat(service.obtenerTasaVenta(MonedaFE.USD, DIA)).isEqualByComparingTo("461.00");
    }

    @Test
    @DisplayName("criterio 12: con una fila inactiva y ninguna activa, registrarDesdeProveedor crea la nueva activa (CREADO)")
    void inactivaSinActiva_proveedorCreaNuevaActiva() {
        TipoCambio inactiva = persistir(FuenteTipoCambio.MANUAL, "440.00", "450.00", false);

        EstadoSincronizacionTipoCambio[] estado = new EstadoSincronizacionTipoCambio[1];
        assertThatCode(() -> {
            estado[0] = service.registrarDesdeProveedor(MonedaFE.USD,
                    new TasaDelDia(DIA, new BigDecimal("449.86"), new BigDecimal("455.60")),
                    FuenteTipoCambio.API_EXTERNA);
            sincronizarConBd();
        }).doesNotThrowAnyException();

        assertThat(estado[0]).isEqualTo(EstadoSincronizacionTipoCambio.CREADO);
        assertThat(repo.findAll()).hasSize(2);
        assertThat(repo.findById(inactiva.getId())).get().extracting(TipoCambio::isActivo).isEqualTo(false);
        assertThat(repo.findByFechaAndMonedaDestinoAndActivoTrue(DIA, MonedaFE.USD)).get().satisfies(tc -> {
            assertThat(tc.getFuente()).isEqualTo(FuenteTipoCambio.API_EXTERNA);
            assertThat(tc.getTasaVenta()).isEqualByComparingTo("455.60");
        });
    }

    @Test
    @DisplayName("criterio 13: tras una corrección MANUAL, una nueva sincronización del mismo día devuelve CONSERVADO y no modifica nada")
    void sincronizacionTrasCorreccionManual_conserva() {
        persistir(FuenteTipoCambio.API_EXTERNA, "449.86", "455.60", true);
        TipoCambio manual = service.registrar(manual("450.00", "460.00"));
        sincronizarConBd();
        List<String> antes = instantanea();

        EstadoSincronizacionTipoCambio estado = service.registrarDesdeProveedor(MonedaFE.USD,
                new TasaDelDia(DIA, new BigDecimal("452.00"), new BigDecimal("458.00")),
                FuenteTipoCambio.API_EXTERNA);
        sincronizarConBd();

        assertThat(estado).isEqualTo(EstadoSincronizacionTipoCambio.CONSERVADO);
        assertThat(instantanea()).isEqualTo(antes);
        assertThat(repo.findByFechaAndMonedaDestinoAndActivoTrue(DIA, MonedaFE.USD)).get()
                .extracting(TipoCambio::getId).isEqualTo(manual.getId());
    }

    private List<String> instantanea() {
        return repo.findAll().stream()
                .sorted(Comparator.comparing(TipoCambio::getId))
                .map(tc -> tc.getId() + "|" + tc.getFuente() + "|" + tc.isActivo() + "|"
                        + tc.getTasaCompra().stripTrailingZeros().toPlainString() + "|"
                        + tc.getTasaVenta().stripTrailingZeros().toPlainString())
                .toList();
    }

    private TipoCambio manual(String compra, String venta) {
        return TipoCambio.builder()
                .monedaOrigen(MonedaFE.CRC)
                .monedaDestino(MonedaFE.USD)
                .fecha(DIA)
                .tasaCompra(new BigDecimal(compra))
                .tasaVenta(new BigDecimal(venta))
                .fuente(FuenteTipoCambio.MANUAL)
                .creadoPor("admin")
                .build();
    }

    private TipoCambio persistir(FuenteTipoCambio fuente, String compra, String venta, boolean activo) {
        TipoCambio tc = em.persistAndFlush(TipoCambio.builder()
                .monedaOrigen(MonedaFE.CRC)
                .monedaDestino(MonedaFE.USD)
                .fecha(DIA)
                .tasaCompra(new BigDecimal(compra))
                .tasaVenta(new BigDecimal(venta))
                .fuente(fuente)
                .activo(activo)
                .build());
        em.clear();
        return tc;
    }

    private void sincronizarConBd() {
        em.flush();
        em.clear();
    }
}
