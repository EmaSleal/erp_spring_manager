package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.EmpresaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.HaciendaCallbackDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ComprobanteElectronicoMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.*;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.ClaveNumericaGenerator;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

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
    private final EmpresaRepository empresaRepository;
    private final ConfiguracionHaciendaService configuracionService;
    private final XmlGeneratorService xmlGenerator;
    private final FirmaDigitalService firmaDigital;
    private final HaciendaApiService haciendaApi;
    private final ClaveNumericaGenerator claveGenerator;
    private final ComprobanteElectronicoMapper mapper;
    
    @Value("${facturacion.comprobantes.directorio-base:./comprobantes}")
    private String directorioBase;
    
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
            
            // Esperar 30 segundos antes de consultar estado
            Thread.sleep(30000);
            
            // Consultar y actualizar estado
            consultarYActualizarEstado(comprobanteId);
            
        } catch (InterruptedException e) {
            log.warn("⚠️ Proceso asíncrono interrumpido: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("❌ Error en procesamiento asíncrono: {}", e.getMessage());
            // Marcar como error
            try {
                ComprobanteElectronico comprobante = repository.findById(comprobanteId)
                    .orElseThrow();
                comprobante.setEstado(EstadoComprobante.ERROR);
                comprobante.setUltimoError("Error asíncrono: " + e.getMessage());
                repository.save(comprobante);
            } catch (Exception ex) {
                log.error("❌ No se pudo actualizar estado de error", ex);
            }
        }
    }
    
    @Override
    @Transactional
    public ComprobanteElectronicoDTO generarDesdeFactura(Long facturaId) {
        log.info("Generando comprobante electrónico desde factura ID: {}", facturaId);
        
        // Necesitamos el empresaId, obtenerlo de configuración activa
        // Por ahora usar empresaId 1 como default, debería pasarse como parámetro
        Long empresaId = 1L; // TODO: Pasar empresaId como parámetro
        TipoComprobanteElectronico tipo = TipoComprobanteElectronico.FACTURA_ELECTRONICA; // Default
        
        Factura factura = facturaRepository.findById(facturaId.intValue())
            .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada"));
        
        // Obtener empresa desde configuración activa ya que Factura no tiene referencia directa
        ConfiguracionHacienda config = configuracionService.obtenerActivaPorEmpresa(empresaId)
            .orElseThrow(() -> new IllegalStateException("No hay configuración activa de Hacienda"));
        
        Empresa empresa = config.getEmpresa();
        
        // Generar consecutivo y clave numérica
        Long consecutivo = config.siguienteConsecutivo(tipo.getCodigo());
        LocalDateTime fechaEmision = LocalDateTime.now();
        
        String claveNumerica = claveGenerator.generar(
            empresa.getRuc(),
            consecutivo,
            tipo,
            fechaEmision
        );
        
        // Crear comprobante
        ComprobanteElectronico comprobante = ComprobanteElectronico.builder()
            .factura(factura)
            .empresa(empresa)
            .tipoComprobante(tipo)
            .claveNumerica(claveNumerica)
            .consecutivo(String.format("%020d", consecutivo))
            .fechaEmision(fechaEmision)
            .estado(EstadoComprobante.GENERADO)
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
        
        ConfiguracionHacienda config = configuracionService.obtenerActivaPorEmpresa(
            comprobante.getEmpresa().getIdEmpresa().longValue()
        ).orElseThrow(() -> new IllegalStateException("No hay configuración activa"));
        
        try {
            String xmlFirmado = firmaDigital.firmarXml(
                comprobante.getXmlComprobante(),
                config.getRutaCertificado(),
                config.getPinCertificado()
            );
            
            comprobante.setXmlComprobante(xmlFirmado);
            comprobante.setEstado(EstadoComprobante.FIRMADO);
            
            comprobante = repository.save(comprobante);
            
            // Guardar XML firmado en filesystem
            try {
                guardarXmlEnFilesystem(comprobante.getId(), xmlFirmado);
            } catch (Exception e) {
                log.warn("⚠️ No se pudo guardar XML en filesystem: {}", e.getMessage());
            }
            
            log.info("Comprobante firmado exitosamente");
            
            return mapper.toDTO(comprobante);
        } catch (Exception e) {
            log.error("Error firmando comprobante: {}", e.getMessage());
            comprobante.setEstado(EstadoComprobante.ERROR);
            comprobante.setUltimoError("Error en firma digital: " + e.getMessage());
            comprobante = repository.save(comprobante);
            return mapper.toDTO(comprobante);
        }
    }
    
    @Override
    @Transactional
    public ComprobanteElectronicoDTO enviarAHacienda(Long id) {
        log.info("Enviando comprobante ID: {} a Hacienda", id);
        
        ComprobanteElectronico comprobante = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado"));
        
        if (!comprobante.listoParaEnviar()) {
            throw new IllegalStateException("Comprobante no está listo para enviar");
        }
        
        ConfiguracionHacienda config = configuracionService.obtenerActivaPorEmpresa(
            comprobante.getEmpresa().getIdEmpresa().longValue()
        ).orElseThrow(() -> new IllegalStateException("No hay configuración activa"));
        
        // Verificar y renovar token si es necesario
        configuracionService.verificarYRenovarToken(config.getId());
        
        try {
            comprobante.setEstado(EstadoComprobante.ENVIADO);
            comprobante.setFechaEnvio(LocalDateTime.now());
            comprobante.incrementarIntentos();
            
            var respuesta = haciendaApi.enviarComprobante(
                comprobante.getXmlComprobante(),
                comprobante.getClaveNumerica(),
                config.getId()
            );
            
            comprobante.setCodigoRespuesta(respuesta.getCodigoMensaje().getCodigo());
            comprobante.setMensajeRespuesta(respuesta.getMensaje());
            comprobante.setFechaRespuesta(respuesta.getFechaRespuesta());
            
            if (respuesta.getExitoso()) {
                comprobante.setEstado(EstadoComprobante.ACEPTADO);
                log.info("Comprobante aceptado por Hacienda");
            } else {
                comprobante.setEstado(EstadoComprobante.RECHAZADO);
                log.warn("Comprobante rechazado por Hacienda: {}", respuesta.getMensaje());
            }
            
        } catch (Exception e) {
            log.error("Error enviando a Hacienda: {}", e.getMessage());
            comprobante.setEstado(EstadoComprobante.ERROR);
            comprobante.setUltimoError(e.getMessage());
        }
        
        comprobante = repository.save(comprobante);
        return mapper.toDTO(comprobante);
    }
    
    @Override
    @Transactional
    public ComprobanteElectronicoDTO reenviar(Long id) {
        log.info("Reenviando comprobante ID: {}", id);
        
        ComprobanteElectronico comprobante = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado"));
        
        if (!comprobante.puedeReenviar()) {
            throw new IllegalStateException("Comprobante no puede ser reenviado");
        }
        
        return enviarAHacienda(id);
    }
    
    @Override
    @Transactional
    public ComprobanteElectronicoDTO consultarEstado(Long id) {
        log.info("Consultando estado de comprobante ID: {}", id);
        
        ComprobanteElectronico comprobante = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado"));
        
        ConfiguracionHacienda config = configuracionService.obtenerActivaPorEmpresa(
            comprobante.getEmpresa().getIdEmpresa().longValue()
        ).orElseThrow(() -> new IllegalStateException("No hay configuración activa"));
        
        try {
            var respuesta = haciendaApi.consultarComprobante(
                comprobante.getClaveNumerica(),
                config.getId()
            );
            
            comprobante.setCodigoRespuesta(respuesta.getCodigoMensaje().getCodigo());
            comprobante.setMensajeRespuesta(respuesta.getMensaje());
            comprobante.setFechaRespuesta(respuesta.getFechaRespuesta());
            
            if (respuesta.getExitoso()) {
                comprobante.setEstado(EstadoComprobante.ACEPTADO);
            }
            
            comprobante = repository.save(comprobante);
            return mapper.toDTO(comprobante);
        } catch (Exception e) {
            log.error("Error consultando estado: {}", e.getMessage());
            return mapper.toDTO(comprobante);
        }
    }
    
    @Override
    @Transactional
    public ComprobanteElectronicoDTO consultarYActualizarEstado(Long comprobanteId) {
        log.info("🔍 Consultando y actualizando estado de comprobante ID: {}", comprobanteId);
        
        ComprobanteElectronico comprobante = repository.findById(comprobanteId)
            .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado"));
        
        ConfiguracionHacienda config = configuracionService.obtenerActivaPorEmpresa(
            comprobante.getEmpresa().getIdEmpresa().longValue()
        ).orElseThrow(() -> new IllegalStateException("No hay configuración activa"));
        
        try {
            var respuesta = haciendaApi.consultarComprobante(
                comprobante.getClaveNumerica(),
                config.getId()
            );
            
            // Actualizar con información de respuesta
            comprobante.setCodigoRespuesta(respuesta.getCodigoMensaje().getCodigo());
            comprobante.setMensajeRespuesta(respuesta.getMensaje());
            comprobante.setFechaRespuesta(respuesta.getFechaRespuesta());
            
            // Determinar estado según respuesta
            if (respuesta.getExitoso()) {
                comprobante.setEstado(EstadoComprobante.ACEPTADO);
                log.info("✅ Comprobante ACEPTADO por Hacienda");
            } else if ("rechazado".equalsIgnoreCase(respuesta.getIndicadorEstado())) {
                comprobante.setEstado(EstadoComprobante.RECHAZADO);
                log.warn("❌ Comprobante RECHAZADO: {}", respuesta.getMensaje());
            } else if ("procesando".equalsIgnoreCase(respuesta.getIndicadorEstado())) {
                comprobante.setEstado(EstadoComprobante.ENVIADO);
                log.info("⏳ Comprobante aún en procesamiento");
            }
            
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
    
    @Override
    @Transactional
    public boolean enviarPorEmail(Long id, String emailAdicional) {
        log.info("Enviando comprobante por email ID: {}", id);
        
        ComprobanteElectronico comprobante = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado"));
        
        // TODO: Integrar con servicio de email
        // Adjuntar XML y PDF del comprobante
        // Usar emailAdicional si se proporcionó
        
        comprobante.setEnviadoEmail(true);
        comprobante.setFechaEnvioEmail(LocalDateTime.now());
        repository.save(comprobante);
        
        log.info("Email enviado (TODO: implementar servicio real)");
        return true;
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
     */
    private List<ComprobanteElectronicoDTO> convertirResultadosADTO(List<Object[]> results) {
        return results.stream()
            .map(row -> ComprobanteElectronicoDTO.builder()
                .id(((Number) row[0]).longValue())
                .claveNumerica((String) row[1])
                .consecutivo((String) row[2])
                .tipoComprobante(TipoComprobanteElectronico.valueOf((String) row[3]))
                .fechaEmision((LocalDateTime) row[4])
                .estado(EstadoComprobante.valueOf((String) row[5]))
                .facturaId(row[6] != null ? ((Number) row[6]).longValue() : null)
                .empresaId(((Number) row[7]).longValue())
                .codigoRespuesta((String) row[8])
                .mensajeRespuesta((String) row[9])
                .intentosEnvio((Integer) row[10])
                .enviadoEmail((Boolean) row[11])
                .build())
            .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    @Transactional
    public void procesarCallbackHacienda(HaciendaCallbackDTO callback) {
        log.info("🔄 Procesando callback de Hacienda para clave: {}", callback.getClaveNumerica());
        
        // 1. Buscar comprobante por clave numérica
        ComprobanteElectronico comprobante = repository.findByClaveNumerica(callback.getClaveNumerica())
            .orElseThrow(() -> new EntityNotFoundException(
                "Comprobante no encontrado con clave: " + callback.getClaveNumerica()
            ));
        
        log.debug("📋 Comprobante encontrado ID: {}, Estado actual: {}", 
            comprobante.getId(), comprobante.getEstado());
        
        // 2. Mapear estado de Hacienda a nuestro enum
        EstadoComprobante nuevoEstado = mapearEstadoHacienda(
            callback.getEstado(), 
            callback.getCodigoRespuesta()
        );
        
        log.info("🔄 Actualizando estado: {} → {}", comprobante.getEstado(), nuevoEstado);
        
        // 3. Actualizar comprobante
        comprobante.setEstado(nuevoEstado);
        comprobante.setCodigoRespuesta(callback.getCodigoRespuesta());
        comprobante.setMensajeRespuesta(callback.getMensaje());
        
        // 4. Parsear y guardar fecha de respuesta
        try {
            LocalDateTime fechaRespuesta = LocalDateTime.parse(
                callback.getFechaRespuesta(),
                DateTimeFormatter.ISO_DATE_TIME
            );
            comprobante.setFechaRespuesta(fechaRespuesta);
        } catch (Exception e) {
            log.warn("⚠️ Error parseando fecha de respuesta: {}. Usando fecha actual.", 
                callback.getFechaRespuesta());
            comprobante.setFechaRespuesta(LocalDateTime.now());
        }
        
        // 5. Guardar XML de respuesta (si viene)
        if (callback.getXmlRespuesta() != null && !callback.getXmlRespuesta().isEmpty()) {
            try {
                // Decodificar Base64
                String xmlDecodificado = new String(
                    Base64.getDecoder().decode(callback.getXmlRespuesta()),
                    StandardCharsets.UTF_8
                );
                
                comprobante.setXmlRespuesta(xmlDecodificado);
                log.info("💾 XML de respuesta decodificado y guardado en BD");
                
                // Guardar XML en filesystem
                guardarXmlRespuestaEnFilesystem(comprobante, xmlDecodificado);
                
            } catch (IllegalArgumentException e) {
                log.warn("⚠️ XML de respuesta no está en Base64, guardando directo");
                comprobante.setXmlRespuesta(callback.getXmlRespuesta());
            } catch (IOException e) {
                log.error("❌ Error guardando XML de respuesta en filesystem: {}", e.getMessage());
            }
        }
        
        // 6. Guardar en base de datos
        repository.save(comprobante);
        log.info("✅ Comprobante actualizado a estado: {}", nuevoEstado);
        
        // 7. Notificar al usuario (implementación futura)
        notificarCambioEstado(comprobante);
        
        // 8. Enviar email si está configurado
        if (debeEnviarEmailNotificacion(comprobante)) {
            enviarEmailNotificacion(comprobante);
        }
    }
    
    /**
     * Mapea el estado recibido de Hacienda a nuestro enum.
     * 
     * @param estado Estado textual de Hacienda
     * @param codigo Código de respuesta numérico
     * @return Estado mapeado
     */
    private EstadoComprobante mapearEstadoHacienda(String estado, String codigo) {
        if (estado == null || estado.isEmpty()) {
            log.warn("⚠️ Estado vacío, usando código: {}", codigo);
            return mapearPorCodigo(codigo);
        }
        
        return switch (estado.toLowerCase().trim()) {
            case "aceptado", "accepted" -> EstadoComprobante.ACEPTADO;
            case "rechazado", "rejected" -> EstadoComprobante.RECHAZADO;
            case "procesando", "processing" -> EstadoComprobante.ENVIADO;
            case "error", "failed" -> EstadoComprobante.ERROR;
            default -> {
                log.warn("⚠️ Estado desconocido: '{}', usando código: {}", estado, codigo);
                yield mapearPorCodigo(codigo);
            }
        };
    }
    
    /**
     * Mapea el estado basándose solo en el código de respuesta.
     */
    private EstadoComprobante mapearPorCodigo(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            return EstadoComprobante.ERROR;
        }
        
        return switch (codigo) {
            case "1" -> EstadoComprobante.ACEPTADO;      // Aceptado
            case "2" -> EstadoComprobante.ACEPTADO;      // Aceptado parcialmente
            case "3" -> EstadoComprobante.RECHAZADO;     // Rechazado
            default -> EstadoComprobante.ERROR;          // Otros códigos = error
        };
    }
    
    /**
     * Guarda el XML de respuesta en el filesystem.
     * 
     * @param comprobante Comprobante electrónico
     * @param xml XML de respuesta
     * @throws IOException si hay error guardando el archivo
     */
    private void guardarXmlRespuestaEnFilesystem(ComprobanteElectronico comprobante, String xml) 
            throws IOException {
        
        LocalDateTime fecha = comprobante.getFechaEmision();
        String año = String.valueOf(fecha.getYear());
        String mes = String.format("%02d", fecha.getMonthValue());
        
        // Crear estructura de directorios
        Path directorioBase = Paths.get(this.directorioBase);
        Path directorioComprobante = directorioBase.resolve(año).resolve(mes);
        Files.createDirectories(directorioComprobante);
        
        // Nombre del archivo: {claveNumerica}_respuesta.xml
        String nombreArchivo = comprobante.getClaveNumerica() + "_respuesta.xml";
        Path archivoXml = directorioComprobante.resolve(nombreArchivo);
        
        // Escribir archivo
        Files.writeString(
            archivoXml, 
            xml, 
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        );
        
        log.info("💾 XML de respuesta guardado en: {}", archivoXml.toAbsolutePath());
    }
    
    /**
     * Notifica al usuario mediante WebSocket sobre el cambio de estado.
     * 
     * @param comprobante Comprobante actualizado
     */
    private void notificarCambioEstado(ComprobanteElectronico comprobante) {
        // TODO: Implementar notificación WebSocket cuando el módulo esté disponible
        log.info("🔔 Notificación WebSocket pendiente para comprobante: {} - Estado: {}", 
            comprobante.getId(), comprobante.getEstado());
    }
    
    /**
     * Verifica si debe enviar email de notificación.
     * 
     * @param comprobante Comprobante electrónico
     * @return true si debe enviar email
     */
    private boolean debeEnviarEmailNotificacion(ComprobanteElectronico comprobante) {
        // Solo notificar para estados terminales importantes
        EstadoComprobante estado = comprobante.getEstado();
        return estado == EstadoComprobante.ACEPTADO || 
               estado == EstadoComprobante.RECHAZADO;
    }
    
    /**
     * Envía email de notificación al cliente sobre el estado del comprobante.
     * 
     * @param comprobante Comprobante electrónico
     */
    private void enviarEmailNotificacion(ComprobanteElectronico comprobante) {
        // TODO: Integrar con EmailService cuando esté implementado
        log.info("📧 Email de notificación pendiente para comprobante: {} - Estado: {}", 
            comprobante.getId(), comprobante.getEstado());
    }
}
