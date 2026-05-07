## 📝 SISTEMA DE PLANTILLAS

### PlantillaService

**Ubicación:** `src/main/java/api/whats_orders_manager/service/PlantillaService.java`

```java
@Service
@Transactional
public class PlantillaService {

    private final PlantillaWhatsAppRepository plantillaRepository;
    
    /**
     * Obtiene una plantilla por código
     */
    public PlantillaWhatsApp obtenerPorCodigo(String codigo) {
        return plantillaRepository.findByCodigo(codigo)
            .orElseThrow(() -> new EntityNotFoundException(
                "Plantilla no encontrada: " + codigo
            ));
    }
    
    /**
     * Reemplaza variables en el contenido de la plantilla
     */
    public String procesarPlantilla(String codigo, Map<String, String> variables) {
        PlantillaWhatsApp plantilla = obtenerPorCodigo(codigo);
        
        if (!plantilla.getActiva()) {
            throw new IllegalStateException("Plantilla inactiva: " + codigo);
        }
        
        String contenido = plantilla.getContenido();
        
        // Reemplazar cada variable
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            contenido = contenido.replace(placeholder, entry.getValue());
        }
        
        // Validar que no queden variables sin reemplazar
        if (contenido.contains("{") && contenido.contains("}")) {
            log.warn("Plantilla {} tiene variables sin reemplazar: {}", codigo, contenido);
        }
        
        return contenido;
    }
    
    /**
     * Lista todas las plantillas activas
     */
    public List<PlantillaWhatsApp> listarActivas() {
        return plantillaRepository.findByActivaTrue();
    }
    
    /**
     * Crea o actualiza una plantilla
     */
    public PlantillaWhatsApp guardar(PlantillaWhatsAppDTO dto) {
        PlantillaWhatsApp plantilla;
        
        if (dto.getId() != null) {
            plantilla = plantillaRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException("Plantilla no encontrada"));
        } else {
            plantilla = new PlantillaWhatsApp();
        }
        
        plantilla.setCodigo(dto.getCodigo());
        plantilla.setNombre(dto.getNombre());
        plantilla.setContenido(dto.getContenido());
        plantilla.setDescripcion(dto.getDescripcion());
        plantilla.setVariablesDisponibles(dto.getVariablesDisponibles());
        plantilla.setActiva(dto.getActiva());
        plantilla.setTipoNotificacion(dto.getTipoNotificacion());
        
        return plantillaRepository.save(plantilla);
    }
    
    /**
     * Elimina una plantilla
     */
    public void eliminar(Long id) {
        PlantillaWhatsApp plantilla = plantillaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Plantilla no encontrada"));
        
        plantillaRepository.delete(plantilla);
        log.info("Plantilla eliminada: {}", plantilla.getCodigo());
    }
}
```

### Plantillas Predefinidas

```sql
-- Datos iniciales de plantillas WhatsApp
INSERT INTO plantillas_whatsapp (codigo, nombre, contenido, variables_disponibles, activa, tipo_notificacion) VALUES
('FACTURA_NUEVA', 'Nueva Factura Emitida', 
 'Hola {nombre}, hemos emitido la factura #{numero} por un total de {total}€. Vence el {fecha_vencimiento}.', 
 '["nombre", "numero", "total", "fecha_vencimiento"]', 
 true, 'FACTURA_NUEVA'),

('FACTURA_RECORDATORIO', 'Recordatorio de Pago',
 'Hola {nombre}, te recordamos que la factura #{numero} por {total}€ vence el {fecha_vencimiento}. ¡Gracias!',
 '["nombre", "numero", "total", "fecha_vencimiento"]',
 true, 'FACTURA_RECORDATORIO'),

('FACTURA_VENCIDA', 'Factura Vencida',
 'Hola {nombre}, la factura #{numero} por {total}€ ha vencido. Por favor, procede con el pago lo antes posible.',
 '["nombre", "numero", "total", "fecha_vencimiento"]',
 true, 'FACTURA_VENCIDA'),

('FACTURA_PAGADA', 'Confirmación de Pago',
 '¡Pago recibido! Hola {nombre}, confirmamos el pago de la factura #{numero} por {total}€. ¡Gracias por tu preferencia!',
 '["nombre", "numero", "total", "fecha_pago"]',
 true, 'FACTURA_PAGADA'),

('PEDIDO_CONFIRMADO', 'Pedido Confirmado',
 'Hola {nombre}, tu pedido #{numero} ha sido confirmado. Total: {total}€. Fecha estimada de entrega: {fecha_entrega}.',
 '["nombre", "numero", "total", "fecha_entrega"]',
 true, 'PEDIDO_CONFIRMADO'),

('PEDIDO_ENVIADO', 'Pedido Enviado',
 'Hola {nombre}, tu pedido #{numero} ha sido enviado. Número de seguimiento: {tracking}.',
 '["nombre", "numero", "tracking"]',
 true, 'PEDIDO_ENVIADO'),

('USUARIO_BIENVENIDA', 'Bienvenida',
 '¡Bienvenido {nombre}! Tu cuenta ha sido creada exitosamente. Usuario: {email}',
 '["nombre", "email"]',
 true, 'USUARIO_NUEVO'),

('PASSWORD_RESET', 'Restablecimiento de Contraseña',
 'Hola {nombre}, hemos recibido una solicitud de restablecimiento de contraseña. Código: {codigo}. Válido por 15 minutos.',
 '["nombre", "codigo"]',
 true, 'PASSWORD_RESET');
```

---

