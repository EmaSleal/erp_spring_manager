package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.RespuestaHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MensajeHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ComprobanteElectronicoMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-033 (criterios 19 y 20): el JSON de GET /recepcion/{clave} no trae «mensaje»; el motivo del rechazo solo viene en
 * el MensajeHacienda de respuesta-xml. Antes, mensaje_respuesta quedaba vacío y la pantalla (que esperaba ese XML en
 * base64 dentro de mensajeRespuesta) no mostraba nada. Fixture: el MensajeHacienda real del comprobante 28 de SANDBOX.
 */
@DisplayName("T-033 — motivo del rechazo de Hacienda en el comprobante")
class MotivoRechazoHaciendaTest {

    static final String MENSAJE_HACIENDA_28 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><MensajeHacienda "
            + "xmlns=\"https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/mensajeHacienda\">\n"
            + "    <Clave>50613112600011820087800100001010000000028172833386</Clave>\n"
            + "    <NombreEmisor>EMISOR DE PRUEBA</NombreEmisor>\n"
            + "    <Mensaje>3</Mensaje>\n"
            + "    <EstadoMensaje>Rechazado</EstadoMensaje>\n"
            + "    <DetalleMensaje>Este comprobante fue recibido en el ambiente de pruebas, por lo cual no tiene validez "
            + "para fines tributarios.\n\nEl comprobante electrónico tiene los siguientes errores: &#13;\n[&#13;\n"
            + "codigo, mensaje, fila, columna&#13;\n"
            + "-53, \"\"La fecha de emisión del comprobante electrónico no puede ser superior al día de hoy.\"\", 0, 0&#13;\n"
            + "-99, \"\"La numeración consecutiva 00100001010000000028 del comprobante que se está utilizando para este "
            + "archivo XML ya existe en nuestras bases de datos\"\", 0, 0&#13;\n"
            + "-20, \"\"El nodo denominado 'Número de teléfono emisor' no posee la estructura establecida para el mismo\"\", "
            + "0, 0&#13;\n&#13;\n]</DetalleMensaje>\n"
            + "    <TotalFactura>5000.00000</TotalFactura>\n"
            + "</MensajeHacienda>";

    @Test
    @DisplayName("al consultar un RECHAZADO: codigo_respuesta = Mensaje (3) y mensaje_respuesta = DetalleMensaje")
    void rechazado_persisteCodigoYDetalle() {
        ComprobanteElectronicoRepository repository = mock(ComprobanteElectronicoRepository.class);
        ConfiguracionHaciendaService configuracion = mock(ConfiguracionHaciendaService.class);
        HaciendaApiService hacienda = mock(HaciendaApiService.class);
        ConfiguracionEmpresa empresa = ConfiguracionEmpresa.builder().idConfiguracion(1).build();
        ComprobanteElectronico comprobante = ComprobanteElectronico.builder().id(3L).empresa(empresa)
                .claveNumerica("50613112600011820087800100001010000000028172833386")
                .estado(EstadoComprobante.ENVIADO).build();
        when(repository.findById(3L)).thenReturn(Optional.of(comprobante));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(configuracion.obtenerConfiguracionActiva())
                .thenReturn(ConfiguracionHacienda.builder().id(9L).empresa(empresa).build());
        when(hacienda.consultarComprobante(anyString(), anyLong())).thenReturn(RespuestaHaciendaDTO.builder()
                .indicadorEstado("rechazado").codigoMensaje(MensajeHacienda.RECHAZADO).mensaje("")
                .xmlRespuesta(Base64.getEncoder().encodeToString(MENSAJE_HACIENDA_28.getBytes(StandardCharsets.UTF_8)))
                .codigoHttp(200).build());
        ComprobanteElectronicoServiceImpl service = new ComprobanteElectronicoServiceImpl(repository, null, null,
                configuracion, null, null, null, hacienda, null, mock(ComprobanteElectronicoMapper.class), null, null,
                Clock.systemUTC(), null, null);

        service.consultarYActualizarEstado(3L);

        assertThat(comprobante.getEstado()).isEqualTo(EstadoComprobante.RECHAZADO);
        assertThat(comprobante.getXmlRespuesta()).isEqualTo(MENSAJE_HACIENDA_28);
        assertThat(comprobante.getCodigoRespuesta()).isEqualTo("3");
        assertThat(comprobante.getMensajeRespuesta())
                .startsWith("Este comprobante fue recibido en el ambiente de pruebas")
                .contains("-53, ").contains("-99, ").contains("-20, ")
                .doesNotContain("\r");
    }

    @Test
    @DisplayName("MensajeHacienda con acentos en ISO-8859-1 (como llegó en SANDBOX) se lee sin caracteres de reemplazo")
    void mensajeEnIso88591_seLeeBien() {
        byte[] latin1 = MENSAJE_HACIENDA_28.getBytes(StandardCharsets.ISO_8859_1);
        byte[] utf8 = MENSAJE_HACIENDA_28.getBytes(StandardCharsets.UTF_8);

        assertThat(ComprobanteElectronicoServiceImpl.textoDelMensajeHacienda(latin1))
                .isEqualTo(MENSAJE_HACIENDA_28).contains("emisión").doesNotContain("\uFFFD");
        assertThat(ComprobanteElectronicoServiceImpl.textoDelMensajeHacienda(utf8)).isEqualTo(MENSAJE_HACIENDA_28);
    }

    @Test
    @DisplayName("sin respuesta-xml, código y mensaje quedan como los informa el JSON")
    void sinXml_noCambia() {
        ComprobanteElectronico comprobante = ComprobanteElectronico.builder().codigoRespuesta("2")
                .mensajeRespuesta("ind-estado procesando").build();

        ComprobanteElectronicoServiceImpl.adoptarMotivoDelMensajeHacienda(comprobante);

        assertThat(comprobante.getCodigoRespuesta()).isEqualTo("2");
        assertThat(comprobante.getMensajeRespuesta()).isEqualTo("ind-estado procesando");
    }
}
