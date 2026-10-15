package api.astro.whats_orders_manager.modules.configuracion.service;

import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.EmpresaRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.impl.EmpresaServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-016 (ADR-019) — Empresa única: getEmpresaPrincipal ya no crea una empresa en silencio,
 * save() rechaza una segunda empresa y update() conserva el flag activo (H2, JPA, caché real).
 *
 * Los métodos corren sin transacción de prueba (NOT_SUPPORTED) para que cada llamada al servicio
 * confirme como en producción; la caché "empresa" es la de Spring (proxy @Cacheable).
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({EmpresaServiceImpl.class, EmpresaUnicaJpaTest.CacheTestConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-016 — empresa única (H2)")
class EmpresaUnicaJpaTest {

    @TestConfiguration
    @EnableCaching
    static class CacheTestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("empresa");
        }
    }

    @Autowired private EmpresaService service;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private CacheManager cacheManager;

    @BeforeEach
    void limpiar() {
        empresaRepository.deleteAll();
        cacheManager.getCache("empresa").clear();
    }

    @AfterEach
    void limpiarDespues() {
        empresaRepository.deleteAll();
        cacheManager.getCache("empresa").clear();
    }

    private Empresa empresa(String nombre, boolean activo) {
        Empresa e = new Empresa();
        e.setNombreEmpresa(nombre);
        e.setActivo(activo);
        return empresaRepository.save(e);
    }

    @Test
    @DisplayName("tabla vacía → crea la empresa por defecto (instalación nueva)")
    void tablaVacia_creaEmpresaPorDefecto() {
        Empresa principal = service.getEmpresaPrincipal();

        assertThat(principal.getIdEmpresa()).isNotNull();
        assertThat(principal.getNombreEmpresa()).isEqualTo("Mi Empresa S.A.C.");
        assertThat(principal.getActivo()).isTrue();
        assertThat(empresaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("filas sin ninguna activa → IllegalStateException y no se crea ninguna fila")
    void filasSinActiva_lanzaYNoCrea() {
        empresa("Monrachem", false);

        assertThatThrownBy(() -> service.getEmpresaPrincipal())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No hay una empresa activa configurada");

        assertThat(empresaRepository.count()).isEqualTo(1);
        assertThat(empresaRepository.contarEmpresasActivas()).isZero();
    }

    @Test
    @DisplayName("dos activas → IllegalStateException (sin NonUniqueResult críptico)")
    void dosActivas_lanza() {
        Empresa a = empresa("Monrachem", true);
        Empresa b = empresa("Otra", true);

        assertThatThrownBy(() -> service.getEmpresaPrincipal())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Hay 2 empresas activas")
                .hasMessageContaining(a.getIdEmpresa() + ", " + b.getIdEmpresa());
        assertThatThrownBy(() -> service.findEmpresaActiva())
                .isInstanceOf(IllegalStateException.class);
        assertThat(empresaRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("save() con una empresa existente → rechazo; la existente sigue activa")
    void save_conEmpresaExistente_rechaza() {
        Empresa existente = empresa("Monrachem", true);

        Empresa nueva = new Empresa();
        nueva.setNombreEmpresa("Mi Empresa S.A.C.");

        assertThatThrownBy(() -> service.save(nueva, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe una empresa");
        assertThatThrownBy(() -> service.saveOrUpdate(nueva, 1))
                .isInstanceOf(IllegalStateException.class);

        assertThat(empresaRepository.count()).isEqualTo(1);
        assertThat(empresaRepository.findById(existente.getIdEmpresa()).orElseThrow().getActivo()).isTrue();
    }

    @Test
    @DisplayName("update() con activo = false en el objeto recibido → sigue activa en la BD")
    void update_ignoraActivoRecibido() {
        Empresa existente = empresa("Monrachem", true);

        Empresa formulario = new Empresa();
        formulario.setIdEmpresa(existente.getIdEmpresa());
        formulario.setNombreEmpresa("Monrachem S.A.");
        formulario.setActivo(false);

        Empresa actualizada = service.update(formulario, 1);

        assertThat(actualizada.getActivo()).isTrue();
        Empresa enBd = empresaRepository.findById(existente.getIdEmpresa()).orElseThrow();
        assertThat(enBd.getActivo()).isTrue();
        assertThat(enBd.getNombreEmpresa()).isEqualTo("Monrachem S.A.");

        formulario.setActivo(false);
        service.saveOrUpdate(formulario, 1);
        assertThat(empresaRepository.findById(existente.getIdEmpresa()).orElseThrow().getActivo()).isTrue();
    }

    @Test
    @DisplayName("tras un error, una llamada posterior con datos corregidos funciona (la caché no guardó el error)")
    void error_noQuedaEnCache() {
        Empresa inactiva = empresa("Monrachem", false);

        assertThatThrownBy(() -> service.getEmpresaPrincipal())
                .isInstanceOf(IllegalStateException.class);
        assertThat(cacheManager.getCache("empresa").get("principal")).isNull();

        // Corrección de datos fuera del servicio (como V31)
        inactiva.setActivo(true);
        empresaRepository.save(inactiva);

        Empresa principal = service.getEmpresaPrincipal();
        assertThat(principal.getIdEmpresa()).isEqualTo(inactiva.getIdEmpresa());
        assertThat(principal.getNombreEmpresa()).isEqualTo("Monrachem");
        assertThat(cacheManager.getCache("empresa").get("principal")).isNotNull();
        assertThat(empresaRepository.count()).isEqualTo(1);
    }
}
