package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.cliente.service.ClienteService;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ConfiguracionHaciendaNoDisponibleException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.CertificadoHaciendaValidator;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.LecturaConfiguracionActiva;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import api.astro.whats_orders_manager.modules.producto.service.ArticuloMaestroService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * T-043 (ADR-048): datos del panel de inicio de Facturación. Solo conteos operativos (sin montos ni gráficos) y el
 * estado de la configuración de Hacienda sin datos sensibles. Los conteos son consultas COUNT; el contador de clientes
 * con datos FE inválidos reutiliza el existente (T-038), que valida en memoria con las reglas de DatosFeValidator.
 */
@Service
@Slf4j
public class PanelFacturacionService {

    public static final String MENSAJE_CERTIFICADO_ILEGIBLE = "No se pudo leer el certificado";
    /** Días de anticipación con los que el vencimiento del certificado pasa a alerta. */
    public static final long DIAS_ALERTA_CERTIFICADO = 30;

    /** Estados de comprobante vigente que requieren acción (ADR-048 §2). */
    static final List<EstadoComprobante> ESTADOS_CON_ACCION = List.of(EstadoComprobante.ERROR,
        EstadoComprobante.NO_TRANSMITIDO, EstadoComprobante.ENVIO_INCIERTO, EstadoComprobante.RECHAZADO);

    /** Contadores del panel; todos son enteros no negativos. */
    public record Contadores(long comprobantesError, long comprobantesNoTransmitido, long comprobantesEnvioIncierto,
                             long comprobantesRechazado, long notasEnTramite, long anuladasSinSustituta,
                             long sinComprobante, long articulosSinCabys, long clientesFeInvalidos) {
    }

    /**
     * Tarjeta de Hacienda: solo ambiente, vencimiento del certificado y si hay token vigente. Nunca usuario,
     * contraseña, PIN ni token. {@code aviso} no es null cuando no hay configuración legible (textos fijos de T-041).
     */
    public record TarjetaHacienda(String aviso, String ambiente, boolean produccion, String certificadoTexto,
                                  Long diasParaVencer, boolean certificadoAlerta, boolean tokenVigente,
                                  LocalDateTime tokenExpira) {

        public boolean disponible() {
            return aviso == null;
        }

        /** Ambiente como enum, o null si no hay ambiente legible. */
        public AmbienteHacienda ambienteEnum() {
            return ambiente == null ? null : AmbienteHacienda.valueOf(ambiente);
        }

        public static TarjetaHacienda conAviso(String aviso) {
            return new TarjetaHacienda(aviso, null, false, null, null, false, false, null);
        }
    }

    /**
     * {@code ambienteContadores} es el ambiente por el que se filtraron los contadores de comprobantes, notas y
     * anuladas (el de la configuración activa) o null si no hay configuración legible: entonces cuentan todos los
     * ambientes y la vista muestra {@link #LEYENDA_SIN_AMBIENTE}.
     */
    public record Panel(Contadores contadores, TarjetaHacienda hacienda, AmbienteHacienda ambienteContadores) {
    }

    public static final String LEYENDA_SIN_AMBIENTE =
        "Sin ambiente activo: los contadores incluyen todos los ambientes";

    private final ComprobanteElectronicoRepository comprobanteRepository;
    private final NotaFacturaRepository notaRepository;
    private final FacturaRepository facturaRepository;
    private final ArticuloMaestroService articuloMaestroService;
    private final ClienteService clienteService;
    private final ConfiguracionHaciendaService configuracionService;
    private final ConfiguracionEmpresaRepository empresaRepository;
    private final CertificadoHaciendaValidator certificadoValidator;
    private final PlatformTransactionManager transactionManager;

    public PanelFacturacionService(ComprobanteElectronicoRepository comprobanteRepository,
                                   NotaFacturaRepository notaRepository, FacturaRepository facturaRepository,
                                   ArticuloMaestroService articuloMaestroService, ClienteService clienteService,
                                   ConfiguracionHaciendaService configuracionService,
                                   ConfiguracionEmpresaRepository empresaRepository,
                                   CertificadoHaciendaValidator certificadoValidator,
                                   PlatformTransactionManager transactionManager) {
        this.comprobanteRepository = comprobanteRepository;
        this.notaRepository = notaRepository;
        this.facturaRepository = facturaRepository;
        this.articuloMaestroService = articuloMaestroService;
        this.clienteService = clienteService;
        this.configuracionService = configuracionService;
        this.empresaRepository = empresaRepository;
        this.certificadoValidator = certificadoValidator;
        this.transactionManager = transactionManager;
    }

    /**
     * Primero la lectura de Hacienda: su ambiente (null si no hay configuración legible) parametriza los contadores
     * (T-044: lo de otro ambiente no se puede reenviar ni sustituir desde la configuración activa).
     */
    public Panel obtener() {
        TarjetaHacienda hacienda = tarjetaHacienda(Instant.now());
        AmbienteHacienda ambiente = hacienda.disponible() ? hacienda.ambienteEnum() : null;
        return new Panel(contadores(ambiente), hacienda, ambiente);
    }

    /**
     * @param ambiente ambiente de la configuración activa; null = sin filtrar por ambiente. «Facturas sin comprobante»
     *                 no lo usa: cuenta las no anuladas sin ningún comprobante, de cualquier ambiente.
     */
    @Transactional(readOnly = true)
    public Contadores contadores(AmbienteHacienda ambiente) {
        Map<EstadoComprobante, Long> porEstado = new EnumMap<>(EstadoComprobante.class);
        for (Object[] fila : comprobanteRepository.contarVigentesPorEstado(ESTADOS_CON_ACCION, ambiente)) {
            porEstado.put((EstadoComprobante) fila[0], ((Number) fila[1]).longValue());
        }
        return new Contadores(
            porEstado.getOrDefault(EstadoComprobante.ERROR, 0L),
            porEstado.getOrDefault(EstadoComprobante.NO_TRANSMITIDO, 0L),
            porEstado.getOrDefault(EstadoComprobante.ENVIO_INCIERTO, 0L),
            porEstado.getOrDefault(EstadoComprobante.RECHAZADO, 0L),
            notaRepository.contarPorEstado(EstadoNotaFactura.EN_TRAMITE, ambiente),
            facturaRepository.contarAnuladasQueAdmitenSustituta(ambiente),
            facturaRepository.contarSinComprobante(),
            articuloMaestroService.countActivosSinCabys(),
            clienteService.contarClientesConDatosFeInvalidos());
    }

    /**
     * Estado de Hacienda con la lectura aislada de T-041 (no {@code obtenerConfiguracionActiva(empresaId)}, P-098).
     * Nunca lanza: sin configuración válida o con credenciales ilegibles devuelve el aviso fijo correspondiente.
     */
    public TarjetaHacienda tarjetaHacienda(Instant ahora) {
        ConfiguracionHacienda config;
        String rutaCertificado;
        String pin;
        try {
            ConfiguracionEmpresa empresa = empresaRepository.findFirstByOrderByIdConfiguracionAsc().orElse(null);
            config = LecturaConfiguracionActiva.leer(configuracionService, transactionManager, empresa);
            rutaCertificado = config.getRutaCertificado();
            pin = config.getPinCertificado();
        } catch (ConfiguracionHaciendaNoDisponibleException e) {
            return TarjetaHacienda.conAviso(e.getMensajeUsuario());
        } catch (RuntimeException e) {
            log.warn("Panel de Facturación: no se pudo leer la configuración de Hacienda ({})",
                e.getClass().getSimpleName());
            return TarjetaHacienda.conAviso(ConfiguracionHaciendaNoDisponibleException.MENSAJE_ILEGIBLE);
        }
        String ambiente = config.getAmbiente() != null ? config.getAmbiente().name() : null;
        boolean produccion = "PRODUCCION".equals(ambiente);
        boolean tokenVigente = config.isTokenValido();
        LocalDateTime tokenExpira = config.getTokenExpira();

        String texto;
        Long dias = null;
        boolean alerta;
        try {
            // lectura propia del keystore: si falla, solo esta parte de la tarjeta lo dice
            Instant vence = certificadoValidator.leerVencimiento(rutaCertificado, pin);
            dias = ChronoUnit.DAYS.between(ahora.atZone(ZoneId.systemDefault()).toLocalDate(),
                vence.atZone(ZoneId.systemDefault()).toLocalDate());
            boolean vencido = !vence.isAfter(ahora);
            String fecha = vence.atZone(ZoneId.systemDefault()).toLocalDate().toString();
            texto = vencido ? "Vencido el " + fecha : "Vence el " + fecha + " (en " + dias + " días)";
            alerta = vencido || dias < DIAS_ALERTA_CERTIFICADO;
        } catch (RuntimeException e) {
            texto = MENSAJE_CERTIFICADO_ILEGIBLE;
            alerta = true;
        }
        return new TarjetaHacienda(null, ambiente, produccion, texto, dias, alerta, tokenVigente, tokenExpira);
    }
}
