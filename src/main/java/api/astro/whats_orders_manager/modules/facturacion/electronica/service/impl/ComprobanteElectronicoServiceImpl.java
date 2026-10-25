package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmpresaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.RespuestaHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ResultadoConsultaManualDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.ResultadoEnvioHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ConsultaManualDemasiadoProntoException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ConsultaManualNoPermitidaException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ComprobanteElectronicoMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.*;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.ClaveNumericaGenerator;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.LineaFacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaPdfService;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.shared.service.EmailService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Implementación del servicio de comprobantes electrónicos.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComprobanteElectronicoServiceImpl implements ComprobanteElectronicoService {
    
    private final ComprobanteElectronicoRepository repository;
    private final FacturaRepository facturaRepository;
    private final LineaFacturaRepository lineaFacturaRepository;
    private final ConfiguracionHaciendaService configuracionService;
    private final ConfiguracionEmpresaService configuracionEmpresaService;
    private final XmlGeneratorService xmlGenerator;
    private final FirmaDigitalService firmaDigital;
    private final HaciendaApiService haciendaApi;
    private final ClaveNumericaGenerator claveGenerator;
    private final ComprobanteElectronicoMapper mapper;
    private final FacturaPdfService facturaPdfService;
    private final EmailService emailService;
    private final Clock clock;
    private final LimiteConsultaManualHacienda limiteConsultaManual;

    /**
     * Minutos desde el último envío a partir de los cuales un 404 de la consulta de un ENVIO_INCIERTO significa que
     * Hacienda no lo tiene (NO_TRANSMITIDO). Antes, el 404 no cambia el estado (ADR-030 §2, ADR-031 §4).
     */
    public static final long MINUTOS_ANTES_DE_VERIFICAR = 15;

    /** Estados en los que se puede "Consultar estado en Hacienda" (ADR-031 §4). */
    public static final Set<EstadoComprobante> CONSULTABLES_A_MANO = EnumSet.of(EstadoComprobante.ENVIADO,
        EstadoComprobante.PROCESANDO, EstadoComprobante.ENVIO_INCIERTO, EstadoComprobante.ERROR_HACIENDA);

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    
    @Value("${facturacion.comprobantes.directorio-base:./comprobantes}")
    private String directorioBase;

    /**
     * Consultas seguidas con ind-estado "error" tras las que el sondeo deja de consultar el comprobante y lo señala
     * para revisarlo en ATV.
     */
    @Value("${facturacion.hacienda.max-consultas-error:" + MAX_CONSULTAS_ERROR_HACIENDA_DEFECTO + "}")
    private int maxConsultasErrorHacienda = MAX_CONSULTAS_ERROR_HACIENDA_DEFECTO;

    /** Valor por defecto de facturacion.hacienda.max-consultas-error. */
    public static final int MAX_CONSULTAS_ERROR_HACIENDA_DEFECTO = 5;
    
    @Override
    @Transactional
    public ComprobanteElectronicoDTO procesarFactura(Long facturaId) {
        log.info("🚀 Iniciando procesamiento completo de factura ID: {}", facturaId);
        
        // VALIDAR que el cliente requiera facturación electrónica
        Factura factura = facturaRepository.findByIdFactura(facturaId.intValue())
            .orElseThrow(() -> new EntityNotFoundException("Factura no encontrada con ID: " + facturaId));
        
        if (factura.getCliente() == null) {
            throw new IllegalArgumentException("La factura no tiene cliente asignado");
        }
        
        if (Boolean.FALSE.equals(factura.getCliente().getRequiereFacturaElectronica())) {
            log.warn("⚠️ Cliente {} no requiere facturación electrónica", factura.getCliente().getNombre());
            throw new IllegalStateException("El cliente '" + factura.getCliente().getNombre() + 
                "' no requiere facturación electrónica. Verifique la configuración del cliente.");
        }
        
        // 1. Generar comprobante
        ComprobanteElectronicoDTO comprobante = generarDesdeFactura(facturaId);
        log.info("✅ Comprobante generado con clave: {}", comprobante.getClaveNumerica());
        
        // 2. Firmar comprobante
        try {
            comprobante = firmar(comprobante.getId());
            log.info("✅ Comprobante firmado digitalmente");
        } catch (Exception e) {
            log.error("❌ Error firmando comprobante: {}", e.getMessage());
            throw new RuntimeException("Error en firma digital: " + e.getMessage(), e);
        }
        
        // 3. Enviar a Hacienda de forma asíncrona
        procesarFacturaAsync(comprobante.getId());
        
        log.info("✅ Factura procesada. Envío a Hacienda en progreso (asíncrono)");
        return comprobante;
    }
    
    @Override
    @Async
    @Transactional
    public void procesarFacturaAsync(Long comprobanteId) {
        log.info("📤 Iniciando envío asíncrono de comprobante ID: {}", comprobanteId);
        
        try {
            // Enviar a Hacienda
            ComprobanteElectronicoDTO resultado = enviarAHacienda(comprobanteId);
            log.info("✅ Comprobante enviado a Hacienda: {}", resultado.getEstado());
            if (resultado.getEstado() != EstadoComprobante.ENVIADO) {
                // NO_TRANSMITIDO, ENVIO_INCIERTO o ERROR: no hay nada que sondear
                return;
            }

            // Hacienda es asíncrono: esperar y reintentar hasta obtener estado final
            int maxReintentos = 5;
            long esperaMs = 30_000;
            for (int intento = 1; intento <= maxReintentos; intento++) {
                Thread.sleep(esperaMs);
                var actualizado = consultarYActualizarEstado(comprobanteId);
                EstadoComprobante estadoActual = actualizado.getEstado();
                if (estadoActual == EstadoComprobante.ACEPTADO
                        || estadoActual == EstadoComprobante.RECHAZADO
                        || estadoActual == EstadoComprobante.ENVIO_INCIERTO) {
                    log.info("✅ Estado final obtenido: {} (intento {}/{})", estadoActual, intento, maxReintentos);
                    break;
                }
                log.info("⏳ Hacienda aún procesando (intento {}/{}), reintentando en {}s...",
                        intento, maxReintentos, esperaMs / 1000);
            }

        } catch (InterruptedException e) {
            log.warn("⚠️ Proceso asíncrono interrumpido: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // El envío ya clasificó su resultado; un fallo del sondeo posterior no cambia el estado (ADR-030:
            // ERROR es solo una falla local antes de transmitir). El job horario vuelve a consultar.
            log.error("❌ Error en el sondeo asíncrono del comprobante {}: {}", comprobanteId, e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public ComprobanteElectronicoDTO generarDesdeFactura(Long facturaId) {
        log.info("Generando comprobante electrónico desde factura ID: {}", facturaId);
        
        TipoComprobanteElectronico tipo = TipoComprobanteElectronico.FACTURA_ELECTRONICA;

        Factura factura = facturaRepository.findById(facturaId.intValue())
            .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada"));

        // ADR-030 §4: un solo comprobante vigente por factura. Solo un RECHAZADO cede su lugar (reemisión con
        // consecutivo y clave nuevos); el rechazado queda como historial, sin otros cambios.
        repository.findFirstByFactura_IdFacturaAndVigenteTrue(factura.getIdFactura()).ifPresent(vigente -> {
            if (vigente.getEstado() != EstadoComprobante.RECHAZADO) {
                throw new IllegalStateException("La factura ya tiene el comprobante vigente "
                    + vigente.getClaveNumerica() + " en estado " + vigente.getEstado()
                    + "; solo se emite uno nuevo si el vigente fue RECHAZADO");
            }
            vigente.setVigente(false);
            repository.saveAndFlush(vigente); // antes del INSERT del nuevo: la BD admite un solo vigente
            log.info("Comprobante rechazado {} pasa a historial", vigente.getClaveNumerica());
        });

        // ADR-021: la configuración activa (exactamente una) de la empresa única
        ConfiguracionEmpresa empresa = configuracionEmpresaService.obtenerConfiguracion()
            .orElseThrow(() -> new IllegalStateException(
                "No hay configuración de empresa; complétela en Configuración → Empresa."));
        ConfiguracionHacienda config = configuracionActivaDe(empresa);

        if (empresa.getRuc() == null || empresa.getRuc().isBlank()) {
            throw new IllegalStateException(
                "La empresa no tiene número de identificación (cédula jurídica) configurado. " +
                "Configúrelo en Configuración → Empresa antes de emitir comprobantes.");
        }

        // Generar consecutivo y clave numérica
        Long consecutivo = config.siguienteConsecutivo(tipo.getCodigo());
        LocalDateTime fechaEmision = LocalDateTime.now();

        String claveNumerica = claveGenerator.generar(
            empresa.getRuc(),
            consecutivo,
            tipo,
            fechaEmision
        );
        //log de factura
        log.info("Objeto de factura para generación de comprobante: {}", factura);
        // Crear comprobante
        ComprobanteElectronico comprobante = ComprobanteElectronico.builder()
            .factura(factura)
            .empresa(empresa)
            .tipoComprobante(tipo)
            .claveNumerica(claveNumerica)
            .consecutivo(String.format("%020d", consecutivo))
            .fechaEmision(fechaEmision)
            .estado(EstadoComprobante.GENERADO)
            .vigente(true)
            .intentosEnvio(0)
            .enviadoEmail(false)
            .build();
        
        // Generar XML
        String xml = generarXml(comprobante);
        comprobante.setXmlComprobante(xml);
        
        comprobante = repository.save(comprobante);
        log.info("Comprobante generado con clave: {}", claveNumerica);
        
        return mapper.toDTO(comprobante);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<ComprobanteElectronicoDTO> obtenerPorId(Long id) {
        return repository.findById(id).map(mapper::toDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<ComprobanteElectronicoDTO> obtenerPorClaveNumerica(String claveNumerica) {
        return repository.findByClaveNumerica(claveNumerica).map(mapper::toDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<ComprobanteElectronicoDTO> obtenerPorFactura(Long facturaId) {
        return repository.findByFacturaId(facturaId).map(mapper::toDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ComprobanteElectronicoDTO> listarPorEmpresa(Integer empresaId, int page, int size) {
        List<Object[]> results = repository.findAllByEmpresaIdAsDTO(empresaId, page, size);
        return convertirResultadosADTO(results);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ComprobanteElectronicoDTO> listarPorEstado(Integer empresaId, EstadoComprobante estado, int page, int size) {
        List<Object[]> results = repository.findByEmpresaIdAndEstadoAsDTO(empresaId, estado.name(), page, size);
        return convertirResultadosADTO(results);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ComprobanteElectronicoDTO> listarPorRangoFechas(Integer empresaId, LocalDateTime fechaInicio, LocalDateTime fechaFin, int page, int size) {
        List<Object[]> results = repository.findByEmpresaIdAndFechaEmisionBetweenAsDTO(empresaId, fechaInicio, fechaFin, page, size);
        return convertirResultadosADTO(results);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ComprobanteElectronico> obtenerPendientesEnvio() {
        List<Object[]> results = repository.findPendientesEnvio();
        // Convertir Object[] a entities usando IDs y luego cargar las entities completas
        return results.stream()
            .map(row -> repository.findById(((Number) row[0]).longValue()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    @Transactional
    public ComprobanteElectronicoDTO firmar(Long id) {
        log.info("Firmando comprobante ID: {}", id);

        ComprobanteElectronico comprobante = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado"));
        firmarInterno(comprobante);
        return mapper.toDTO(repository.save(comprobante));
    }

    /** Firma el XML del comprobante: FIRMADO, o ERROR (falla local) si no se pudo firmar. */
    private void firmarInterno(ComprobanteElectronico comprobante) {
        try {
            ConfiguracionHacienda config = configuracionActivaDe(comprobante.getEmpresa());
            String xmlFirmado = firmaDigital.firmarXml(
                comprobante.getXmlComprobante(),
                config.getRutaCertificado(),
                config.getPinCertificado()
            );

            comprobante.setXmlComprobante(xmlFirmado);
            comprobante.setEstado(EstadoComprobante.FIRMADO);

            // Guardar XML firmado en filesystem
            try {
                guardarXmlEnFilesystem(comprobante.getId(), xmlFirmado);
            } catch (Exception e) {
                log.warn("⚠️ No se pudo guardar XML en filesystem: {}", e.getMessage());
            }

            log.info("Comprobante firmado exitosamente");
        } catch (Exception e) {
            log.error("Error firmando comprobante: {}", e.getMessage());
            comprobante.setEstado(EstadoComprobante.ERROR);
            comprobante.setUltimoError("Error en firma digital: " + e.getMessage());
        }
    }

    /**
     * Envía el comprobante a Hacienda con su clave (ADR-030). Solo si Hacienda no lo tiene: GENERADO, FIRMADO,
     * ERROR o NO_TRANSMITIDO. Si el XML se descartó por una edición, o el comprobante quedó en GENERADO o ERROR,
     * se regenera (misma clave) y se firma antes de enviar. El estado final lo fija la clasificación en origen del
     * cliente de Hacienda: ENVIADO, NO_TRANSMITIDO o ENVIO_INCIERTO; ERROR solo por fallas locales previas.
     */
    @Override
    @Transactional
    public ComprobanteElectronicoDTO enviarAHacienda(Long id) {
        log.info("Enviando comprobante ID: {} a Hacienda", id);

        ComprobanteElectronico comprobante = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado"));

        if (!comprobante.puedeReenviar()) {
            throw new IllegalStateException("El comprobante en estado " + comprobante.getEstado()
                + " no se puede enviar: Hacienda ya lo tiene o pudo recibirlo");
        }

        if (!prepararXmlFirmado(comprobante)) {
            return mapper.toDTO(repository.save(comprobante));
        }

        ConfiguracionHacienda config;
        try {
            config = configuracionActivaDe(comprobante.getEmpresa());
        } catch (Exception e) {
            comprobante.setEstado(EstadoComprobante.ERROR);
            comprobante.setUltimoError("Configuración de Hacienda: " + e.getMessage());
            return mapper.toDTO(repository.save(comprobante));
        }

        // Token: si no se puede renovar, la recepción nunca se intenta
        try {
            configuracionService.verificarYRenovarToken(config.getId());
        } catch (Exception e) {
            comprobante.setEstado(EstadoComprobante.NO_TRANSMITIDO);
            comprobante.setUltimoError("No se obtuvo el token de Hacienda: " + e.getMessage());
            return mapper.toDTO(repository.save(comprobante));
        }

        comprobante.setFechaEnvio(LocalDateTime.now(clock));
        comprobante.incrementarIntentos();
        RespuestaHaciendaDTO respuesta = haciendaApi.enviarComprobante(
            comprobante.getXmlComprobante(),
            comprobante.getClaveNumerica(),
            config.getId()
        );

        try {
            ResultadoEnvioHacienda resultado = respuesta.getResultadoEnvio() != null
                ? respuesta.getResultadoEnvio()
                : ResultadoEnvioHacienda.ENVIO_INCIERTO;
            comprobante.setEstado(resultado.estado());
            comprobante.setCodigoRespuesta(respuesta.getCodigoHttp() != null
                ? String.valueOf(respuesta.getCodigoHttp()) : null);
            comprobante.setMensajeRespuesta(respuesta.getMensaje());
            comprobante.setFechaRespuesta(respuesta.getFechaRespuesta());
            comprobante.setUltimoError(resultado == ResultadoEnvioHacienda.RECIBIDO ? null : respuesta.getMensaje());
            log.info("Envío de {} clasificado como {} → {}", comprobante.getClaveNumerica(), resultado,
                comprobante.getEstado());
        } catch (Exception e) {
            // Ya hubo un POST: ante cualquier duda, Hacienda pudo recibirlo
            log.error("Error procesando la respuesta del envío: {}", e.getMessage());
            comprobante.setEstado(EstadoComprobante.ENVIO_INCIERTO);
            comprobante.setUltimoError(e.getMessage());
        }

        return mapper.toDTO(repository.save(comprobante));
    }

    /**
     * Deja el comprobante con su XML firmado. Regenera el XML (misma clave y fecha de emisión) si se descartó o si
     * quedó en ERROR, y firma si está en GENERADO o se regeneró.
     *
     * @return false si quedó en ERROR (falla local)
     */
    private boolean prepararXmlFirmado(ComprobanteElectronico comprobante) {
        boolean regenerar = comprobante.getXmlComprobante() == null
            || comprobante.getEstado() == EstadoComprobante.ERROR;
        if (regenerar) {
            try {
                comprobante.setXmlComprobante(generarXml(comprobante));
                comprobante.setEstado(EstadoComprobante.GENERADO);
                log.info("XML regenerado con la misma clave {}", comprobante.getClaveNumerica());
            } catch (Exception e) {
                comprobante.setEstado(EstadoComprobante.ERROR);
                comprobante.setUltimoError("Error generando el XML: " + e.getMessage());
                return false;
            }
        }
        if (comprobante.getEstado() == EstadoComprobante.GENERADO) {
            firmarInterno(comprobante);
        }
        return comprobante.getEstado() != EstadoComprobante.ERROR;
    }

    @Override
    @Transactional
    public ComprobanteElectronicoDTO reenviar(Long id) {
        log.info("Reenviando comprobante ID: {}", id);

        ComprobanteElectronico comprobante = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado"));

        if (!comprobante.puedeReenviar()) {
            throw new IllegalStateException("Comprobante no puede ser reenviado en estado " + comprobante.getEstado());
        }

        return enviarAHacienda(id);
    }

    @Override
    @Transactional
    public ComprobanteElectronicoDTO reemitir(Long id) {
        ComprobanteElectronico rechazado = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado"));
        if (rechazado.getEstado() != EstadoComprobante.RECHAZADO || !Boolean.TRUE.equals(rechazado.getVigente())) {
            throw new IllegalStateException("Solo se reemite el comprobante vigente RECHAZADO por Hacienda; "
                + "este está en estado " + rechazado.getEstado());
        }
        if (rechazado.getFactura() == null) {
            throw new IllegalStateException("El comprobante no está asociado a una factura");
        }
        log.info("Reemitiendo la factura {} tras el rechazo de {}", rechazado.getFactura().getIdFactura(),
            rechazado.getClaveNumerica());
        return generarDesdeFactura(rechazado.getFactura().getIdFactura().longValue());
    }

    @Override
    @Transactional
    public ComprobanteElectronicoDTO consultarEstado(Long id) {
        log.info("Consultando estado de comprobante ID: {}", id);
        try {
            return consultarYActualizarEstado(id);
        } catch (Exception e) {
            log.error("Error consultando estado: {}", e.getMessage());
            return repository.findById(id).map(mapper::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado"));
        }
    }

    /**
     * Sondeo del estado (tras el envío y en el job horario). Adopta lo que informa Hacienda (ADR-030; mapeo en
     * aplicarRespuestaDeConsulta). Un 404 se interpreta con la misma regla que la consulta manual
     * ({@link #interpretarNoExiste}).
     */
    @Override
    @Transactional
    public ComprobanteElectronicoDTO consultarYActualizarEstado(Long comprobanteId) {
        log.info("🔍 Consultando y actualizando estado de comprobante ID: {}", comprobanteId);

        ComprobanteElectronico comprobante = repository.findById(comprobanteId)
            .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado"));
        if (comprobante.getEstado().esTerminal()) {
            return mapper.toDTO(comprobante); // ACEPTADO, RECHAZADO (historial inmutable) o ANULADO
        }

        ConfiguracionHacienda config = configuracionActivaDe(comprobante.getEmpresa());

        try {
            var respuesta = haciendaApi.consultarComprobante(
                comprobante.getClaveNumerica(),
                config.getId()
            );

            if (Boolean.TRUE.equals(respuesta.getDocumentoNoExiste())) {
                interpretarNoExiste(comprobante, respuesta);
                return mapper.toDTO(repository.save(comprobante));
            }

            aplicarRespuestaDeConsulta(comprobante, respuesta);
            comprobante = repository.save(comprobante);
            log.info("✅ Estado actualizado: {}", comprobante.getEstado());

            return mapper.toDTO(comprobante);

        } catch (Exception e) {
            log.error("❌ Error consultando estado en Hacienda: {}", e.getMessage());
            comprobante.setUltimoError("Error consultando estado: " + e.getMessage());
            repository.save(comprobante);
            throw new RuntimeException("Error consultando estado en Hacienda", e);
        }
    }

    /**
     * "Consultar estado en Hacienda" (ADR-031 §4): GET /recepcion/{clave}, de solo lectura, para un comprobante
     * ENVIADO, PROCESANDO, ENVIO_INCIERTO o ERROR_HACIENDA, como máximo 1 vez cada 30 s por comprobante. Un 404 se
     * interpreta según el estado de origen ({@link #interpretarNoExiste}); otra respuesta se adopta como en el sondeo.
     * En ERROR_HACIENDA, el contador de consultas con "error" vuelve a 0 antes de aplicar la respuesta, de modo que el
     * sondeo lo vuelve a incluir.
     *
     * @throws ConsultaManualNoPermitidaException (409) si el estado no se consulta a mano
     * @throws ConsultaManualDemasiadoProntoException (429) si no pasaron 30 s desde la consulta manual anterior
     */
    @Override
    @Transactional
    public ResultadoConsultaManualDTO consultarEstadoManual(Long id) {
        ComprobanteElectronico comprobante = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado"));
        EstadoComprobante estadoPrevio = comprobante.getEstado();
        if (!CONSULTABLES_A_MANO.contains(estadoPrevio)) {
            throw new ConsultaManualNoPermitidaException("Solo se consulta el estado en Hacienda de un comprobante "
                + "ENVIADO, PROCESANDO, ENVIO_INCIERTO o ERROR_HACIENDA; este está en estado " + estadoPrevio);
        }
        limiteConsultaManual.exigirYRegistrar(id);

        ConfiguracionHacienda config = configuracionActivaDe(comprobante.getEmpresa());
        RespuestaHaciendaDTO respuesta = haciendaApi.consultarComprobante(comprobante.getClaveNumerica(), config.getId());
        if (estadoPrevio == EstadoComprobante.ERROR_HACIENDA) {
            comprobante.setConsultasErrorHacienda(0);
        }
        String mensaje;
        if (Boolean.TRUE.equals(respuesta.getDocumentoNoExiste())) {
            mensaje = interpretarNoExiste(comprobante, respuesta);
        } else {
            aplicarRespuestaDeConsulta(comprobante, respuesta);
            mensaje = "Estado consultado en Hacienda: " + comprobante.getEstado().getCodigo();
        }
        log.info("Consulta manual de {} ({}): {} → {}", comprobante.getClaveNumerica(), estadoPrevio,
            Boolean.TRUE.equals(respuesta.getDocumentoNoExiste()) ? "404" : "ind-estado " + respuesta.getIndicadorEstado(),
            comprobante.getEstado());
        return new ResultadoConsultaManualDTO(mapper.toDTO(repository.save(comprobante)), mensaje);
    }

    /**
     * Regla del 404 de GET /recepcion/{clave} ("no existe"), única para la consulta manual y el sondeo (ADR-031 §4):
     * <ul>
     *   <li>ENVIO_INCIERTO y al menos 15 minutos desde el último envío → NO_TRANSMITIDO (se corrige y se reenvía con la
     *       misma clave);</li>
     *   <li>ENVIO_INCIERTO antes de 15 minutos → sin cambios: Hacienda puede tardar en registrar un envío;</li>
     *   <li>ENVIADO, PROCESANDO o ERROR_HACIENDA → ENVIO_INCIERTO (T-025): Hacienda ya confirmó la recepción, así que
     *       nunca pasa directo a NO_TRANSMITIDO.</li>
     * </ul>
     *
     * @return mensaje para el usuario
     */
    private String interpretarNoExiste(ComprobanteElectronico comprobante, RespuestaHaciendaDTO respuesta) {
        EstadoComprobante estado = comprobante.getEstado();
        String mensaje;
        if (estado == EstadoComprobante.ENVIO_INCIERTO) {
            LocalDateTime desde = disponibleParaVerificar(comprobante);
            if (desde == null || !LocalDateTime.now(clock).isBefore(desde)) {
                comprobante.setEstado(EstadoComprobante.NO_TRANSMITIDO);
                mensaje = "Hacienda no tiene el comprobante: queda como no transmitido y se puede corregir y reenviar "
                    + "con la misma clave.";
            } else {
                mensaje = "Hacienda todavía no registra el comprobante; puede tardar hasta " + MINUTOS_ANTES_DE_VERIFICAR
                    + " minutos desde el envío. Si a partir de las " + desde.format(HORA)
                    + " sigue sin registrarlo, la consulta lo marcará como no transmitido.";
            }
        } else if (estado == EstadoComprobante.ENVIADO || estado == EstadoComprobante.PROCESANDO
                || estado == EstadoComprobante.ERROR_HACIENDA) {
            comprobante.setEstado(EstadoComprobante.ENVIO_INCIERTO);
            comprobante.setConsultasErrorHacienda(0);
            mensaje = "Hacienda respondió que no tiene el comprobante, aunque había confirmado su recepción: queda como "
                + "envío incierto. Vuelva a consultar más tarde.";
        } else {
            mensaje = "Hacienda no tiene el comprobante.";
        }
        comprobante.setMensajeRespuesta(respuesta.getMensaje());
        comprobante.setFechaRespuesta(respuesta.getFechaRespuesta());
        log.warn("⚠️ Hacienda no tiene el comprobante {} (404): {} → {}", comprobante.getClaveNumerica(), estado,
            comprobante.getEstado());
        return mensaje;
    }

    /** Momento a partir del cual un 404 de un ENVIO_INCIERTO significa NO_TRANSMITIDO (null si no hubo envío). */
    public static LocalDateTime disponibleParaVerificar(ComprobanteElectronico comprobante) {
        return disponibleParaVerificar(comprobante.getFechaEnvio());
    }

    /** Momento a partir del cual un 404 significa NO_TRANSMITIDO para un envío hecho en fechaEnvio (null si no hubo). */
    public static LocalDateTime disponibleParaVerificar(LocalDateTime fechaEnvio) {
        return fechaEnvio != null ? fechaEnvio.plusMinutes(MINUTOS_ANTES_DE_VERIFICAR) : null;
    }

    /**
     * Adopta el estado informado por la consulta. Cada ind-estado documentado de GET /recepcion/{clave} se mapea de
     * forma explícita (docs/referencias/HACIENDA_API_RECEPCION.md):
     * <ul>
     *   <li>recibido, procesando → PROCESANDO (Hacienda lo tiene y no lo resolvió);</li>
     *   <li>aceptado → ACEPTADO; rechazado → RECHAZADO (única asignación de RECHAZADO);</li>
     *   <li>error → ERROR_HACIENDA: bloqueado, porque Hacienda tiene el documento. No es PROCESANDO para que se vea
     *       que Hacienda informó un error y se pueda señalar tras N consultas seguidas;</li>
     *   <li>cualquier otro valor (o ninguno) → ENVIO_INCIERTO: no se sabe qué tiene Hacienda; se resuelve con
     *       "Consultar estado en Hacienda".</li>
     * </ul>
     * El contador de consultas con "error" vuelve a 0 con cualquier otra respuesta.
     */
    private void aplicarRespuestaDeConsulta(ComprobanteElectronico comprobante, RespuestaHaciendaDTO respuesta) {
        comprobante.setCodigoRespuesta(respuesta.getCodigoMensaje() != null
            ? respuesta.getCodigoMensaje().getCodigo() : null);
        comprobante.setMensajeRespuesta(respuesta.getMensaje());
        comprobante.setFechaRespuesta(respuesta.getFechaRespuesta());
        if (respuesta.getXmlRespuesta() != null && !respuesta.getXmlRespuesta().isBlank()) {
            try {
                comprobante.setXmlRespuesta(new String(
                    Base64.getDecoder().decode(respuesta.getXmlRespuesta()), StandardCharsets.UTF_8));
            } catch (IllegalArgumentException e) {
                comprobante.setXmlRespuesta(respuesta.getXmlRespuesta());
            }
        }
        String indicador = respuesta.getIndicadorEstado() != null
            ? respuesta.getIndicadorEstado().trim().toLowerCase() : "";
        EstadoComprobante estado = switch (indicador) {
            case "recibido", "procesando" -> EstadoComprobante.PROCESANDO;
            case "aceptado" -> EstadoComprobante.ACEPTADO;
            case "rechazado" -> EstadoComprobante.RECHAZADO;
            case "error" -> EstadoComprobante.ERROR_HACIENDA;
            default -> {
                log.warn("ind-estado no documentado '{}' para {}: ENVIO_INCIERTO", respuesta.getIndicadorEstado(),
                    comprobante.getClaveNumerica());
                yield EstadoComprobante.ENVIO_INCIERTO;
            }
        };
        comprobante.setEstado(estado);
        if (estado != EstadoComprobante.ERROR_HACIENDA) {
            comprobante.setConsultasErrorHacienda(0);
            return;
        }
        int consultas = (comprobante.getConsultasErrorHacienda() != null ? comprobante.getConsultasErrorHacienda() : 0) + 1;
        comprobante.setConsultasErrorHacienda(consultas);
        if (comprobante.requiereRevisionEnAtv(maxConsultasErrorHacienda)) {
            comprobante.setMensajeRespuesta(mensajeRevisarEnAtv(consultas)
                + (respuesta.getMensaje() != null && !respuesta.getMensaje().isBlank()
                    ? " Último mensaje de Hacienda: " + respuesta.getMensaje() : ""));
            log.warn("Comprobante {}: {} consultas seguidas con ind-estado 'error'; se deja de consultar (revisar en ATV)",
                comprobante.getClaveNumerica(), consultas);
        }
    }

    /** Mensaje con el que se señala un comprobante ERROR_HACIENDA que el sondeo ya no consulta. */
    static String mensajeRevisarEnAtv(int consultas) {
        return "Hacienda informó \"error\" en " + consultas + " consultas seguidas: se dejó de consultar. "
            + "Revise el comprobante en ATV.";
    }

    @Override
    @Transactional
    public boolean enviarPorEmail(Long id, String emailAdicional) {
        log.info("Enviando comprobante por email ID: {}", id);

        ComprobanteElectronico comprobante = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado"));

        Factura factura = comprobante.getFactura();
        Cliente cliente = (factura != null) ? factura.getCliente() : null;

        String destino = (emailAdicional != null && !emailAdicional.isBlank())
            ? emailAdicional
            : (cliente != null ? cliente.getEmail() : null);

        if (destino == null || destino.isBlank()) {
            throw new IllegalStateException("El cliente no tiene email registrado y no se proporcionó un email adicional");
        }

        try {
            api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa empresa =
                comprobante.getEmpresa();

            String clave = comprobante.getClaveNumerica();
            String tipoDesc = comprobante.getTipoComprobante().getDescripcion();

            DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            DateTimeFormatter fmtHora  = DateTimeFormatter.ofPattern("HH:mm");
            LocalDateTime emision = comprobante.getFechaEmision();

            String moneda = (factura != null && factura.getMonedaFE() != null)
                ? factura.getMonedaFE().name() : "";
            String total = (factura != null && factura.getTotal() != null)
                ? String.format("%,.2f", factura.getTotal()) : "0.00";
            String consecutivo = comprobante.getConsecutivo();
            String numFactura  = (factura != null) ? factura.getNumeroFactura() : "";

            String clienteNombre = (cliente != null) ? cliente.getNombre() : "";
            String clienteId     = (cliente != null && cliente.getNumeroIdentificacion() != null)
                ? cliente.getNumeroIdentificacion() : "";
            String clienteTel    = (cliente != null && cliente.getTelefono() != null)
                ? cliente.getTelefono() : "";
            String clienteEmail  = (cliente != null && cliente.getEmail() != null)
                ? cliente.getEmail() : "";

            String emisorNombre = (empresa != null && empresa.getRazonSocial() != null)
                ? empresa.getRazonSocial() : "";
            String emisorId     = (empresa != null && empresa.getNumeroIdentificacion() != null)
                ? empresa.getNumeroIdentificacion() : "";
            String emisorTel    = (empresa != null && empresa.getTelefono() != null)
                ? empresa.getTelefono() : "";
            String emisorEmail  = (empresa != null && empresa.getEmail() != null)
                ? empresa.getEmail() : "";

            String cuerpo = String.format(
                "Estimado(a) cliente:%n%s%n%n" +
                "Encontrará adjunto el comprobante electrónico para %s en formato XML " +
                "y su representación en formato PDF, con las siguientes características:%n%n" +
                "Datos del emisor:%n" +
                "Nombre:                          %s%n" +
                "Número de identificación:        %s%n" +
                "Teléfono:                        %s%n" +
                "Email:                           %s%n%n" +
                "Datos del receptor:%n" +
                "Nombre:                          %s%n" +
                "Número de identificación:        %s%n" +
                "Teléfono:                        %s%n" +
                "Email:                           %s%n%n" +
                "Datos de la Factura Electrónica:%n" +
                "Consecutivo electrónico:         %s%n" +
                "Clave numérica:                  %s%n" +
                "Consecutivo interno:             %s%n" +
                "Fecha:                           %s%n" +
                "Hora:                            %s%n" +
                "Moneda:                          %s%n" +
                "Total:                           %s%n%n" +
                "Adjuntos de la Factura Electrónica:%n" +
                "XML (comprimidos):               %s_documentos.zip%n" +
                "PDF:                             %s.pdf%n",
                clienteNombre, tipoDesc,
                emisorNombre, emisorId, emisorTel, emisorEmail,
                clienteNombre, clienteId, clienteTel, clienteEmail,
                consecutivo, clave, numFactura,
                emision.format(fmtFecha), emision.format(fmtHora),
                moneda, total,
                clave, clave
            );

            String asunto = tipoDesc + " - " + numFactura;

            Map<String, byte[]> adjuntos = new LinkedHashMap<>();

            ByteArrayOutputStream zipBuffer = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(zipBuffer)) {
                if (comprobante.getXmlComprobante() != null) {
                    zos.putNextEntry(new ZipEntry(clave + ".xml"));
                    zos.write(comprobante.getXmlComprobante().getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
                if (comprobante.getXmlRespuesta() != null) {
                    zos.putNextEntry(new ZipEntry(clave + "_respuesta.xml"));
                    zos.write(comprobante.getXmlRespuesta().getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
            }
            if (zipBuffer.size() > 0) {
                adjuntos.put(clave + "_documentos.zip", zipBuffer.toByteArray());
            }

            if (factura != null) {
                byte[] pdf = facturaPdfService.generarPdfFactura(factura.getIdFactura());
                adjuntos.put(clave + ".pdf", pdf);
            }

            emailService.enviarEmailConMultiplesAdjuntos(destino, asunto, cuerpo, adjuntos);

            comprobante.setEnviadoEmail(true);
            comprobante.setFechaEnvioEmail(LocalDateTime.now());
            repository.save(comprobante);

            log.info("✅ Comprobante {} enviado por email a: {}", clave, destino);
            return true;

        } catch (Exception e) {
            log.error("❌ Error al enviar comprobante por email ID {}: {}", id, e.getMessage());
            throw new RuntimeException("Error al enviar comprobante por email: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public ComprobanteElectronicoDTO anular(Long id, String motivo) {
        log.info("Anulando comprobante ID: {}", id);
        
        ComprobanteElectronico comprobante = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado"));
        
        if (comprobante.getEstado() != EstadoComprobante.ACEPTADO) {
            throw new IllegalStateException("Solo se pueden anular comprobantes aceptados");
        }
        
        // TODO: Generar nota de crédito para anular
        
        comprobante.setEstado(EstadoComprobante.ANULADO);
        comprobante.setUltimoError("Anulado: " + motivo);
        
        comprobante = repository.save(comprobante);
        return mapper.toDTO(comprobante);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long contarPorEstado(Integer empresaId, EstadoComprobante estado) {
        return repository.countByEmpresaIdAndEstado(empresaId, estado.name());
    }
    
    @Override
    @Transactional(readOnly = true)
    public String descargarXml(Long id) {
        log.info("📎 Descargando XML del comprobante ID: {}", id);
        
        ComprobanteElectronico comprobante = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado"));
        
        // Intentar leer desde filesystem si existe
        if (comprobante.getRutaArchivoXml() != null) {
            try {
                Path path = Paths.get(comprobante.getRutaArchivoXml());
                if (Files.exists(path)) {
                    String xml = Files.readString(path);
                    log.info("✅ XML leído desde filesystem: {}", path);
                    return xml;
                }
            } catch (IOException e) {
                log.warn("⚠️ No se pudo leer XML desde filesystem, usando BD: {}", e.getMessage());
            }
        }
        
        // Fallback a BD
        if (comprobante.getXmlComprobante() != null) {
            log.info("💾 XML obtenido desde base de datos");
            return comprobante.getXmlComprobante();
        }
        
        throw new IllegalStateException("No hay XML disponible para este comprobante");
    }
    
    @Override
    @Transactional
    public String guardarXmlEnFilesystem(Long id, String xml) {
        log.info("💾 Guardando XML del comprobante ID: {} en filesystem", id);
        
        ComprobanteElectronico comprobante = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado"));
        
        try {
            // Generar ruta: /comprobantes/{año}/{mes}/{claveNumerica}.xml
            LocalDateTime fecha = comprobante.getFechaEmision();
            String anio = String.valueOf(fecha.getYear());
            String mes = String.format("%02d", fecha.getMonthValue());
            
            // Crear directorio si no existe
            Path directorio = Paths.get(directorioBase, anio, mes);
            Files.createDirectories(directorio);
            
            // Crear archivo
            String nombreArchivo = comprobante.getClaveNumerica() + ".xml";
            Path archivoPath = directorio.resolve(nombreArchivo);
            
            // Escribir XML
            Files.writeString(archivoPath, xml, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            String rutaRelativa = String.format("%s/%s/%s", anio, mes, nombreArchivo);
            
            // Actualizar comprobante con ruta
            comprobante.setRutaArchivoXml(archivoPath.toString());
            repository.save(comprobante);
            
            log.info("✅ XML guardado exitosamente en: {}", archivoPath);
            return rutaRelativa;
            
        } catch (IOException e) {
            log.error("❌ Error guardando XML en filesystem: {}", e.getMessage(), e);
            throw new RuntimeException("Error guardando XML: " + e.getMessage(), e);
        }
    }
    
    /**
     * Genera XML según el tipo de comprobante.
     */
    private String generarXml(ComprobanteElectronico comprobante) {
        return switch (comprobante.getTipoComprobante()) {
            case FACTURA_ELECTRONICA, FACTURA_ELECTRONICA_COMPRA, FACTURA_ELECTRONICA_EXPORTACION ->
                xmlGenerator.generarXmlFactura(comprobante);
            case TIQUETE_ELECTRONICO ->
                xmlGenerator.generarXmlTiquete(comprobante);
            case NOTA_CREDITO ->
                xmlGenerator.generarXmlNotaCredito(comprobante);
            case NOTA_DEBITO ->
                xmlGenerator.generarXmlNotaDebito(comprobante);
            default ->
                throw new IllegalArgumentException("Tipo de comprobante no soportado para generación de XML");
        };
    }
    
    /**
     * Convierte resultados de SP (Object[]) a DTOs.
     * JDBC retorna java.sql.Timestamp para DATETIME y Number para INT/TINYINT —
     * nunca castear directamente a LocalDateTime, Integer o Boolean.
     */
    private List<ComprobanteElectronicoDTO> convertirResultadosADTO(List<Object[]> results) {
        return results.stream()
            .map(row -> ComprobanteElectronicoDTO.builder()
                .id(((Number) row[0]).longValue())
                .claveNumerica((String) row[1])
                .consecutivo((String) row[2])
                .tipoComprobante(TipoComprobanteElectronico.valueOf((String) row[3]))
                .fechaEmision(row[4] != null ? ((java.sql.Timestamp) row[4]).toLocalDateTime() : null)
                .estado(EstadoComprobante.valueOf((String) row[5]))
                .facturaId(row[6] != null ? ((Number) row[6]).longValue() : null)
                .empresaId(((Number) row[7]).longValue())
                .codigoRespuesta((String) row[8])
                .mensajeRespuesta((String) row[9])
                .intentosEnvio(row[10] != null ? ((Number) row[10]).intValue() : 0)
                .enviadoEmail(row[11] instanceof Boolean b ? b : row[11] instanceof Number n && n.intValue() != 0)
                // Acción "Consultar estado en Hacienda" del listado (ADR-031 §4)
                .consultableEnHacienda(CONSULTABLES_A_MANO.contains(EstadoComprobante.valueOf((String) row[5])))
                .build())
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * ADR-021: la única configuración de Hacienda activa, que debe pertenecer a la empresa indicada
     * (la empresa única al emitir; la del comprobante en los pasos posteriores).
     */
    private ConfiguracionHacienda configuracionActivaDe(ConfiguracionEmpresa empresa) {
        ConfiguracionHacienda config = configuracionService.obtenerConfiguracionActiva();
        Integer empresaDeLaConfiguracion = config.getEmpresa().getIdConfiguracion();
        if (!empresaDeLaConfiguracion.equals(empresa.getIdConfiguracion())) {
            throw new IllegalStateException("La configuración de Hacienda activa (id " + config.getId()
                + ") pertenece a la empresa " + empresaDeLaConfiguracion + ", no a la empresa "
                + empresa.getIdConfiguracion() + ".");
        }
        return config;
    }
}
