package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaConflictoException;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaPedidoInvalidoException;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaSustitutaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** T-039 (criterio 2): POST /api/facturas/{id}/sustituta — códigos HTTP, cuerpo y permiso. */
@DisplayName("T-039 — endpoint de la factura sustituta")
class FacturaSustitutaRestControllerTest {

    private FacturaSustitutaService servicio;
    private MockMvc mvc;
    private final UsernamePasswordAuthenticationToken usuario =
            new UsernamePasswordAuthenticationToken("vendedor", "x");

    @BeforeEach
    void setUp() {
        servicio = mock(FacturaSustitutaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new FacturaSustitutaRestController(servicio)).build();
    }

    private org.springframework.test.web.servlet.ResultActions llamar(String cuerpo) throws Exception {
        var req = post("/api/facturas/5/sustituta").principal(usuario);
        if (cuerpo != null) {
            req = req.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return mvc.perform(req);
    }

    @Test
    @DisplayName("201 con el id y el número de la factura nueva; el cuerpo es opcional")
    void creada() throws Exception {
        Factura nueva = new Factura();
        nueva.setIdFactura(6);
        nueva.setNumeroFactura("FA01-00006");
        when(servicio.crear(eq(5), any())).thenReturn(nueva);

        llamar(null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.idFactura").value(6))
                .andExpect(jsonPath("$.data.numeroFactura").value("FA01-00006"));
        llamar("{\"razon\":\"Corrige el precio\"}").andExpect(status().isCreated());
        org.mockito.Mockito.verify(servicio).crear(5, "Corrige el precio");
    }

    @Test
    @DisplayName("409 con el mensaje fijo cuando no procede o ya hay sustituta")
    void conflicto() throws Exception {
        when(servicio.crear(eq(5), any())).thenThrow(
                new NotaFacturaConflictoException("La factura ya tiene una sustituta: FA01-00006"));

        llamar(null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("La factura ya tiene una sustituta: FA01-00006"));
    }

    @Test
    @DisplayName("400 con razón inválida y con JSON mal formado, sin eco del texto técnico")
    void razonInvalida() throws Exception {
        when(servicio.crear(eq(5), any())).thenThrow(
                new NotaFacturaPedidoInvalidoException("La razón debe tener entre 3 y 180 caracteres"));

        llamar("{\"razon\":\"ab\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La razón debe tener entre 3 y 180 caracteres"));
        llamar("{no es json").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(FacturaSustitutaRestController.MENSAJE_FORMATO));
    }

    @Test
    @DisplayName("404 si la factura no existe; 500 genérico (sin e.getMessage()) si algo inesperado falla")
    void noExisteYErrorInesperado() throws Exception {
        when(servicio.crear(eq(5), any())).thenThrow(new NoSuchElementException("detalle interno"));
        llamar(null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(FacturaSustitutaRestController.MENSAJE_NO_EXISTE));

        when(servicio.crear(eq(5), any())).thenThrow(new IllegalStateException("secreto interno"));
        llamar(null).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(FacturaSustitutaRestController.MENSAJE_ERROR));
    }

    @Test
    @DisplayName("exige el permiso FACTURA_CREAR")
    void permiso() throws Exception {
        var metodo = FacturaSustitutaRestController.class.getMethod("emitir", Integer.class,
                api.astro.whats_orders_manager.modules.facturacion.dto.EmitirSustitutaRequest.class,
                org.springframework.security.core.Authentication.class);
        assertThat(metodo.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_CREAR')");
    }
}
