package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión del plan de cuentas contables.
 * Maneja la lógica de negocio para cuentas contables jerárquicas.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CuentaContableService {
    
    private final CuentaContableRepository cuentaRepository;
    private final DetalleAsientoRepository detalleRepository;
    
    /**
     * Obtiene todas las cuentas contables.
     * @return Lista de todas las cuentas
     */
    public List<CuentaContable> obtenerTodas() {
        log.debug("Obteniendo todas las cuentas contables");
        return cuentaRepository.findAll();
    }
    
    /**
     * Obtiene todas las cuentas activas.
     * @return Lista de cuentas activas
     */
    public List<CuentaContable> obtenerActivas() {
        log.debug("Obteniendo cuentas activas");
        return cuentaRepository.findByActiva(true);
    }
    
    /**
     * Busca una cuenta por ID.
     * @param id ID de la cuenta
     * @return Cuenta si existe
     */
    public Optional<CuentaContable> obtenerPorId(Long id) {
        log.debug("Buscando cuenta con ID: {}", id);
        return cuentaRepository.findById(id);
    }
    
    /**
     * Busca una cuenta por código.
     * @param codigo Código de la cuenta
     * @return Cuenta si existe
     */
    public Optional<CuentaContable> obtenerPorCodigo(String codigo) {
        log.debug("Buscando cuenta con código: {}", codigo);
        return cuentaRepository.findByCodigo(codigo);
    }
    
    /**
     * Obtiene las cuentas raíz (nivel 1).
     * @return Lista de cuentas raíz
     */
    public List<CuentaContable> obtenerCuentasRaiz() {
        log.debug("Obteniendo cuentas raíz");
        return cuentaRepository.findCuentasRaiz();
    }
    
    /**
     * Obtiene las subcuentas de una cuenta padre.
     * @param cuentaPadreId ID de la cuenta padre
     * @return Lista de subcuentas
     */
    public List<CuentaContable> obtenerSubcuentas(Long cuentaPadreId) {
        log.debug("Obteniendo subcuentas de cuenta ID: {}", cuentaPadreId);
        return cuentaRepository.findSubcuentasByCuentaPadreId(cuentaPadreId);
    }
    
    /**
     * Obtiene cuentas por tipo.
     * @param tipo Tipo de cuenta
     * @return Lista de cuentas del tipo
     */
    public List<CuentaContable> obtenerPorTipo(TipoCuenta tipo) {
        log.debug("Obteniendo cuentas de tipo: {}", tipo);
        return cuentaRepository.findByTipo(tipo);
    }
    
    /**
     * Obtiene cuentas de balance (Activo, Pasivo, Capital).
     * @return Lista de cuentas de balance
     */
    public List<CuentaContable> obtenerCuentasDeBalance() {
        log.debug("Obteniendo cuentas de balance");
        return cuentaRepository.findCuentasDeBalance();
    }
    
    /**
     * Obtiene cuentas de resultados (Ingresos, Egresos).
     * @return Lista de cuentas de resultados
     */
    public List<CuentaContable> obtenerCuentasDeResultados() {
        log.debug("Obteniendo cuentas de resultados");
        return cuentaRepository.findCuentasDeResultados();
    }
    
    /**
     * Obtiene cuentas que pueden recibir movimientos.
     * @return Lista de cuentas operativas
     */
    public List<CuentaContable> obtenerCuentasOperativas() {
        log.debug("Obteniendo cuentas operativas");
        return cuentaRepository.findByAceptaMovimientos(true);
    }
    
    /**
     * Busca cuentas por código o nombre.
     * @param termino Término de búsqueda
     * @return Lista de cuentas que coinciden
     */
    public List<CuentaContable> buscarCuentas(String termino) {
        log.debug("Buscando cuentas con término: {}", termino);
        return cuentaRepository.buscarCuentas(termino);
    }
    
    /**
     * Crea una nueva cuenta contable.
     * @param cuenta Cuenta a crear
     * @return Cuenta creada
     * @throws IllegalArgumentException si el código ya existe o es inválido
     */
    @Transactional
    public CuentaContable crear(CuentaContable cuenta) {
        log.info("Creando nueva cuenta: {}", cuenta.getCodigo());
        
        // Validar que el código no exista
        if (cuentaRepository.existsByCodigo(cuenta.getCodigo())) {
            throw new IllegalArgumentException("Ya existe una cuenta con el código: " + cuenta.getCodigo());
        }
        
        // Si tiene cuenta padre, validar y agregar
        if (cuenta.getCuentaPadre() != null) {
            CuentaContable padre = cuentaRepository.findById(cuenta.getCuentaPadre().getIdCuenta())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta padre no encontrada"));
            
            // Validar que el código sea consistente con la jerarquía
            if (!cuenta.getCodigo().startsWith(padre.getCodigo())) {
                throw new IllegalArgumentException(
                    "El código de la subcuenta debe comenzar con el código de la cuenta padre"
                );
            }
            
            padre.agregarSubcuenta(cuenta);
        }
        
        CuentaContable guardada = cuentaRepository.save(cuenta);
        log.info("Cuenta creada exitosamente: {} - {}", guardada.getCodigo(), guardada.getNombre());
        
        return guardada;
    }
    
    /**
     * Actualiza una cuenta contable.
     * @param id ID de la cuenta
     * @param cuentaActualizada Datos actualizados
     * @return Cuenta actualizada
     * @throws IllegalArgumentException si la cuenta no existe o tiene movimientos
     */
    @Transactional
    public CuentaContable actualizar(Long id, CuentaContable cuentaActualizada) {
        log.info("Actualizando cuenta ID: {}", id);
        
        CuentaContable cuenta = cuentaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada"));
        
        // No permitir cambiar código si tiene movimientos
        if (!cuenta.getCodigo().equals(cuentaActualizada.getCodigo())) {
            if (tieneMovimientos(id)) {
                throw new IllegalArgumentException(
                    "No se puede cambiar el código de una cuenta con movimientos contables"
                );
            }
            
            // Validar que el nuevo código no exista
            if (cuentaRepository.existsByCodigo(cuentaActualizada.getCodigo())) {
                throw new IllegalArgumentException(
                    "Ya existe una cuenta con el código: " + cuentaActualizada.getCodigo()
                );
            }
        }
        
        // Actualizar campos permitidos
        cuenta.setNombre(cuentaActualizada.getNombre());
        cuenta.setDescripcion(cuentaActualizada.getDescripcion());
        cuenta.setActiva(cuentaActualizada.getActiva());
        cuenta.setAceptaMovimientos(cuentaActualizada.getAceptaMovimientos());
        
        CuentaContable guardada = cuentaRepository.save(cuenta);
        log.info("Cuenta actualizada: {} - {}", guardada.getCodigo(), guardada.getNombre());
        
        return guardada;
    }
    
    /**
     * Activa o desactiva una cuenta.
     * @param id ID de la cuenta
     * @param activa True para activar, false para desactivar
     * @return Cuenta actualizada
     */
    @Transactional
    public CuentaContable cambiarEstado(Long id, boolean activa) {
        log.info("Cambiando estado de cuenta ID: {} a {}", id, activa ? "activa" : "inactiva");
        
        CuentaContable cuenta = cuentaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada"));
        
        cuenta.setActiva(activa);
        
        CuentaContable guardada = cuentaRepository.save(cuenta);
        log.info("Estado cambiado para cuenta: {}", guardada.getCodigo());
        
        return guardada;
    }
    
    /**
     * Elimina una cuenta contable.
     * @param id ID de la cuenta
     * @throws IllegalArgumentException si tiene movimientos o subcuentas
     */
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando cuenta ID: {}", id);
        
        CuentaContable cuenta = cuentaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada"));
        
        // Validar que no tenga subcuentas
        Long subcuentas = cuentaRepository.countSubcuentas(id);
        if (subcuentas > 0) {
            throw new IllegalArgumentException(
                "No se puede eliminar una cuenta con " + subcuentas + " subcuenta(s)"
            );
        }
        
        // Validar que no tenga movimientos
        if (tieneMovimientos(id)) {
            throw new IllegalArgumentException(
                "No se puede eliminar una cuenta con movimientos contables"
            );
        }
        
        cuentaRepository.delete(cuenta);
        log.info("Cuenta eliminada: {}", cuenta.getCodigo());
    }
    
    /**
     * Verifica si una cuenta tiene movimientos contables.
     * @param cuentaId ID de la cuenta
     * @return true si tiene movimientos
     */
    public boolean tieneMovimientos(Long cuentaId) {
        Long movimientos = detalleRepository.contarMovimientosCuenta(cuentaId);
        return movimientos > 0;
    }
    
    /**
     * Valida si un código de cuenta es válido.
     * @param codigo Código a validar
     * @return true si es válido
     */
    public boolean esCodigoValido(String codigo) {
        return codigo != null && codigo.matches("^[0-9]{1,10}(\\.[0-9]{1,10})*$");
    }
    
    /**
     * Calcula el nivel de una cuenta según su código.
     * @param codigo Código de la cuenta
     * @return Nivel (cantidad de puntos + 1)
     */
    public Integer calcularNivel(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            return 0;
        }
        return codigo.split("\\.").length;
    }
    
    /**
     * Genera el siguiente código disponible para una cuenta padre.
     * @param codigoPadre Código de la cuenta padre (null para nivel raíz)
     * @return Siguiente código disponible
     */
    public String generarSiguienteCodigo(String codigoPadre) {
        List<CuentaContable> hermanas;
        
        if (codigoPadre == null || codigoPadre.isEmpty()) {
            // Cuentas raíz
            hermanas = cuentaRepository.findCuentasRaiz();
        } else {
            // Subcuentas
            hermanas = cuentaRepository.findByCodigoStartingWith(codigoPadre + ".");
        }
        
        if (hermanas.isEmpty()) {
            return codigoPadre == null ? "1" : codigoPadre + ".1";
        }
        
        // Encontrar el número más alto
        int maxNumero = hermanas.stream()
            .map(c -> {
                String codigo = c.getCodigo();
                String ultimaParte = codigo.substring(codigo.lastIndexOf('.') + 1);
                try {
                    return Integer.parseInt(ultimaParte);
                } catch (NumberFormatException e) {
                    return 0;
                }
            })
            .max(Integer::compareTo)
            .orElse(0);
        
        return codigoPadre == null ? String.valueOf(maxNumero + 1) : codigoPadre + "." + (maxNumero + 1);
    }
    
    /**
     * Obtiene la ruta completa de una cuenta (incluyendo padres).
     * @param cuentaId ID de la cuenta
     * @return Lista de cuentas desde raíz hasta la cuenta
     */
    public List<CuentaContable> obtenerRutaCuenta(Long cuentaId) {
        log.debug("Obteniendo ruta de cuenta ID: {}", cuentaId);
        
        CuentaContable cuenta = cuentaRepository.findById(cuentaId)
            .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada"));
        
        List<CuentaContable> ruta = new java.util.ArrayList<>();
        ruta.add(cuenta);
        
        CuentaContable padre = cuenta.getCuentaPadre();
        while (padre != null) {
            ruta.add(0, padre);
            padre = padre.getCuentaPadre();
        }
        
        return ruta;
    }
}
