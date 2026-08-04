package api.astro.whats_orders_manager.modules.rrhh.service.impl;

import api.astro.whats_orders_manager.modules.rrhh.model.Empleado;
import api.astro.whats_orders_manager.modules.rrhh.model.SaldoVacaciones;
import api.astro.whats_orders_manager.modules.rrhh.repository.EmpleadoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.SaldoVacacionesRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.SaldoVacacionesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SaldoVacacionesServiceImpl implements SaldoVacacionesService {

    private final SaldoVacacionesRepository saldoVacacionesRepository;
    private final EmpleadoRepository empleadoRepository;

    @Override
    public void inicializar(Long empleadoId) {
        Empleado empleado = requireEmpleado(empleadoId);

        SaldoVacaciones saldo = new SaldoVacaciones();
        saldo.setEmpleado(empleado);
        saldo.setDiasGenerados(BigDecimal.ZERO);
        saldo.setDiasDisfrutados(BigDecimal.ZERO);
        saldo.setFechaUltimoCalculo(LocalDate.now());

        saldoVacacionesRepository.save(saldo);
        log.info("SaldoVacaciones inicializado para empleado id={}", empleadoId);
    }

    @Override
    public void acreditarDias(Long empleadoId, BigDecimal dias) {
        Empleado empleado = requireEmpleado(empleadoId);

        SaldoVacaciones saldo = saldoVacacionesRepository.findByEmpleadoId(empleadoId)
                .orElseGet(() -> {
                    SaldoVacaciones nuevo = new SaldoVacaciones();
                    nuevo.setEmpleado(empleado);
                    nuevo.setDiasGenerados(BigDecimal.ZERO);
                    nuevo.setDiasDisfrutados(BigDecimal.ZERO);
                    return nuevo;
                });

        saldo.setDiasGenerados(saldo.getDiasGenerados().add(dias));
        saldo.setFechaUltimoCalculo(LocalDate.now());

        saldoVacacionesRepository.save(saldo);
        log.info("Acreditados {} dias de vacaciones para empleado id={} — total generado: {}",
                dias, empleadoId, saldo.getDiasGenerados());
    }

    @Override
    public void descontarDias(Long empleadoId, BigDecimal dias) {
        SaldoVacaciones saldo = saldoVacacionesRepository.findByEmpleadoId(empleadoId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No existe saldo de vacaciones para el empleado id=" + empleadoId));

        BigDecimal disponibles = saldo.getDiasDisponiblesCalculados();
        if (dias.compareTo(disponibles) > 0) {
            throw new IllegalArgumentException(
                    "Saldo de dias disponibles insuficiente. Disponible: " + disponibles
                    + ", solicitado: " + dias + " para empleado id=" + empleadoId);
        }

        saldo.setDiasDisfrutados(saldo.getDiasDisfrutados().add(dias));
        saldoVacacionesRepository.save(saldo);
        log.info("Descontados {} dias de vacaciones para empleado id={} — disfrutados acumulado: {}",
                dias, empleadoId, saldo.getDiasDisfrutados());
    }

    @Override
    @Transactional(readOnly = true)
    public SaldoVacaciones consultarSaldo(Long empleadoId) {
        return saldoVacacionesRepository.findByEmpleadoId(empleadoId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No existe saldo de vacaciones para el empleado id=" + empleadoId));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Empleado requireEmpleado(Long empleadoId) {
        return empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new NoSuchElementException("Empleado no encontrado: " + empleadoId));
    }
}
