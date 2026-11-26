package api.astro.whats_orders_manager.modules.compras;

import api.astro.whats_orders_manager.modules.compras.service.AdjuntoComprobante;
import api.astro.whats_orders_manager.modules.compras.service.AdjuntoComprobanteService;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaPorPagarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** T-058 criterio 11: validación del adjunto del comprobante y nombre guardado no adivinable. */
@DisplayName("T-058 — adjunto del comprobante: validación y almacenamiento")
class AdjuntoComprobanteServiceTest {

    private static final byte[] PDF = "%PDF-1.4 contenido".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    private static final byte[] XML = "﻿  <?xml version=\"1.0\"?><Factura/>".getBytes(StandardCharsets.UTF_8);

    @TempDir Path uploads;
    CuentaPorPagarRepository repo = mock(CuentaPorPagarRepository.class);
    AdjuntoComprobanteService servicio;

    @BeforeEach
    void setUp() {
        servicio = new AdjuntoComprobanteService(repo, uploads.toString(), DataSize.ofKilobytes(1));
    }

    private MockMultipartFile archivo(String nombre, byte[] contenido) {
        return new MockMultipartFile("adjunto", nombre, "application/octet-stream", contenido);
    }

    @Test
    @DisplayName("sin archivo (null o vacío) no hay adjunto")
    void sinArchivo() {
        assertThat(servicio.validar(null)).isNull();
        assertThat(servicio.validar(archivo("a.pdf", new byte[0]))).isNull();
    }

    @Test
    @DisplayName("acepta XML (con BOM), PDF, JPG, JPEG y PNG y deriva el tipo de la extensión")
    void aceptaPermitidos() {
        assertThat(servicio.validar(archivo("f.PDF", PDF)).tipo()).isEqualTo("application/pdf");
        assertThat(servicio.validar(archivo("f.png", PNG)).tipo()).isEqualTo("image/png");
        assertThat(servicio.validar(archivo("f.jpg", JPG)).tipo()).isEqualTo("image/jpeg");
        assertThat(servicio.validar(archivo("f.jpeg", JPG)).extension()).isEqualTo("jpeg");
        assertThat(servicio.validar(archivo("f.xml", XML)).tipo()).isEqualTo("application/xml");
    }

    @Test
    @DisplayName("extensión no permitida, sin extensión o doble extensión engañosa: mensaje fijo")
    void extensionNoPermitida() {
        for (String n : new String[]{"virus.exe", "sin_extension", "a.pdf.exe", "a.html", "a.svg", ""}) {
            assertThatThrownBy(() -> servicio.validar(archivo(n, PDF)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(AdjuntoComprobanteService.MSG_EXTENSION);
        }
    }

    @Test
    @DisplayName("el contenido debe corresponder a la extensión (bytes mágicos): un .pdf con texto se rechaza")
    void contenidoNoCorresponde() {
        assertThatThrownBy(() -> servicio.validar(archivo("a.pdf", "hola".getBytes())))
                .hasMessage(AdjuntoComprobanteService.MSG_CONTENIDO);
        assertThatThrownBy(() -> servicio.validar(archivo("a.png", JPG))).hasMessage(AdjuntoComprobanteService.MSG_CONTENIDO);
        assertThatThrownBy(() -> servicio.validar(archivo("a.xml", "no es xml".getBytes())))
                .hasMessage(AdjuntoComprobanteService.MSG_CONTENIDO);
    }

    @Test
    @DisplayName("tamaño máximo configurable: por encima del límite, mensaje fijo con el límite")
    void tamanoMaximo() {
        byte[] grande = new byte[2048];
        System.arraycopy(PDF, 0, grande, 0, PDF.length);
        assertThatThrownBy(() -> servicio.validar(archivo("a.pdf", grande)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El adjunto excede el tamaño máximo permitido de 1 KB");
    }

    @Test
    @DisplayName("guardar: ruta cxp/<id>/<UUID>.<ext> distinta del nombre original, distinta en cada llamada y dentro de uploads")
    void guardarNombreNoAdivinable() throws Exception {
        AdjuntoComprobante a = servicio.validar(archivo("factura-secreta-123.pdf", PDF));
        String ruta1 = servicio.guardar(7L, a);
        String ruta2 = servicio.guardar(7L, a);
        assertThat(ruta1).matches("cxp/7/[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\.pdf");
        assertThat(ruta1).doesNotContain("factura-secreta").isNotEqualTo(ruta2);
        assertThat(Files.readAllBytes(uploads.resolve(ruta1))).isEqualTo(PDF);
    }

    @Test
    @DisplayName("cargar: sin adjunto vacío; con adjunto devuelve el archivo; una ruta con '..' o absoluta jamás se sirve")
    void cargarYTraversal() throws Exception {
        CuentaPorPagar sin = new CuentaPorPagar();
        when(repo.findById(1L)).thenReturn(Optional.of(sin));
        assertThat(servicio.cargar(1L)).isEmpty();
        assertThat(servicio.cargar(99L)).isEmpty();

        String ruta = servicio.guardar(2L, servicio.validar(archivo("a.pdf", PDF)));
        CuentaPorPagar con = new CuentaPorPagar();
        con.setAdjuntoRuta(ruta);
        con.setAdjuntoNombreOriginal("a.pdf");
        con.setAdjuntoTipo("application/pdf");
        when(repo.findById(2L)).thenReturn(Optional.of(con));
        var d = servicio.cargar(2L).orElseThrow();
        assertThat(d.archivo()).isEqualTo(uploads.resolve(ruta).normalize());
        assertThat(d.nombreOriginal()).isEqualTo("a.pdf");

        Files.writeString(uploads.getParent().resolve("secreto.txt"), "x");
        for (String mala : new String[]{"../secreto.txt", "cxp/../../secreto.txt", "/etc/passwd"}) {
            CuentaPorPagar c = new CuentaPorPagar();
            c.setAdjuntoRuta(mala);
            when(repo.findById(3L)).thenReturn(Optional.of(c));
            assertThat(servicio.cargar(3L)).as(mala).isEmpty();
        }
    }
}
