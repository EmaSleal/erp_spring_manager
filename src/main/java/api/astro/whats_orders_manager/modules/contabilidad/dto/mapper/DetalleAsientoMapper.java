package api.astro.whats_orders_manager.modules.contabilidad.dto.mapper;

import api.astro.whats_orders_manager.modules.contabilidad.dto.DetalleAsientoDTO;
import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para conversión entre DetalleAsiento y DetalleAsientoDTO.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
@Component
@RequiredArgsConstructor
public class DetalleAsientoMapper {
    
    private final AsientoContableRepository asientoRepository;
    private final CuentaContableRepository cuentaRepository;
    
    /**
     * Convierte una entidad DetalleAsiento a DTO.
     * @param detalle Entidad
     * @return DTO
     */
    public DetalleAsientoDTO toDTO(DetalleAsiento detalle) {
        if (detalle == null) {
            return null;
        }
        
        DetalleAsientoDTO dto = DetalleAsientoDTO.builder()
            .idDetalle(detalle.getIdDetalle())
            .debe(detalle.getDebe())
            .haber(detalle.getHaber())
            .descripcion(detalle.getDescripcion())
            .creadoPor(detalle.getCreateBy())
            .modificadoPor(detalle.getModifiedBy())
            .build();
        
        // Información del asiento
        if (detalle.getAsiento() != null) {
            dto.setAsientoId(detalle.getAsiento().getIdAsiento());
            dto.setAsientoNumero(detalle.getAsiento().getNumero());
        }
        
        // Información de la cuenta
        if (detalle.getCuenta() != null) {
            dto.setCuentaId(detalle.getCuenta().getIdCuenta());
            dto.setCuentaCodigo(detalle.getCuenta().getCodigo());
            dto.setCuentaNombre(detalle.getCuenta().getNombre());
        }
        
        // Información calculada
        dto.setMonto(detalle.getMonto());
        dto.setEsDebe(detalle.esDebe());
        dto.setEsHaber(detalle.esHaber());
        
        return dto;
    }
    
    /**
     * Convierte DTO a entidad DetalleAsiento.
     * @param dto DTO
     * @return Entidad
     */
    public DetalleAsiento toEntity(DetalleAsientoDTO dto) {
        if (dto == null) {
            return null;
        }
        
        DetalleAsiento detalle = new DetalleAsiento();
        detalle.setIdDetalle(dto.getIdDetalle());
        detalle.setDebe(dto.getDebe());
        detalle.setHaber(dto.getHaber());
        detalle.setDescripcion(dto.getDescripcion());
        
        // Buscar asiento si existe
        if (dto.getAsientoId() != null) {
            asientoRepository.findById(dto.getAsientoId())
                .ifPresent(detalle::setAsiento);
        }
        
        // Buscar cuenta si existe
        if (dto.getCuentaId() != null) {
            cuentaRepository.findById(dto.getCuentaId())
                .ifPresent(detalle::setCuenta);
        }
        
        return detalle;
    }
    
    /**
     * Convierte una lista de entidades a DTOs.
     * @param detalles Lista de entidades
     * @return Lista de DTOs
     */
    public List<DetalleAsientoDTO> toDTOList(List<DetalleAsiento> detalles) {
        if (detalles == null) {
            return List.of();
        }
        
        return detalles.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Convierte una lista de DTOs a entidades.
     * @param dtos Lista de DTOs
     * @return Lista de entidades
     */
    public List<DetalleAsiento> toEntityList(List<DetalleAsientoDTO> dtos) {
        if (dtos == null) {
            return List.of();
        }
        
        return dtos.stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * Actualiza una entidad existente con datos del DTO.
     * @param detalle Entidad a actualizar
     * @param dto DTO con nuevos datos
     */
    public void updateEntityFromDTO(DetalleAsiento detalle, DetalleAsientoDTO dto) {
        if (detalle == null || dto == null) {
            return;
        }
        
        detalle.setDebe(dto.getDebe());
        detalle.setHaber(dto.getHaber());
        detalle.setDescripcion(dto.getDescripcion());
        
        // Actualizar cuenta si cambió
        if (dto.getCuentaId() != null) {
            cuentaRepository.findById(dto.getCuentaId())
                .ifPresent(detalle::setCuenta);
        }
    }
}
