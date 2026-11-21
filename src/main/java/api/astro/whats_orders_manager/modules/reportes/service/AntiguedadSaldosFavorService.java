package api.astro.whats_orders_manager.modules.reportes.service;

import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoSaldoFavor;
import api.astro.whats_orders_manager.modules.facturacion.model.SaldoFavorCliente;
import api.astro.whats_orders_manager.modules.facturacion.repository.SaldoFavorClienteRepository;
import api.astro.whats_orders_manager.modules.facturacion.util.TramoAntiguedad;
import api.astro.whats_orders_manager.modules.reportes.dto.AntiguedadSaldosFavorDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reporte de antigüedad de los saldos a favor vivos (T-049, ADR-058 §6): por cliente y moneda, en los tramos fijos 0-7,
 * 8-30, 31-90, 91-180 y más de 180 días contados desde {@code fechaOrigen} hasta la fecha de corte (hoy por defecto), con
 * totales por tramo y moneda y la columna de reservado. Solo lectura: no existe ninguna acción contable asociada (ADR-053 §5).
 */
@Service
@RequiredArgsConstructor
public class AntiguedadSaldosFavorService {

    private final SaldoFavorClienteRepository saldoRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AntiguedadSaldosFavorDTO generar(LocalDate fechaCorte) {
        LocalDate corte = fechaCorte != null ? fechaCorte : LocalDate.now(clock);
        Map<String, Acumulado> porClienteYMoneda = new LinkedHashMap<>();
        Map<String, Acumulado> porMoneda = new LinkedHashMap<>();
        for (SaldoFavorCliente s : saldoRepository.findVivosHasta(EstadoSaldoFavor.VIGENTE, corte)) {
            TramoAntiguedad tramo = TramoAntiguedad.de(ChronoUnit.DAYS.between(s.getFechaOrigen(), corte));
            porClienteYMoneda.computeIfAbsent(s.getCliente().getIdCliente() + "|" + s.getMoneda(),
                k -> new Acumulado(s.getCliente().getIdCliente(), s.getCliente().getNombre(), s.getMoneda()))
                .sumar(tramo, s.getSaldoVivo(), s.getMontoReservado());
            porMoneda.computeIfAbsent(s.getMoneda(), k -> new Acumulado(null, "Total", s.getMoneda()))
                .sumar(tramo, s.getSaldoVivo(), s.getMontoReservado());
        }
        List<AntiguedadSaldosFavorDTO.Fila> filas = new ArrayList<>();
        porClienteYMoneda.values().forEach(a -> filas.add(a.aFila()));
        List<AntiguedadSaldosFavorDTO.Fila> totales = new ArrayList<>();
        porMoneda.values().forEach(a -> totales.add(a.aFila()));
        return new AntiguedadSaldosFavorDTO(corte, filas, totales);
    }

    private static final class Acumulado {
        private final Integer idCliente;
        private final String cliente;
        private final String moneda;
        private final BigDecimal[] tramos = new BigDecimal[TramoAntiguedad.values().length];
        private BigDecimal reservado = BigDecimal.ZERO;

        Acumulado(Integer idCliente, String cliente, String moneda) {
            this.idCliente = idCliente;
            this.cliente = cliente;
            this.moneda = moneda;
            java.util.Arrays.fill(tramos, BigDecimal.ZERO);
        }

        void sumar(TramoAntiguedad tramo, BigDecimal vivo, BigDecimal reservado) {
            tramos[tramo.ordinal()] = tramos[tramo.ordinal()].add(vivo);
            this.reservado = this.reservado.add(reservado);
        }

        AntiguedadSaldosFavorDTO.Fila aFila() {
            BigDecimal total = BigDecimal.ZERO;
            for (BigDecimal t : tramos) {
                total = total.add(t);
            }
            return new AntiguedadSaldosFavorDTO.Fila(idCliente, cliente, moneda, tramos[0], tramos[1], tramos[2],
                tramos[3], tramos[4], total, reservado, total.subtract(reservado));
        }
    }
}
