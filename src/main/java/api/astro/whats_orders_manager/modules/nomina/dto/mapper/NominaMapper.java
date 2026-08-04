package api.astro.whats_orders_manager.modules.nomina.dto.mapper;

import api.astro.whats_orders_manager.modules.nomina.dto.NominaDTO;
import api.astro.whats_orders_manager.modules.nomina.dto.NominaResumenDTO;
import api.astro.whats_orders_manager.modules.nomina.model.Nomina;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Stateless mapper: Nomina entity ↔ NominaDTO / NominaResumenDTO.
 */
@Component
@RequiredArgsConstructor
public class NominaMapper {

    private final DetalleNominaMapper detalleMapper;

    /** Full DTO including detail lines. */
    public NominaDTO toDTO(Nomina nomina) {
        if (nomina == null) {
            return null;
        }

        return NominaDTO.builder()
                .id(nomina.getId())
                .numero(nomina.getNumero())
                .periodoInicio(nomina.getPeriodoInicio())
                .periodoFin(nomina.getPeriodoFin())
                .fechaPago(nomina.getFechaPago())
                .tipo(nomina.getTipo())
                .estado(nomina.getEstado())
                .totalBruto(nomina.getTotalBruto())
                .totalNeto(nomina.getTotalNeto())
                .cantidadEmpleados(nomina.getDetalles() != null ? nomina.getDetalles().size() : 0)
                .detalles(detalleMapper.toDTOList(nomina.getDetalles()))
                .build();
    }

    /** Summary DTO — no detail lines, used for list endpoints. */
    public NominaResumenDTO toResumenDTO(Nomina nomina) {
        if (nomina == null) {
            return null;
        }

        return NominaResumenDTO.builder()
                .id(nomina.getId())
                .numero(nomina.getNumero())
                .periodoInicio(nomina.getPeriodoInicio())
                .periodoFin(nomina.getPeriodoFin())
                .fechaPago(nomina.getFechaPago())
                .tipo(nomina.getTipo())
                .estado(nomina.getEstado())
                .totalBruto(nomina.getTotalBruto())
                .totalNeto(nomina.getTotalNeto())
                .cantidadEmpleados(nomina.getDetalles() != null ? nomina.getDetalles().size() : 0)
                .build();
    }

    public List<NominaResumenDTO> toResumenDTOList(List<Nomina> nominas) {
        if (nominas == null) {
            return List.of();
        }
        return nominas.stream()
                .map(this::toResumenDTO)
                .collect(Collectors.toList());
    }
}
