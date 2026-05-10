## 🔔 NOTIFICACIONES MULTICANAL

### NotificacionService (Orquestador)

**Ubicación:** `src/main/java/api/whats_orders_manager/service/NotificacionService.java`

```java
@Service
@Transactional
@Slf4j
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final WhatsAppService whatsAppService;
    private final EmailService emailService;
    private final NotificacionWebSocketController webSocketController;
    private final PlantillaService plantillaService;
    private final PreferenciaNotificacionService preferenciaService;
    
    /**
     * Método principal para enviar notificaciones multicanal
     */
    public void notificar(TipoNotificacion tipo, Usuario usuario, Map<String, String> datos) {
        log.info("Iniciando notificación {} para usuario {}", tipo, usuario.getEmail());
        
        // Obtener preferencias del usuario
        PreferenciaNotificacion preferencias = preferenciaService.obtenerPreferencias(usuario);
        
        // Enviar por cada canal habilitado
        if (preferencias.getNotificacionesWeb()) {
            enviarNotificacionWeb(tipo, usuario, datos);
        }
        
        if (preferencias.getNotificacionesEmail()) {
            enviarNotificacionEmail(tipo, usuario, datos);
        }
        
        if (preferencias.getNotificacionesWhatsApp() && preferencias.getValidadoTelefono()) {
            enviarNotificacionWhatsApp(tipo, usuario, datos, preferencias.getTelefono());
        }
        
        log.info("Notificaciones enviadas exitosamente");
    }
    
    /**
     * Envío de notificación web (WebSocket + BD)
     */
    private void enviarNotificacionWeb(TipoNotificacion tipo, Usuario usuario, 
                                      Map<String, String> datos) {
        try {
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuario(usuario);
            notificacion.setTipo(tipo);
            notificacion.setCanal(CanalNotificacion.WEB);
            notificacion.setTitulo(generarTitulo(tipo));
            notificacion.setMensaje(generarMensajeWeb(tipo, datos));
            notificacion.setEstado(EstadoNotificacion.ENVIADO);
            notificacion.setEnviadoEn(LocalDateTime.now());
            
            // Referencias
            if (datos.containsKey("facturaId")) {
                notificacion.setFacturaId(Long.parseLong(datos.get("facturaId")));
            }
            if (datos.containsKey("pedidoId")) {
                notificacion.setPedidoId(Long.parseLong(datos.get("pedidoId")));
            }
            
            notificacionRepository.save(notificacion);
            
            // Enviar por WebSocket
            webSocketController.enviarNotificacion(usuario.getId(), notificacion);
            
            log.info("Notificación web enviada a usuario {}", usuario.getId());
            
        } catch (Exception e) {
            log.error("Error al enviar notificación web", e);
        }
    }
    
    /**
     * Envío de notificación por email
     */
    private void enviarNotificacionEmail(TipoNotificacion tipo, Usuario usuario,
                                        Map<String, String> datos) {
        try {
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuario(usuario);
            notificacion.setTipo(tipo);
            notificacion.setCanal(CanalNotificacion.EMAIL);
            notificacion.setTitulo(generarTitulo(tipo));
            notificacion.setMensaje(generarMensajeEmail(tipo, datos));
            notificacion.setEstado(EstadoNotificacion.ENVIANDO);
            
            notificacionRepository.save(notificacion);
            
            // Enviar email
            emailService.enviarEmail(
                usuario.getEmail(),
                notificacion.getTitulo(),
                notificacion.getMensaje()
            );
            
            // Actualizar estado
            notificacion.setEstado(EstadoNotificacion.ENVIADO);
            notificacion.setEnviadoEn(LocalDateTime.now());
            notificacionRepository.save(notificacion);
            
            log.info("Email enviado a {}", usuario.getEmail());
            
        } catch (Exception e) {
            log.error("Error al enviar email", e);
            
            // Marcar como fallido
            Notificacion notificacion = notificacionRepository.findById(
                notificacion.getId()
            ).orElse(null);
            
            if (notificacion != null) {
                notificacion.setEstado(EstadoNotificacion.FALLIDO);
                notificacion.setErrorMensaje(e.getMessage());
                notificacionRepository.save(notificacion);
            }
        }
    }
    
    /**
     * Envío de notificación por WhatsApp
     */
    private void enviarNotificacionWhatsApp(TipoNotificacion tipo, Usuario usuario,
                                           Map<String, String> datos, String telefono) {
        try {
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuario(usuario);
            notificacion.setTipo(tipo);
            notificacion.setCanal(CanalNotificacion.WHATSAPP);
            notificacion.setTitulo(generarTitulo(tipo));
            notificacion.setEstado(EstadoNotificacion.ENVIANDO);
            
            notificacionRepository.save(notificacion);
            
            // Procesar plantilla
            String codigoPlantilla = tipo.name();
            String mensaje = plantillaService.procesarPlantilla(codigoPlantilla, datos);
            
            notificacion.setMensaje(mensaje);
            
            // Enviar por WhatsApp
            String messageId = whatsAppService.enviarMensaje(telefono, mensaje);
            
            // Actualizar estado
            notificacion.setWhatsappMessageId(messageId);
            notificacion.setWhatsappEstado("sent");
            notificacion.setEstado(EstadoNotificacion.ENVIADO);
            notificacion.setEnviadoEn(LocalDateTime.now());
            notificacionRepository.save(notificacion);
            
            log.info("WhatsApp enviado a {} (ID: {})", telefono, messageId);
            
        } catch (Exception e) {
            log.error("Error al enviar WhatsApp", e);
            
            // Marcar como fallido
            Notificacion notificacion = notificacionRepository.findById(
                notificacion.getId()
            ).orElse(null);
            
            if (notificacion != null) {
                notificacion.setEstado(EstadoNotificacion.FALLIDO);
                notificacion.setErrorMensaje(e.getMessage());
                notificacionRepository.save(notificacion);
            }
        }
    }
    
    /**
     * Actualiza el estado de un mensaje de WhatsApp (desde webhook)
     */
    public void actualizarEstadoWhatsApp(String messageId, String estado) {
        Optional<Notificacion> optNotificacion = notificacionRepository
            .findByWhatsappMessageId(messageId);
        
        if (optNotificacion.isEmpty()) {
            log.warn("Notificación no encontrada para mensaje WhatsApp: {}", messageId);
            return;
        }
        
        Notificacion notificacion = optNotificacion.get();
        notificacion.setWhatsappEstado(estado);
        
        switch (estado) {
            case "delivered":
                notificacion.setEstado(EstadoNotificacion.ENTREGADO);
                notificacion.setEntregadoEn(LocalDateTime.now());
                break;
            case "read":
                notificacion.setEstado(EstadoNotificacion.LEIDO);
                notificacion.setLeidoEn(LocalDateTime.now());
                notificacion.setLeida(true);
                break;
            case "failed":
                notificacion.setEstado(EstadoNotificacion.FALLIDO);
                break;
        }
        
        notificacionRepository.save(notificacion);
        log.info("Estado de notificación actualizado: {} -> {}", messageId, estado);
    }
    
    private String generarTitulo(TipoNotificacion tipo) {
        return switch (tipo) {
            case FACTURA_NUEVA -> "Nueva Factura Emitida";
            case FACTURA_PAGADA -> "Pago Confirmado";
            case FACTURA_VENCIDA -> "Factura Vencida";
            case FACTURA_RECORDATORIO -> "Recordatorio de Pago";
            case PEDIDO_CONFIRMADO -> "Pedido Confirmado";
            case PEDIDO_ENVIADO -> "Pedido Enviado";
            case USUARIO_NUEVO -> "Bienvenido";
            case PASSWORD_RESET -> "Restablecer Contraseña";
            default -> "Notificación";
        };
    }
    
    private String generarMensajeWeb(TipoNotificacion tipo, Map<String, String> datos) {
        // Generar mensaje simple para notificaciones web
        return String.format("Tienes una nueva notificación: %s", generarTitulo(tipo));
    }
    
    private String generarMensajeEmail(TipoNotificacion tipo, Map<String, String> datos) {
        // Generar HTML para emails (puede usar templates más elaborados)
        return String.format("<h2>%s</h2><p>Detalles: %s</p>", 
            generarTitulo(tipo), datos.toString());
    }
}
```

---

