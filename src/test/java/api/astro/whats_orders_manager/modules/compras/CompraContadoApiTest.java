package api.astro.whats_orders_manager.modules.compras;

import api.astro.whats_orders_manager.modules.compras.controller.CompraContadoRestController;
import api.astro.whats_orders_manager.modules.compras.dto.CompraContadoResultadoDTO;
import api.astro.whats_orders_manager.modules.compras.service.AdjuntoComprobanteService;
import api.astro.whats_orders_manager.modules.compras.service.CompraContadoService;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaPorPagarRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.ComprobanteProveedorDuplicadoException;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenNoDefinidoException;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.security.ApiSecurityErrorHandler;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-058 criterio 11 a nivel del controlador: permisos (crear CxP Y registrar pago), 409 del duplicado y de la fecha sin tramo,
 * validación del adjunto con mensaje fijo y descarga solo con permiso de CxP.
 */
@WebMvcTest(value = CompraContadoRestController.class, properties = {
        "app.uploads.directorio=target/test-data/uploads-compras-api",
        "app.compras.adjunto.tamano-maximo=1KB"})
@Import({CompraContadoApiTest.SeguridadConfig.class, ApiSecurityErrorHandler.class, AdjuntoComprobanteService.class})
@DisplayName("T-058 — API de compra de contado y descarga del adjunto")
class CompraContadoApiTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class SeguridadConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http, ApiSecurityErrorHandler api) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(e -> e.accessDeniedHandler(api).authenticationEntryPoint(api));
            return http.build();
        }
    }

    private static final String DATOS = """
            {"proveedorId":1,"numeroComprobanteProveedor":"F-1","fechaComprobante":"2026-06-15","metodoPago":"EFECTIVO",
             "lineas":[{"cuentaContableId":1,"codigoTarifa":"08","base":1000,"iva":0,"tratamiento":"NO_ACREDITABLE"}]}""";
    private static final byte[] PDF = "%PDF-1.4 x".getBytes(StandardCharsets.ISO_8859_1);

    @MockitoBean private CompraContadoService compraContadoService;
    @MockitoBean private UsuarioService usuarioService;
    @MockitoBean private CuentaPorPagarRepository cuentaRepository;
    @MockitoBean(name = "permisoService") private PermisoService permisoService;
    @MockitoBean private UsuarioActividadService usuarioActividadService;
    @MockitoBean private api.astro.whats_orders_manager.modules.configuracion.repository.ProvinciaCostaRicaRepository provinciaRepository;

    @Autowired private MockMvc mockMvc;
    @Autowired private AdjuntoComprobanteService adjuntoService;

    @BeforeEach
    void preparar() {
        when(permisoService.tienePermisoPorCodigo(anyString(), anyString())).thenReturn(true);
        when(usuarioService.findByEmail(anyString())).thenReturn(Optional.of(new Usuario()));
        when(compraContadoService.registrar(any(), any(), any())).thenReturn(
                new CompraContadoResultadoDTO(5L, "CPP-2026-00005", 9L, "PAG-2026-00009", List.of(), false));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder envio(MockMultipartFile... extra) {
        var b = multipart("/api/compras/contado")
                .file(new MockMultipartFile("datos", "", MediaType.APPLICATION_JSON_VALUE, DATOS.getBytes(StandardCharsets.UTF_8)));
        for (MockMultipartFile f : extra) b.file(f);
        return b.with(csrf()).with(user("cajero"));
    }

    @Test
    @DisplayName("sin CUENTA_PAGAR_CREAR o sin CUENTA_PAGAR_REGISTRAR_PAGO → 403 y no se registra nada")
    void permisos() throws Exception {
        for (String faltante : new String[]{"CUENTA_PAGAR_CREAR", "CUENTA_PAGAR_REGISTRAR_PAGO"}) {
            when(permisoService.tienePermisoPorCodigo(anyString(), eq(faltante))).thenReturn(false);
            mockMvc.perform(envio()).andExpect(status().isForbidden());
            when(permisoService.tienePermisoPorCodigo(anyString(), eq(faltante))).thenReturn(true);
        }
        verifyNoInteractions(compraContadoService);
    }

    @Test
    @DisplayName("con ambos permisos: 200, el servicio recibe el DTO y la respuesta trae el número de la CxP")
    void ok() throws Exception {
        mockMvc.perform(envio())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cuentaPorPagarId").value(5))
                .andExpect(jsonPath("$.data.numeroCuentaPorPagar").value("CPP-2026-00005"))
                .andExpect(jsonPath("$.data.pagoId").value(9))
                .andExpect(jsonPath("$.message").value("Compra de contado registrada: CPP-2026-00005"));
        var dto = ArgumentCaptor.forClass(api.astro.whats_orders_manager.modules.compras.dto.CompraContadoDTO.class);
        verify(compraContadoService).registrar(dto.capture(), eq(null), any());
        assertThat(dto.getValue().getNumeroComprobanteProveedor()).isEqualTo("F-1");
    }

    @Test
    @DisplayName("comprobante duplicado → 409 con el mensaje existente; fecha sin tramo → 409; otro error de negocio → 400")
    void errores() throws Exception {
        org.mockito.Mockito.doThrow(new ComprobanteProveedorDuplicadoException("F-1"))
                .when(compraContadoService).registrar(any(), any(), any());
        mockMvc.perform(envio()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe una cuenta por pagar de este proveedor con el comprobante F-1"));
        org.mockito.Mockito.doThrow(new RegimenNoDefinidoException(LocalDate.of(1999, 1, 1)))
                .when(compraContadoService).registrar(any(), any(), any());
        mockMvc.perform(envio()).andExpect(status().isConflict());
        org.mockito.Mockito.doThrow(new IllegalArgumentException("mala"))
                .when(compraContadoService).registrar(any(), any(), any());
        mockMvc.perform(envio()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("mala"));
    }

    @Test
    @DisplayName("adjunto: extensión no permitida, contenido que no corresponde y tamaño excedido → 400 con mensaje fijo, sin registrar")
    void adjuntoInvalido() throws Exception {
        mockMvc.perform(envio(new MockMultipartFile("adjunto", "virus.exe", "application/pdf", PDF)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(AdjuntoComprobanteService.MSG_EXTENSION));
        mockMvc.perform(envio(new MockMultipartFile("adjunto", "a.pdf", "application/pdf", "hola".getBytes())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(AdjuntoComprobanteService.MSG_CONTENIDO));
        byte[] grande = new byte[2000];
        System.arraycopy(PDF, 0, grande, 0, PDF.length);
        mockMvc.perform(envio(new MockMultipartFile("adjunto", "a.pdf", "application/pdf", grande)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El adjunto excede el tamaño máximo permitido de 1 KB"));
        verify(compraContadoService, never()).registrar(any(), any(), any());
    }

    @Test
    @DisplayName("adjunto válido: el servicio lo recibe validado")
    void adjuntoValido() throws Exception {
        mockMvc.perform(envio(new MockMultipartFile("adjunto", "a.pdf", "application/pdf", PDF)))
                .andExpect(status().isOk());
        var adj = ArgumentCaptor.forClass(api.astro.whats_orders_manager.modules.compras.service.AdjuntoComprobante.class);
        verify(compraContadoService).registrar(any(), adj.capture(), any());
        assertThat(adj.getValue().extension()).isEqualTo("pdf");
        assertThat(adj.getValue().nombreOriginal()).isEqualTo("a.pdf");
    }

    @Test
    @DisplayName("descarga: 200 con CUENTA_PAGAR_VER (attachment, nosniff, tipo de la extensión); 403 sin el permiso; 404 sin adjunto")
    void descarga() throws Exception {
        String ruta = adjuntoService.guardar(11L, adjuntoService.validar(
                new MockMultipartFile("adjunto", "original.pdf", "text/html", PDF)));
        assertThat(ruta).matches("cxp/11/[0-9a-f-]{36}\\.pdf").doesNotContain("original");
        CuentaPorPagar c = new CuentaPorPagar();
        c.setAdjuntoRuta(ruta);
        c.setAdjuntoNombreOriginal("original.pdf");
        c.setAdjuntoTipo("application/pdf");
        when(cuentaRepository.findById(11L)).thenReturn(Optional.of(c));
        when(cuentaRepository.findById(12L)).thenReturn(Optional.of(new CuentaPorPagar()));

        mockMvc.perform(get("/api/cuentas-pagar/11/adjunto").with(user("lector")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("original.pdf")))
                .andExpect(content().bytes(PDF));
        mockMvc.perform(get("/api/cuentas-pagar/12/adjunto").with(user("lector"))).andExpect(status().isNotFound());

        when(permisoService.tienePermisoPorCodigo(anyString(), eq("CUENTA_PAGAR_VER"))).thenReturn(false);
        mockMvc.perform(get("/api/cuentas-pagar/11/adjunto").with(user("lector"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("descarga con ruta guardada que sale del directorio de uploads: 404, nunca se sirve")
    void descargaTraversal() throws Exception {
        CuentaPorPagar c = new CuentaPorPagar();
        c.setAdjuntoRuta("../../../../etc/passwd");
        c.setAdjuntoNombreOriginal("passwd");
        c.setAdjuntoTipo("application/pdf");
        when(cuentaRepository.findById(13L)).thenReturn(Optional.of(c));
        mockMvc.perform(get("/api/cuentas-pagar/13/adjunto").with(user("lector"))).andExpect(status().isNotFound());
    }
}
