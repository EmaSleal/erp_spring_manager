package api.astro.whats_orders_manager.modules.seguridad.controller;

import api.astro.whats_orders_manager.modules.rrhh.model.Ausencia;
import api.astro.whats_orders_manager.modules.rrhh.model.Departamento;
import api.astro.whats_orders_manager.modules.rrhh.model.Empleado;
import api.astro.whats_orders_manager.modules.rrhh.repository.AusenciaRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.EmpleadoRepository;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PerfilController.verPerfil() — PR3 self-service hub logic.
 *
 * Covers:
 * PR3.2.2 — verPerfil with no linked empleado hides hub sections
 * PR3.2.3 — verPerfil for a jefe shows pending ausencias of the department
 *
 * RED phase: tests reference constructor args (empleadoRepository, ausenciaRepository)
 * that do not exist yet in PerfilController — will fail with compilation error.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PerfilController — employee hub logic")
class PerfilControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmpleadoRepository empleadoRepository;

    @Mock
    private AusenciaRepository ausenciaRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @Mock
    private Model model;

    private PerfilController controller;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        // RED: this constructor invocation will fail to compile until
        // PerfilController adds empleadoRepository and ausenciaRepository params.
        controller = new PerfilController(usuarioService, passwordEncoder, empleadoRepository, ausenciaRepository);

        usuario = new Usuario();
        usuario.setIdUsuario(10);
        usuario.setNombre("Test User");
        usuario.setTelefono("88001100");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("88001100");
        when(usuarioService.findByEmail("88001100")).thenReturn(Optional.of(usuario));
    }

    // =========================================================================
    // PR3.2.2 — verPerfil with no linked empleado hides hub sections
    // =========================================================================

    @Test
    @DisplayName("verPerfil with no empleado linked sets empleado=null and esJefe=false")
    void verPerfil_withNoEmpleado_hidesHubSections() {
        // GIVEN: no employee linked to this user
        when(empleadoRepository.findByUsuarioId(10)).thenReturn(Optional.empty());

        // WHEN
        String view = controller.verPerfil(model, authentication);

        // THEN
        assertThat(view).isEqualTo("modules/seguridad/perfil/ver");
        verify(model).addAttribute("empleado", null);
        verify(model).addAttribute("esJefe", false);
        verify(model).addAttribute(eq("misAusencias"), eq(List.of()));
        verify(model).addAttribute(eq("ausenciasPendientesDept"), eq(List.of()));
        // ausenciaRepository must NOT be called when no empleado found
        verify(ausenciaRepository, never()).findByEmpleadoId(anyLong());
        verify(ausenciaRepository, never()).findPendientesByJefeId(anyLong());
    }

    // =========================================================================
    // PR3.2.3 — verPerfil for a jefe shows pending ausencias of the department
    // =========================================================================

    @Test
    @DisplayName("verPerfil for a jefe sets esJefe=true and populates ausenciasPendientesDept")
    void verPerfil_withJefe_showsPendingAusencias() {
        // GIVEN: build jefe scenario
        Empleado jefe = new Empleado();
        jefe.setId(5L);
        jefe.setNombre("Ana");
        jefe.setPrimerApellido("García");
        jefe.setActivo(true);

        Departamento departamento = new Departamento();
        departamento.setId(1L);
        departamento.setNombre("Sistemas");
        departamento.setJefe(jefe);

        jefe.setDepartamento(departamento);

        Ausencia pendiente = new Ausencia();
        pendiente.setId(200L);
        pendiente.setAprobada(false);
        pendiente.setEmpleado(jefe);

        when(empleadoRepository.findByUsuarioId(10)).thenReturn(Optional.of(jefe));
        when(ausenciaRepository.findByEmpleadoId(5L)).thenReturn(List.of());
        when(ausenciaRepository.findPendientesByJefeId(5L)).thenReturn(List.of(pendiente));

        // WHEN
        String view = controller.verPerfil(model, authentication);

        // THEN
        assertThat(view).isEqualTo("modules/seguridad/perfil/ver");
        verify(model).addAttribute("empleado", jefe);
        verify(model).addAttribute("esJefe", true);
        verify(model).addAttribute(eq("ausenciasPendientesDept"), argThat(list ->
                list instanceof List && ((List<?>) list).size() == 1));
    }
}
