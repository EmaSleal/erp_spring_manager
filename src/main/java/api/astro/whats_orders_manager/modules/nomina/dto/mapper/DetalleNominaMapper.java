package api.astro.whats_orders_manager.modules.nomina.dto.mapper;

import api.astro.whats_orders_manager.modules.nomina.dto.DetalleNominaDTO;
import api.astro.whats_orders_manager.modules.nomina.model.DetalleNomina;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Stateless mapper: DetalleNomina entity ↔ DetalleNominaDTO.
 */
@Component
public class DetalleNominaMapper {

    public DetalleNominaDTO toDTO(DetalleNomina detalle) {
        if (detalle == null) {
            return null;
        }

        return DetalleNominaDTO.builder()
                .id(detalle.getId())
                .empleadoId(detalle.getEmpleado() != null ? detalle.getEmpleado().getId() : null)
                .empleadoNombre(detalle.getEmpleado() != null
                        ? detalle.getEmpleado().getNombreCompleto() : null)
                .salarioBase(detalle.getSalarioBase())
                .horasAusentes(detalle.getHorasAusentes())
                .brutoProrrateado(detalle.getBrutoProrrateado())
                .ccssObrero(detalle.getCcssObrero())
                .ins(detalle.getIns())
                .impuestoRenta(detalle.getImpuestoRenta())
                .creditoFamiliar(detalle.getCreditoFamiliar())
                .solidarista(detalle.getSolidarista())
                .pensionAlimentaria(detalle.getPensionAlimentaria())
                .totalDeducciones(detalle.getTotalDeducciones())
                .salarioNeto(detalle.getSalarioNeto())
                .ccssPatronal(detalle.getCcssPatronal())
                .build();
    }

    public List<DetalleNominaDTO> toDTOList(List<DetalleNomina> detalles) {
        if (detalles == null) {
            return List.of();
        }
        return detalles.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
