package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Servicio para generar números consecutivos de pagos.
 * Formato: PAG-YYYYMMDD-NNNN
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NumeroPagoGeneratorService {
    
    private final PagoRepository pagoRepository;
    
    private static final String PREFIJO = "PAG-";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    /**
     * Genera el siguiente número de pago disponible para la fecha actual.
     * Formato: PAG-YYYYMMDD-NNNN
     * 
     * @return Número de pago único
     */
    @Transactional(readOnly = true)
    public synchronized String generarNumeroPago() {
        return generarNumeroPago(LocalDate.now());
    }
    
    /**
     * Genera el siguiente número de pago disponible para una fecha específica.
     * Formato: PAG-YYYYMMDD-NNNN
     * 
     * @param fecha Fecha para la cual generar el número
     * @return Número de pago único
     */
    @Transactional(readOnly = true)
    public synchronized String generarNumeroPago(LocalDate fecha) {
        String fechaStr = fecha.format(FORMATO_FECHA);
        String patron = PREFIJO + fechaStr + "-%";
        
        // Buscar el último número de pago del día
        String ultimoNumero = pagoRepository.findUltimoNumeroPagoPorPatron(patron);
        
        int consecutivo = 1;
        
        if (ultimoNumero != null && !ultimoNumero.isEmpty()) {
            try {
                // Extraer el consecutivo del último número: PAG-20260119-0001 -> 0001
                String[] partes = ultimoNumero.split("-");
                if (partes.length == 3) {
                    consecutivo = Integer.parseInt(partes[2]) + 1;
                }
            } catch (NumberFormatException e) {
                log.warn("Error al parsear número de pago: {}. Iniciando desde 1", ultimoNumero);
                consecutivo = 1;
            }
        }
        
        String numeroPago = String.format("%s%s-%04d", PREFIJO, fechaStr, consecutivo);
        log.debug("Número de pago generado: {}", numeroPago);
        
        return numeroPago;
    }
    
    /**
     * Valida que un número de pago cumpla con el formato esperado.
     * 
     * @param numeroPago Número a validar
     * @return true si el formato es válido
     */
    public boolean validarFormato(String numeroPago) {
        if (numeroPago == null || numeroPago.isEmpty()) {
            return false;
        }
        
        // Patrón: PAG-YYYYMMDD-NNNN
        String patron = "^PAG-\\d{8}-\\d{4}$";
        return numeroPago.matches(patron);
    }
    
    /**
     * Extrae la fecha de un número de pago.
     * 
     * @param numeroPago Número de pago
     * @return Fecha extraída o null si el formato es inválido
     */
    public LocalDate extraerFecha(String numeroPago) {
        if (!validarFormato(numeroPago)) {
            return null;
        }
        
        try {
            String[] partes = numeroPago.split("-");
            String fechaStr = partes[1];
            return LocalDate.parse(fechaStr, FORMATO_FECHA);
        } catch (Exception e) {
            log.error("Error al extraer fecha del número de pago: {}", numeroPago, e);
            return null;
        }
    }
    
    /**
     * Extrae el consecutivo de un número de pago.
     * 
     * @param numeroPago Número de pago
     * @return Consecutivo o 0 si el formato es inválido
     */
    public int extraerConsecutivo(String numeroPago) {
        if (!validarFormato(numeroPago)) {
            return 0;
        }
        
        try {
            String[] partes = numeroPago.split("-");
            return Integer.parseInt(partes[2]);
        } catch (Exception e) {
            log.error("Error al extraer consecutivo del número de pago: {}", numeroPago, e);
            return 0;
        }
    }
}
