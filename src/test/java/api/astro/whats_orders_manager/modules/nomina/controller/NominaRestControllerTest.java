package api.astro.whats_orders_manager.modules.nomina.controller;

import api.astro.whats_orders_manager.modules.nomina.dto.NominaDTO;
import api.astro.whats_orders_manager.modules.nomina.enums.EstadoNomina;
import api.astro.whats_orders_manager.modules.nomina.enums.TipoNomina;
import api.astro.whats_orders_manager.modules.nomina.service.NominaService;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import api.astro.whats_orders_manager.shared.dto.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD RED phase — NominaRestController unit tests.
 *
 * Written BEFORE NominaRestController implementation exists.
 * Uses Mockito (no Spring context) — verifies permission guard and response shape.
 *
 * For security scenarios (403), the controller delegates the permission check to
 * permisoService.tienePermisoPorCodigo(...) via @PreAuthorize. We test the logic
 * directly by calling the method when permisoService returns false, which simulates
 * what the security layer does.
 *
 * Scenarios:
 * 3.3.1 — POST /api/nomina without NOMINA_CREAR → 403 (permission check fails)
 * 3.3.2 — POST /api/nomina/{id}/calcular without NOMINA_CALCULAR → 403
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NominaRestController — security and response tests")
class NominaRestControllerTest {

    @Mock
    private NominaService nominaService;

    @Mock
    private PermisoService permisoService;

    @Mock
    private Authentication authentication;

    private NominaRestController controller;

    @BeforeEach
    void setUp() {
        controller = new NominaRestController(nominaService, permisoService);
        when(authentication.getName()).thenReturn("user@test.com");
    }

    // =========================================================================
    // 3.3.1 — crear without NOMINA_CREAR returns 403
    // =========================================================================

    @Test
    @DisplayName("3.3.1 crear() returns 403 when user lacks NOMINA_CREAR permission")
    void crear_withoutPermission_returns403() {
        // GIVEN: user does not have NOMINA_CREAR
        when(permisoService.tienePermisoPorCodigo("user@test.com", "NOMINA_CREAR"))
                .thenReturn(false);

        Map<String, String> body = Map.of(
                "periodoInicio", "2026-07-01",
                "periodoFin", "2026-07-31",
                "fechaPago", "2026-08-05",
                "tipo", "MENSUAL"
        );

        // WHEN
        ResponseEntity<ResponseDTO> response = controller.crear(body, authentication);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        verify(nominaService, never()).crear(any(), any(), any(), any());
    }

    @Test
    @DisplayName("3.3.1b crear() succeeds when user has NOMINA_CREAR permission")
    void crear_withPermission_returns200() {
        // GIVEN: user has NOMINA_CREAR
        when(permisoService.tienePermisoPorCodigo("user@test.com", "NOMINA_CREAR"))
                .thenReturn(true);

        NominaDTO dto = NominaDTO.builder()
                .id(1L)
                .numero("NOM-2026-0001")
                .estado(EstadoNomina.BORRADOR)
                .tipo(TipoNomina.MENSUAL)
                .totalBruto(BigDecimal.ZERO)
                .totalNeto(BigDecimal.ZERO)
                .detalles(List.of())
                .build();
        when(nominaService.crear(any(), any(), any(), eq(TipoNomina.MENSUAL))).thenReturn(dto);

        Map<String, String> body = Map.of(
                "periodoInicio", "2026-07-01",
                "periodoFin", "2026-07-31",
                "fechaPago", "2026-08-05",
                "tipo", "MENSUAL"
        );

        // WHEN
        ResponseEntity<ResponseDTO> response = controller.crear(body, authentication);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
    }

    // =========================================================================
    // 3.3.2 — calcular without NOMINA_CALCULAR returns 403
    // =========================================================================

    @Test
    @DisplayName("3.3.2 calcular() returns 403 when user lacks NOMINA_CALCULAR permission")
    void calcular_withoutPermission_returns403() {
        // GIVEN: user does not have NOMINA_CALCULAR
        when(permisoService.tienePermisoPorCodigo("user@test.com", "NOMINA_CALCULAR"))
                .thenReturn(false);

        // WHEN
        ResponseEntity<ResponseDTO> response = controller.calcular(1L, authentication);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().isSuccess()).isFalse();
        verify(nominaService, never()).calcular(anyLong());
    }

    @Test
    @DisplayName("3.3.2b calcular() succeeds when user has NOMINA_CALCULAR permission")
    void calcular_withPermission_returns200() {
        // GIVEN
        when(permisoService.tienePermisoPorCodigo("user@test.com", "NOMINA_CALCULAR"))
                .thenReturn(true);

        NominaDTO dto = NominaDTO.builder()
                .id(1L)
                .estado(EstadoNomina.CALCULADA)
                .detalles(List.of())
                .totalBruto(BigDecimal.ZERO)
                .totalNeto(BigDecimal.ZERO)
                .build();
        when(nominaService.calcular(1L)).thenReturn(dto);

        // WHEN
        ResponseEntity<ResponseDTO> response = controller.calcular(1L, authentication);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
    }
}
