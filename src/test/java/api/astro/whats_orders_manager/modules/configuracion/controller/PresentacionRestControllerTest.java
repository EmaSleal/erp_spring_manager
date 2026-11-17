package api.astro.whats_orders_manager.modules.configuracion.controller;

import api.astro.whats_orders_manager.modules.configuracion.model.Presentacion;
import api.astro.whats_orders_manager.modules.configuracion.repository.PresentacionRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.impl.PresentacionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** T-034 (D5): la presentación solo acepta códigos de unidad del XSD; empacado por defecto true. */
@DisplayName("T-034 — Presentaciones: empacado y código de unidad FE")
class PresentacionRestControllerTest {

    private PresentacionRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository = mock(PresentacionRepository.class);
        when(repository.save(any(Presentacion.class))).thenAnswer(inv -> inv.getArgument(0));
        PresentacionServiceImpl service = new PresentacionServiceImpl();
        ReflectionTestUtils.setField(service, "presentacionRepository", repository);
        mvc = MockMvcBuilders.standaloneSetup(new PresentacionRestController(service)).build();
    }

    private void body(String json, boolean post, int esperado) throws Exception {
        var req = post ? post("/api/configuracion/presentaciones") : put("/api/configuracion/presentaciones/7");
        mvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().is(esperado));
    }

    @Test
    @DisplayName("GET /unidades-fe devuelve el enum del XSD en su orden")
    void unidadesFe() throws Exception {
        mvc.perform(get("/api/configuracion/presentaciones/unidades-fe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0]").value("1"))
                .andExpect(jsonPath("$.data", hasItems("Unid", "L", "m²", "Kg", "Otros")));
    }

    @Test
    @DisplayName("POST empacado=false sin código → 400; con código del enum → 201")
    void post_noEmpacado() throws Exception {
        body("{\"nombre\":\"Litro\",\"empacado\":false}", true, 400);
        body("{\"nombre\":\"Litro\",\"empacado\":false,\"codigoUnidadFE\":\"l\"}", true, 400);
        body("{\"nombre\":\"Litro\",\"empacado\":false,\"codigoUnidadFE\":\"L\"}", true, 201);
        verify(repository, times(1)).save(any());
    }

    @Test
    @DisplayName("POST empacado=true: código opcional; si viene debe ser del enum; empacado ausente o null = true")
    void post_empacado() throws Exception {
        body("{\"nombre\":\"200 g\",\"empacado\":true}", true, 201);
        body("{\"nombre\":\"200 g\"}", true, 201);
        body("{\"nombre\":\"200 g\",\"empacado\":null}", true, 201);
        body("{\"nombre\":\"200 g\",\"empacado\":true,\"codigoUnidadFE\":\"Galon\"}", true, 400);
        body("{\"nombre\":\"200 g\",\"empacado\":true,\"codigoUnidadFE\":\"Kg\"}", true, 201);
    }

    @Test
    @DisplayName("el 400 nombra el valor inválido con un mensaje fijo")
    void mensaje400() throws Exception {
        mvc.perform(post("/api/configuracion/presentaciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"x\",\"empacado\":false,\"codigoUnidadFE\":\"Galon\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Galon")));
    }

    @Test
    @DisplayName("PUT valida igual que POST")
    void put_valida() throws Exception {
        when(repository.findById(7)).thenReturn(Optional.of(new Presentacion()));
        body("{\"nombre\":\"Litro\",\"empacado\":false,\"codigoUnidadFE\":\"l\"}", false, 400);
        body("{\"nombre\":\"Litro\",\"empacado\":false,\"codigoUnidadFE\":\"L\"}", false, 200);
    }

    @Test
    @DisplayName("P-070: una IllegalArgumentException cualquiera responde 500 fijo, no 400 con su texto")
    void illegalArgumentGenerica_500Fijo() throws Exception {
        when(repository.save(any())).thenThrow(new IllegalArgumentException("detalle-interno-spring"));
        mvc.perform(post("/api/configuracion/presentaciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"200 g\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", not(containsString("detalle-interno"))));
        when(repository.findById(7)).thenReturn(Optional.of(new Presentacion()));
        mvc.perform(put("/api/configuracion/presentaciones/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"200 g\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", not(containsString("detalle-interno"))));
    }

    @Test
    @DisplayName("un error inesperado responde 500 con mensaje fijo, sin el detalle de la excepción")
    void error500_sinDetalle() throws Exception {
        when(repository.save(any())).thenThrow(new RuntimeException("jdbc:mysql://secreto"));
        mvc.perform(post("/api/configuracion/presentaciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"200 g\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", not(containsString("secreto"))));
        when(repository.findAll()).thenThrow(new RuntimeException("jdbc:mysql://secreto"));
        mvc.perform(get("/api/configuracion/presentaciones"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", not(containsString("secreto"))));
    }
}
