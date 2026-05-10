package api.astro.whats_orders_manager.modules.whatsapp.service;

import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.whatsapp.model.MensajeWhatsApp;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio de integración entre WhatsApp y Facturación
 * Maneja el envío de facturas por WhatsApp y notificaciones
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - Fase 1.5
 */
@Service
@Slf4j
public class WhatsAppFacturaService {
    
    private final WhatsAppService whatsAppService;
    private final FacturaRepository facturaRepository;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    public WhatsAppFacturaService(
            WhatsAppService whatsAppService,
            FacturaRepository facturaRepository) {
        this.whatsAppService = whatsAppService;
        this.facturaRepository = facturaRepository;
    }
    
    /**
     * Envía una factura por WhatsApp (PDF + mensaje)
     * 
     * @param idFactura ID de la factura
     * @return Mensaje enviado
     */
    @Transactional
    public MensajeWhatsApp enviarFacturaPorWhatsApp(Integer idFactura) {
        log.info("Enviando factura {} por WhatsApp", idFactura);
        
        // Obtener factura
        Factura factura = facturaRepository.findById(idFactura)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada: " + idFactura));
        
        // Validar que tenga cliente con usuario/teléfono
        validarClienteConTelefono(factura);
        
        // Obtener teléfono del usuario asociado al cliente
        String telefono = formatearTelefono(factura.getCliente().getUsuario().getTelefono());
        
        // Construir URL del PDF
        String urlPdf = construirUrlFacturaPdf(idFactura);
        
        // Construir nombre del archivo
        String nombreArchivo = String.format("Factura_%s.pdf", factura.getNumeroFactura());
        
        // Construir mensaje de caption
        String caption = construirMensajeFactura(factura);
        
        // Obtener ID del usuario
        Integer idUsuario = obtenerIdUsuarioDeCliente(factura);
        
        // Enviar documento
        return whatsAppService.enviarDocumento(
                telefono,
                urlPdf,
                nombreArchivo,
                caption,
                idUsuario
        );
    }
    
    /**
     * Envía un recordatorio de pago por WhatsApp
     * 
     * @param idFactura ID de la factura
     * @return Mensaje enviado
     */
    @Transactional
    public MensajeWhatsApp enviarRecordatorioPago(Integer idFactura) {
        log.info("Enviando recordatorio de pago para factura {}", idFactura);
        
        // Obtener factura
        Factura factura = facturaRepository.findById(idFactura)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada: " + idFactura));
        
        // Validar que no esté pagada
        if (factura.getFechaPago() != null) {
            throw new IllegalStateException("La factura ya está pagada");
        }
        
        // Validar cliente con teléfono
        validarClienteConTelefono(factura);
        
        String telefono = formatearTelefono(factura.getCliente().getUsuario().getTelefono());
        String mensaje = construirMensajeRecordatorio(factura);
        Integer idUsuario = obtenerIdUsuarioDeCliente(factura);
        
        // Enviar mensaje de texto
        return whatsAppService.enviarMensajeTexto(telefono, mensaje, idUsuario);
    }
    
    /**
     * Envía una notificación de factura nueva usando plantilla
     * 
     * @param idFactura ID de la factura
     * @param nombrePlantilla Nombre de la plantilla a usar
     * @return Mensaje enviado
     */
    @Transactional
    public MensajeWhatsApp enviarNotificacionFacturaNueva(Integer idFactura, String nombrePlantilla) {
        log.info("Enviando notificación de factura nueva {} con plantilla {}", idFactura, nombrePlantilla);
        
        Factura factura = facturaRepository.findById(idFactura)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada: " + idFactura));
        
        validarClienteConTelefono(factura);
        
        String telefono = formatearTelefono(factura.getCliente().getUsuario().getTelefono());
        Integer idUsuario = obtenerIdUsuarioDeCliente(factura);
        
        // Construir parámetros para la plantilla
        List<String> parametros = List.of(
                factura.getCliente().getNombre(),
                factura.getNumeroFactura() != null ? factura.getNumeroFactura() : "N/A",
                formatearMoneda(factura.getTotal()),
                formatearFecha(convertirSqlDateALocalDate(factura.getCreateDate()))
        );
        
        return whatsAppService.enviarMensajePlantilla(telefono, nombrePlantilla, parametros, idUsuario);
    }
    
    /**
     * Envía confirmación de pago por WhatsApp
     * 
     * @param idFactura ID de la factura pagada
     * @return Mensaje enviado
     */
    @Transactional
    public MensajeWhatsApp enviarConfirmacionPago(Integer idFactura) {
        log.info("Enviando confirmación de pago para factura {}", idFactura);
        
        Factura factura = facturaRepository.findById(idFactura)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada: " + idFactura));
        
        validarClienteConTelefono(factura);
        
        String telefono = formatearTelefono(factura.getCliente().getUsuario().getTelefono());
        String mensaje = construirMensajeConfirmacionPago(factura);
        Integer idUsuario = obtenerIdUsuarioDeCliente(factura);
        
        return whatsAppService.enviarMensajeTexto(telefono, mensaje, idUsuario);
    }
    
    /**
     * Obtiene facturas pendientes de un cliente
     * 
     * @param idCliente ID del cliente
     * @return Lista de facturas pendientes (sin fecha de pago)
     */
    public List<Factura> obtenerFacturasPendientes(Integer idCliente) {
        // Buscar todas las facturas del cliente y filtrar las que no tienen fecha de pago
        return facturaRepository.findByClienteIdCliente(idCliente).stream()
                .filter(f -> f.getFechaPago() == null)
                .toList();
    }
    
    /**
     * Envía resumen de cuenta por WhatsApp
     * 
     * @param idCliente ID del cliente
     * @return Mensaje enviado
     */
    @Transactional
    public MensajeWhatsApp enviarResumenCuenta(Integer idCliente) {
        log.info("Enviando resumen de cuenta para cliente {}", idCliente);
        
        List<Factura> facturasPendientes = obtenerFacturasPendientes(idCliente);
        
        if (facturasPendientes.isEmpty()) {
            throw new IllegalStateException("El cliente no tiene facturas pendientes");
        }
        
        Factura primeraFactura = facturasPendientes.get(0);
        validarClienteConTelefono(primeraFactura);
        
        String telefono = formatearTelefono(primeraFactura.getCliente().getUsuario().getTelefono());
        String mensaje = construirMensajeResumenCuenta(facturasPendientes);
        Integer idUsuario = obtenerIdUsuarioDeCliente(primeraFactura);
        
        return whatsAppService.enviarMensajeTexto(telefono, mensaje, idUsuario);
    }
    
    // ========================================
    // MÉTODOS PRIVADOS - VALIDACIONES
    // ========================================
    
    private void validarClienteConTelefono(Factura factura) {
        if (factura.getCliente() == null) {
            throw new IllegalStateException("La factura no tiene cliente asociado");
        }
        
        if (factura.getCliente().getUsuario() == null) {
            throw new IllegalStateException("El cliente no tiene usuario asociado");
        }
        
        if (factura.getCliente().getUsuario().getTelefono() == null || 
            factura.getCliente().getUsuario().getTelefono().isEmpty()) {
            throw new IllegalStateException("El usuario no tiene teléfono registrado");
        }
    }
    
    // ========================================
    // MÉTODOS PRIVADOS - CONSTRUCCIÓN DE MENSAJES
    // ========================================
    
    private String construirMensajeFactura(Factura factura) {
        return String.format(
                "🧾 *Factura %s*\n\n" +
                "📅 Fecha: %s\n" +
                "💰 Total: %s\n\n" +
                "Adjunto encontrarás tu factura en PDF.\n\n" +
                "¡Gracias por tu preferencia! 🙌",
                factura.getNumeroFactura(),
                formatearFecha(convertirSqlDateALocalDate(factura.getCreateDate())),
                formatearMoneda(factura.getTotal())
        );
    }
    
    private String construirMensajeRecordatorio(Factura factura) {
        return String.format(
                "🔔 Recordatorio de Pago\n\n" +
                "Hola %s,\n\n" +
                "Te recordamos que tienes una factura pendiente:\n\n" +
                "🧾 Factura: %s\n" +
                "💰 Monto: %s\n\n" +
                "Por favor, realiza tu pago a la brevedad.\n\n" +
                "¿Necesitas ayuda? Contáctanos. 📞",
                factura.getCliente().getNombre(),
                factura.getNumeroFactura(),
                formatearMoneda(factura.getTotal())
        );
    }
    
    private String construirMensajeConfirmacionPago(Factura factura) {
        return String.format(
                "✅ *Pago Confirmado*\n\n" +
                "Hola %s,\n\n" +
                "Hemos recibido el pago de tu factura:\n\n" +
                "🧾 Factura: %s\n" +
                "💰 Monto: %s\n" +
                "📅 Fecha de pago: %s\n\n" +
                "¡Gracias por tu pago! 🎉",
                factura.getCliente().getNombre(),
                factura.getNumeroFactura(),
                formatearMoneda(factura.getTotal()),
                formatearFecha(convertirSqlDateALocalDate(factura.getFechaPago()))
        );
    }
    
    private String construirMensajeResumenCuenta(List<Factura> facturas) {
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("📊 *Resumen de Cuenta*\n\n");
        mensaje.append(String.format("Hola %s,\n\n", facturas.get(0).getCliente().getNombre()));
        mensaje.append(String.format("Tienes %d factura(s) pendiente(s):\n\n", facturas.size()));
        
        BigDecimal totalPendiente = BigDecimal.ZERO;
        
        for (int i = 0; i < facturas.size() && i < 5; i++) {
            Factura f = facturas.get(i);
            mensaje.append(String.format(
                    "%d. %s - %s\n",
                    i + 1,
                    f.getNumeroFactura(),
                    formatearMoneda(f.getTotal())
            ));
            totalPendiente = totalPendiente.add(f.getTotal());
        }
        
        if (facturas.size() > 5) {
            mensaje.append(String.format("\n... y %d más\n", facturas.size() - 5));
        }
        
        mensaje.append(String.format("\n💰 *Total pendiente: %s*\n\n", formatearMoneda(totalPendiente)));
        mensaje.append("Por favor, ponte al corriente con tus pagos. 📞");
        
        return mensaje.toString();
    }
    
    // ========================================
    // MÉTODOS PRIVADOS - UTILIDADES
    // ========================================
    
    private String construirUrlFacturaPdf(Integer idFactura) {
        return String.format("%s/api/facturas/%d/pdf", baseUrl, idFactura);
    }
    
    private String formatearTelefono(String telefono) {
        String limpio = telefono.replaceAll("[^0-9+]", "");
        
        if (!limpio.startsWith("+")) {
            if (limpio.length() == 10) {
                limpio = "+52" + limpio;
            } else {
                limpio = "+" + limpio;
            }
        }
        
        return limpio;
    }
    
    private LocalDate convertirSqlDateALocalDate(Object fecha) {
        if (fecha == null) return LocalDate.now();
        
        if (fecha instanceof Date) {
            return ((Date) fecha).toLocalDate();
        }
        
        if (fecha instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) fecha).toLocalDateTime().toLocalDate();
        }
        
        return LocalDate.now();
    }
    
    private String formatearFecha(LocalDate fecha) {
        if (fecha == null) return "N/A";
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
    
    private String formatearMoneda(BigDecimal monto) {
        if (monto == null) return "$0.00";
        return String.format("$%,.2f", monto);
    }
    
    private Integer obtenerIdUsuarioDeCliente(Factura factura) {
        if (factura.getCliente() != null && factura.getCliente().getUsuario() != null) {
            return factura.getCliente().getUsuario().getIdUsuario();
        }
        return null;
    }
}
