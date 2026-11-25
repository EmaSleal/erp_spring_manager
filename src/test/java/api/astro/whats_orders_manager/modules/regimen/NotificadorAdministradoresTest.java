package api.astro.whats_orders_manager.modules.regimen;

import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.service.NotificacionService;
import api.astro.whats_orders_manager.modules.regimen.aviso.NotificadorAdministradores;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** T-056: el aviso llega por notificación web a cada administrador activo, y un fallo con uno no impide avisar a los demás. */
@DisplayName("T-056 — notificación a los administradores")
class NotificadorAdministradoresTest {

    private static Usuario admin(boolean activo) {
        Usuario u = mock(Usuario.class);
        when(u.getActivo()).thenReturn(activo);
        when(u.getBloqueado()).thenReturn(false);
        return u;
    }

    @Test
    @DisplayName("notifica por WEB (tipo SISTEMA) a cada ADMIN activo y salta a los inactivos")
    void notificaALosAdminActivos() {
        UsuarioService usuarios = mock(UsuarioService.class);
        NotificacionService notificaciones = mock(NotificacionService.class);
        Usuario a = admin(true);
        Usuario b = admin(true);
        Usuario inactivo = admin(false);
        when(usuarios.findByRol("ADMIN")).thenReturn(List.of(a, inactivo, b));

        new NotificadorAdministradores(usuarios, notificaciones).notificar("Título", "Mensaje");

        verify(notificaciones).enviarNotificacion(a, TipoNotificacion.SISTEMA, CanalNotificacion.WEB, "Título", "Mensaje");
        verify(notificaciones).enviarNotificacion(b, TipoNotificacion.SISTEMA, CanalNotificacion.WEB, "Título", "Mensaje");
        verify(notificaciones, never()).enviarNotificacion(inactivo, TipoNotificacion.SISTEMA, CanalNotificacion.WEB, "Título", "Mensaje");
    }

    @Test
    @DisplayName("un fallo al notificar a uno no impide avisar a los demás ni se propaga")
    void unFalloNoDetieneLosDemas() {
        UsuarioService usuarios = mock(UsuarioService.class);
        NotificacionService notificaciones = mock(NotificacionService.class);
        Usuario a = admin(true);
        Usuario b = admin(true);
        when(usuarios.findByRol("ADMIN")).thenReturn(List.of(a, b));
        doThrow(new IllegalStateException("x")).when(notificaciones)
                .enviarNotificacion(a, TipoNotificacion.SISTEMA, CanalNotificacion.WEB, "T", "M");

        assertThatCode(() -> new NotificadorAdministradores(usuarios, notificaciones).notificar("T", "M")).doesNotThrowAnyException();
        verify(notificaciones).enviarNotificacion(b, TipoNotificacion.SISTEMA, CanalNotificacion.WEB, "T", "M");
    }
}
