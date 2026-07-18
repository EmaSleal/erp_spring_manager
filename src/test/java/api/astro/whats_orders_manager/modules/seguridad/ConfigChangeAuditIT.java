package api.astro.whats_orders_manager.modules.seguridad;

import api.astro.whats_orders_manager.modules.seguridad.controller.PermisoAdminController;
import api.astro.whats_orders_manager.modules.seguridad.model.Permiso;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration-style tests asserting that UsuarioActividad PERMISSION_CHANGE
 * audit rows are triggered when a permission is updated via PermisoAdminController.
 *
 * Uses Mockito instead of a live DB to verify the wiring between the controller
 * and UsuarioActividadService without requiring MySQL connectivity.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Config/Permission change audit wiring — controller → service integration")
class ConfigChangeAuditIT {

    @Mock
    private PermisoService permisoService;

    @Mock
    private UsuarioActividadService usuarioActividadService;

    @Mock
    private RedirectAttributes redirectAttributes;

    private PermisoAdminController controller;

    private Permiso testPermiso;

    @BeforeEach
    void setUp() {
        controller = new PermisoAdminController(permisoService, usuarioActividadService);

        testPermiso = new Permiso();
        testPermiso.setIdPermiso(10L);
        testPermiso.setCodigo("FACTURA_VER");
        testPermiso.setNombre("Ver Facturas");
        testPermiso.setDescripcion("Permite ver facturas");
        testPermiso.setCategoria("FACTURACION");
        testPermiso.setActivo(true);
        testPermiso.setEsCritico(false);
    }

    @Test
    @DisplayName("actualizarPermiso triggers registrarCambioPermiso audit call")
    void actualizarPermiso_callsRegistrarCambioPermiso() {
        when(permisoService.buscarPorId(10L)).thenReturn(Optional.of(testPermiso));

        controller.actualizarPermiso(10L, "Ver Facturas v2", "Updated description",
                "FACTURACION", true, redirectAttributes);

        verify(usuarioActividadService).registrarCambioPermiso(anyString(), anyString());
    }

    @Test
    @DisplayName("cambiarEstado triggers registrarCambioPermiso audit call")
    void cambiarEstado_callsRegistrarCambioPermiso() {
        when(permisoService.buscarPorId(10L)).thenReturn(Optional.of(testPermiso));

        controller.cambiarEstado(10L, redirectAttributes);

        verify(usuarioActividadService).registrarCambioPermiso(anyString(), anyString());
    }

    @Test
    @DisplayName("toggleCritico triggers registrarCambioPermiso audit call")
    void toggleCritico_callsRegistrarCambioPermiso() {
        when(permisoService.buscarPorId(10L)).thenReturn(Optional.of(testPermiso));

        controller.toggleCritico(10L, redirectAttributes);

        verify(usuarioActividadService).registrarCambioPermiso(anyString(), anyString());
    }

    @Test
    @DisplayName("actualizarPermiso does NOT call audit when permiso not found")
    void actualizarPermiso_permisoNotFound_noAuditCall() {
        when(permisoService.buscarPorId(99L)).thenReturn(Optional.empty());

        controller.actualizarPermiso(99L, "Name", "Desc",
                "CAT", false, redirectAttributes);

        verify(usuarioActividadService, never()).registrarCambioPermiso(anyString(), anyString());
    }
}
