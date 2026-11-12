package api.astro.whats_orders_manager.modules.notificacion.service;

import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.model.PreferenciaNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.repository.PreferenciaNotificacionRepository;
import api.astro.whats_orders_manager.modules.notificacion.service.impl.PreferenciaNotificacionServiceImpl;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion.EMAIL;
import static api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion.WEB;
import static api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion.WHATSAPP;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-030 rev. 1 (P-032, criterios 15 y 16) — usuarioAceptaNotificacion decide con la fila más específica que exista,
 * en el orden (tipo, canal) → (tipo, NULL) → (NULL, canal) → (NULL, NULL); notificacionesDesactivadasGlobal bloquea
 * siempre y, sin ninguna fila aplicable, no se acepta. Cada caso parte de la fila global activa por defecto.
 * H2, JPA; cada prueba se revierte.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import(PreferenciaNotificacionServiceImpl.class)
@DisplayName("T-030 — precedencia de preferencias por especificidad")
class PreferenciaPrecedenciaJpaTest {

    private static final TipoNotificacion X = TipoNotificacion.FACTURA_VENCIDA;
    private static final TipoNotificacion Y = TipoNotificacion.PAGO_RECIBIDO;

    @Autowired private PreferenciaNotificacionService service;
    @Autowired private PreferenciaNotificacionRepository preferenciaRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setEmail("ana.t030@example.invalid");
        usuario.setPassword("x");
        usuario.setRol("ADMIN");
        usuario = usuarioRepository.save(usuario);
    }

    private PreferenciaNotificacion fila(TipoNotificacion tipo, CanalNotificacion canal, boolean activa) {
        return preferenciaRepository.save(PreferenciaNotificacion.builder()
                .usuario(usuario).tipoNotificacion(tipo).canal(canal).activa(activa).build());
    }

    private boolean acepta(TipoNotificacion tipo, CanalNotificacion canal) {
        return service.usuarioAceptaNotificacion(usuario, tipo, canal);
    }

    @Test
    @DisplayName("a) (X, EMAIL) inactiva bloquea X/EMAIL aunque la global esté activa")
    void especificaInactiva_bloquea() {
        fila(null, null, true);
        fila(X, EMAIL, false);

        assertThat(acepta(X, EMAIL)).isFalse();
        assertThat(service.usuarioAceptaNotificacionPorId(usuario.getIdUsuario(), X, EMAIL)).isFalse();
    }

    @Test
    @DisplayName("b) (X, WEB) activa acepta X/WEB")
    void especificaActiva_acepta() {
        fila(null, null, true);
        fila(X, WEB, true);

        assertThat(acepta(X, WEB)).isTrue();
    }

    @Test
    @DisplayName("c) un tipo sin filas propias sigue a la global")
    void tipoSinFilas_sigueGlobal() {
        fila(null, null, true);
        fila(X, EMAIL, false);

        assertThat(acepta(Y, EMAIL)).isTrue();
        assertThat(acepta(Y, WHATSAPP)).isTrue();
    }

    @Test
    @DisplayName("d) (X, NULL) inactiva bloquea X/WHATSAPP si no hay fila (X, WHATSAPP)")
    void porTipoInactiva_bloqueaCanalSinFila() {
        fila(null, null, true);
        fila(X, null, false);
        fila(X, WEB, true);

        assertThat(acepta(X, WHATSAPP)).isFalse();
        assertThat(acepta(X, WEB)).isTrue();
    }

    @Test
    @DisplayName("(NULL, canal) decide antes que la global y después que (tipo, NULL)")
    void porCanal_entreTipoYGlobal() {
        fila(null, null, true);
        fila(null, WHATSAPP, false);
        fila(Y, null, true);

        assertThat(acepta(X, WHATSAPP)).isFalse();
        assertThat(acepta(Y, WHATSAPP)).isTrue();
        assertThat(acepta(X, EMAIL)).isTrue();
    }

    @Test
    @DisplayName("e) notificacionesDesactivadasGlobal = true bloquea aunque la específica esté activa")
    void desactivadasGlobal_bloqueaSiempre() {
        PreferenciaNotificacion global = fila(null, null, true);
        global.setNotificacionesDesactivadasGlobal(true);
        preferenciaRepository.save(global);
        fila(X, EMAIL, true);

        assertThat(acepta(X, EMAIL)).isFalse();
    }

    @Test
    @DisplayName("Global inactiva y sin filas específicas: no acepta")
    void globalInactiva_bloquea() {
        fila(null, null, false);

        assertThat(acepta(Y, WEB)).isFalse();
    }

    @Test
    @DisplayName("Sin ninguna fila aplicable: no acepta (comportamiento previo)")
    void sinFilas_noAcepta() {
        assertThat(acepta(X, EMAIL)).isFalse();
    }

    @Test
    @DisplayName("Globales NULL/NULL duplicadas: decide la de menor id")
    void globalesDuplicadas_decideLaMenor() {
        fila(null, null, false);
        fila(null, null, true);

        assertThat(acepta(X, EMAIL)).isFalse();
    }
}
