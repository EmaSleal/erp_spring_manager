package api.astro.whats_orders_manager.modules.notificacion.service.impl;

import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.model.Notificacion;
import api.astro.whats_orders_manager.modules.notificacion.model.PreferenciaNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.repository.NotificacionRepository;
import api.astro.whats_orders_manager.modules.notificacion.repository.PreferenciaNotificacionRepository;
import api.astro.whats_orders_manager.shared.service.EmailService;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * T-030 rev. 1 (P-032, criterio 16) — con la global activa y (X, EMAIL) inactiva, enviarNotificacion no crea ni envía
 * la notificación X por EMAIL. Usa el servicio de preferencias real sobre un repositorio simulado.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("T-030 — el envío respeta la preferencia específica inactiva")
class NotificacionServicePreferenciasTest {

    private static final TipoNotificacion X = TipoNotificacion.FACTURA_VENCIDA;

    @Mock private NotificacionRepository notificacionRepository;
    @Mock private PreferenciaNotificacionRepository preferenciaRepository;
    @Mock private EmailService emailService;

    @InjectMocks private NotificacionServiceImpl notificacionService;

    private Usuario usuario;

    @BeforeEach
    void preparar() {
        PreferenciaNotificacionServiceImpl preferenciaService = new PreferenciaNotificacionServiceImpl();
        ReflectionTestUtils.setField(preferenciaService, "preferenciaRepository", preferenciaRepository);
        ReflectionTestUtils.setField(notificacionService, "preferenciaService", preferenciaService);

        usuario = new Usuario();
        usuario.setIdUsuario(7);
        usuario.setEmail("ana.t030@example.invalid");
    }

    private PreferenciaNotificacion fila(int id, TipoNotificacion tipo, CanalNotificacion canal, boolean activa) {
        return PreferenciaNotificacion.builder()
                .idPreferencia(id).usuario(usuario).tipoNotificacion(tipo).canal(canal).activa(activa).build();
    }

    @Test
    @DisplayName("a) (X, EMAIL) inactiva: no se guarda ni se envía la notificación")
    void especificaInactiva_noCreaNiEnvia() {
        when(preferenciaRepository.findByUsuarioId(7)).thenReturn(List.of(
                fila(1, null, null, true),
                fila(2, X, CanalNotificacion.EMAIL, false)));

        Notificacion resultado = notificacionService.enviarNotificacion(
                usuario, X, CanalNotificacion.EMAIL, "Factura vencida", "La factura 1 venció");

        assertThat(resultado).isNull();
        verify(notificacionRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }
}
