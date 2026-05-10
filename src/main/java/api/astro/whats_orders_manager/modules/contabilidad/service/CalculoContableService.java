package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para cálculos contables y reportes financieros.
 * Genera balances, estados de resultados y libros contables.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CalculoContableService {
    
    private final CuentaContableRepository cuentaRepository;
    private final DetalleAsientoRepository detalleRepository;
    
    /**
     * DTO para saldo de cuenta.
     */
    public static class SaldoCuenta {
        private final CuentaContable cuenta;
        private final BigDecimal debe;
        private final BigDecimal haber;
        private final BigDecimal saldo;
        
        public SaldoCuenta(CuentaContable cuenta, BigDecimal debe, BigDecimal haber) {
            this.cuenta = cuenta;
            this.debe = debe != null ? debe : BigDecimal.ZERO;
            this.haber = haber != null ? haber : BigDecimal.ZERO;
            this.saldo = this.debe.subtract(this.haber);
        }
        
        public CuentaContable getCuenta() { return cuenta; }
        public BigDecimal getDebe() { return debe; }
        public BigDecimal getHaber() { return haber; }
        public BigDecimal getSaldo() { return saldo; }
        
        public BigDecimal getSaldoDeudor() {
            return saldo.compareTo(BigDecimal.ZERO) > 0 ? saldo : BigDecimal.ZERO;
        }
        
        public BigDecimal getSaldoAcreedor() {
            return saldo.compareTo(BigDecimal.ZERO) < 0 ? saldo.abs() : BigDecimal.ZERO;
        }
    }
    
    /**
     * DTO para movimiento de cuenta.
     */
    public static class MovimientoCuenta {
        private final LocalDate fecha;
        private final String asientoNumero;
        private final String descripcion;
        private final BigDecimal debe;
        private final BigDecimal haber;
        private final BigDecimal saldo;
        
        public MovimientoCuenta(LocalDate fecha, String asientoNumero, String descripcion,
                               BigDecimal debe, BigDecimal haber, BigDecimal saldo) {
            this.fecha = fecha;
            this.asientoNumero = asientoNumero;
            this.descripcion = descripcion;
            this.debe = debe;
            this.haber = haber;
            this.saldo = saldo;
        }
        
        public LocalDate getFecha() { return fecha; }
        public String getAsientoNumero() { return asientoNumero; }
        public String getDescripcion() { return descripcion; }
        public BigDecimal getDebe() { return debe; }
        public BigDecimal getHaber() { return haber; }
        public BigDecimal getSaldo() { return saldo; }
    }
    
    /**
     * Calcula el saldo de una cuenta hasta una fecha.
     * @param cuentaId ID de la cuenta
     * @param fecha Fecha límite
     * @return Saldo calculado
     */
    public BigDecimal calcularSaldoCuenta(Long cuentaId, LocalDate fecha) {
        log.debug("Calculando saldo de cuenta {} hasta {}", cuentaId, fecha);
        BigDecimal saldo = detalleRepository.calcularSaldoCuentaHastaFecha(cuentaId, fecha);
        return saldo != null ? saldo : BigDecimal.ZERO;
    }
    
    /**
     * Calcula el saldo de una cuenta en un período.
     * @param cuentaId ID de la cuenta
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Saldo del período
     */
    public BigDecimal calcularSaldoCuentaPeriodo(Long cuentaId, LocalDate fechaInicio, LocalDate fechaFin) {
        log.debug("Calculando saldo de cuenta {} entre {} y {}", cuentaId, fechaInicio, fechaFin);
        BigDecimal saldo = detalleRepository.calcularSaldoCuentaPeriodo(cuentaId, fechaInicio, fechaFin);
        return saldo != null ? saldo : BigDecimal.ZERO;
    }
    
    /**
     * Obtiene el libro mayor de una cuenta.
     * @param cuentaId ID de la cuenta
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de movimientos con saldos acumulados
     */
    public List<MovimientoCuenta> obtenerLibroMayor(Long cuentaId, LocalDate fechaInicio, LocalDate fechaFin) {
        log.info("Generando libro mayor de cuenta {} entre {} y {}", cuentaId, fechaInicio, fechaFin);
        
        // Saldo inicial (antes del período)
        BigDecimal saldoInicial = BigDecimal.ZERO;
        if (fechaInicio != null) {
            LocalDate diaAnterior = fechaInicio.minusDays(1);
            saldoInicial = calcularSaldoCuenta(cuentaId, diaAnterior);
        }
        
        // Movimientos del período
        List<DetalleAsiento> detalles = fechaInicio != null && fechaFin != null
            ? detalleRepository.obtenerLibroMayorCuentaPeriodo(cuentaId, fechaInicio, fechaFin)
            : detalleRepository.obtenerLibroMayorCuenta(cuentaId);
        
        List<MovimientoCuenta> movimientos = new ArrayList<>();
        BigDecimal saldoAcumulado = saldoInicial;
        
        // Agregar saldo inicial si hay movimientos
        if (!detalles.isEmpty() && saldoInicial.compareTo(BigDecimal.ZERO) != 0) {
            movimientos.add(new MovimientoCuenta(
                fechaInicio != null ? fechaInicio : detalles.get(0).getAsiento().getFecha(),
                "",
                "Saldo Inicial",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                saldoInicial
            ));
        }
        
        // Procesar cada movimiento
        for (DetalleAsiento detalle : detalles) {
            saldoAcumulado = saldoAcumulado.add(detalle.getDebe()).subtract(detalle.getHaber());
            
            movimientos.add(new MovimientoCuenta(
                detalle.getAsiento().getFecha(),
                detalle.getAsiento().getNumero(),
                detalle.getDescripcion(),
                detalle.getDebe(),
                detalle.getHaber(),
                saldoAcumulado
            ));
        }
        
        return movimientos;
    }
    
    /**
     * Genera el balance de comprobación de un período.
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Mapa de cuentas con sus saldos
     */
    public List<SaldoCuenta> generarBalanceComprobacion(LocalDate fechaInicio, LocalDate fechaFin) {
        log.info("Generando balance de comprobación entre {} y {}", fechaInicio, fechaFin);
        
        List<Object[]> resultados = detalleRepository.obtenerBalanceComprobacion(fechaInicio, fechaFin);
        List<SaldoCuenta> saldos = new ArrayList<>();
        
        for (Object[] resultado : resultados) {
            Long cuentaId = (Long) resultado[0];
            BigDecimal totalDebe = (BigDecimal) resultado[1];
            BigDecimal totalHaber = (BigDecimal) resultado[2];
            
            CuentaContable cuenta = cuentaRepository.findById(cuentaId).orElse(null);
            if (cuenta != null) {
                saldos.add(new SaldoCuenta(cuenta, totalDebe, totalHaber));
            }
        }
        
        // Ordenar por código de cuenta
        saldos.sort(Comparator.comparing(s -> s.getCuenta().getCodigo()));
        
        return saldos;
    }
    
    /**
     * Genera el balance general (balance sheet) a una fecha.
     * @param fecha Fecha del balance
     * @return Mapa con activos, pasivos y capital
     */
    public Map<String, Object> generarBalanceGeneral(LocalDate fecha) {
        log.info("Generando balance general al {}", fecha);
        
        Map<String, Object> balance = new HashMap<>();
        
        // Activos
        List<SaldoCuenta> activos = calcularSaldosPorTipo(TipoCuenta.ACTIVO, fecha);
        BigDecimal totalActivos = activos.stream()
            .map(SaldoCuenta::getSaldo)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Pasivos
        List<SaldoCuenta> pasivos = calcularSaldosPorTipo(TipoCuenta.PASIVO, fecha);
        BigDecimal totalPasivos = pasivos.stream()
            .map(s -> s.getSaldo().abs())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Capital
        List<SaldoCuenta> capital = calcularSaldosPorTipo(TipoCuenta.CAPITAL, fecha);
        BigDecimal totalCapital = capital.stream()
            .map(s -> s.getSaldo().abs())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Utilidad del ejercicio
        BigDecimal utilidad = calcularUtilidadDelEjercicio(
            LocalDate.of(fecha.getYear(), 1, 1), 
            fecha
        );
        
        balance.put("fecha", fecha);
        balance.put("activos", activos);
        balance.put("totalActivos", totalActivos);
        balance.put("pasivos", pasivos);
        balance.put("totalPasivos", totalPasivos);
        balance.put("capital", capital);
        balance.put("totalCapital", totalCapital);
        balance.put("utilidadEjercicio", utilidad);
        balance.put("totalPasivosCapital", totalPasivos.add(totalCapital).add(utilidad));
        
        return balance;
    }
    
    /**
     * Genera el estado de resultados (income statement) de un período.
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Mapa con ingresos, egresos y utilidad
     */
    public Map<String, Object> generarEstadoResultados(LocalDate fechaInicio, LocalDate fechaFin) {
        log.info("Generando estado de resultados entre {} y {}", fechaInicio, fechaFin);
        
        Map<String, Object> estado = new HashMap<>();
        
        // Ingresos
        List<SaldoCuenta> ingresos = calcularSaldosPorTipoPeriodo(TipoCuenta.INGRESO, fechaInicio, fechaFin);
        BigDecimal totalIngresos = ingresos.stream()
            .map(s -> s.getSaldo().abs())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Egresos
        List<SaldoCuenta> egresos = calcularSaldosPorTipoPeriodo(TipoCuenta.EGRESO, fechaInicio, fechaFin);
        BigDecimal totalEgresos = egresos.stream()
            .map(SaldoCuenta::getSaldo)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Utilidad
        BigDecimal utilidad = totalIngresos.subtract(totalEgresos);
        
        estado.put("fechaInicio", fechaInicio);
        estado.put("fechaFin", fechaFin);
        estado.put("ingresos", ingresos);
        estado.put("totalIngresos", totalIngresos);
        estado.put("egresos", egresos);
        estado.put("totalEgresos", totalEgresos);
        estado.put("utilidad", utilidad);
        
        return estado;
    }
    
    /**
     * Calcula la utilidad del ejercicio en un período.
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Utilidad (ingresos - egresos)
     */
    public BigDecimal calcularUtilidadDelEjercicio(LocalDate fechaInicio, LocalDate fechaFin) {
        log.debug("Calculando utilidad entre {} y {}", fechaInicio, fechaFin);
        
        // Total ingresos
        List<CuentaContable> cuentasIngreso = cuentaRepository.findByTipo(TipoCuenta.INGRESO);
        BigDecimal totalIngresos = cuentasIngreso.stream()
            .map(c -> {
                BigDecimal saldo = detalleRepository.calcularSaldoCuentaPeriodo(
                    c.getIdCuenta(), fechaInicio, fechaFin
                );
                return saldo != null ? saldo.abs() : BigDecimal.ZERO;
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Total egresos
        List<CuentaContable> cuentasEgreso = cuentaRepository.findByTipo(TipoCuenta.EGRESO);
        BigDecimal totalEgresos = cuentasEgreso.stream()
            .map(c -> {
                BigDecimal saldo = detalleRepository.calcularSaldoCuentaPeriodo(
                    c.getIdCuenta(), fechaInicio, fechaFin
                );
                return saldo != null ? saldo : BigDecimal.ZERO;
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalIngresos.subtract(totalEgresos);
    }
    
    /**
     * Calcula saldos de cuentas por tipo hasta una fecha.
     * @param tipo Tipo de cuenta
     * @param fecha Fecha límite
     * @return Lista de saldos
     */
    private List<SaldoCuenta> calcularSaldosPorTipo(TipoCuenta tipo, LocalDate fecha) {
        List<CuentaContable> cuentas = cuentaRepository.findByTipoAndActiva(tipo, true);
        
        return cuentas.stream()
            .map(cuenta -> {
                BigDecimal debe = detalleRepository.calcularTotalDebitosCuenta(
                    cuenta.getIdCuenta(), 
                    LocalDate.of(1900, 1, 1), 
                    fecha
                );
                BigDecimal haber = detalleRepository.calcularTotalCreditosCuenta(
                    cuenta.getIdCuenta(), 
                    LocalDate.of(1900, 1, 1), 
                    fecha
                );
                return new SaldoCuenta(cuenta, debe, haber);
            })
            .filter(s -> s.getSaldo().compareTo(BigDecimal.ZERO) != 0)
            .collect(Collectors.toList());
    }
    
    /**
     * Calcula saldos de cuentas por tipo en un período.
     * @param tipo Tipo de cuenta
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de saldos
     */
    private List<SaldoCuenta> calcularSaldosPorTipoPeriodo(TipoCuenta tipo, LocalDate fechaInicio, LocalDate fechaFin) {
        List<CuentaContable> cuentas = cuentaRepository.findByTipoAndActiva(tipo, true);
        
        return cuentas.stream()
            .map(cuenta -> {
                BigDecimal debe = detalleRepository.calcularTotalDebitosCuenta(
                    cuenta.getIdCuenta(), fechaInicio, fechaFin
                );
                BigDecimal haber = detalleRepository.calcularTotalCreditosCuenta(
                    cuenta.getIdCuenta(), fechaInicio, fechaFin
                );
                return new SaldoCuenta(cuenta, debe, haber);
            })
            .filter(s -> s.getSaldo().compareTo(BigDecimal.ZERO) != 0)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene estadísticas generales de contabilidad.
     * @return Mapa con estadísticas
     */
    public Map<String, Object> obtenerEstadisticas() {
        log.debug("Obteniendo estadísticas contables");
        
        Map<String, Object> stats = new HashMap<>();
        
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate inicioAnio = LocalDate.of(hoy.getYear(), 1, 1);
        
        // Cuentas
        stats.put("totalCuentas", cuentaRepository.count());
        stats.put("cuentasActivas", cuentaRepository.findByActiva(true).size());
        stats.put("cuentasOperativas", cuentaRepository.findByAceptaMovimientos(true).size());
        
        // Movimientos del mes
        Long movimientosMes = (long) detalleRepository.findCuentasConMovimientos(inicioMes, hoy).size();
        stats.put("cuentasConMovimientosMes", movimientosMes);
        
        // Utilidades
        BigDecimal utilidadMes = calcularUtilidadDelEjercicio(inicioMes, hoy);
        BigDecimal utilidadAnio = calcularUtilidadDelEjercicio(inicioAnio, hoy);
        stats.put("utilidadMes", utilidadMes);
        stats.put("utilidadAnio", utilidadAnio);
        
        return stats;
    }
}
