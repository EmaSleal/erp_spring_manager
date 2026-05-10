package api.astro.whats_orders_manager.modules.contabilidad.dto.mapper;

import api.astro.whats_orders_manager.modules.contabilidad.dto.AsientoContableDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.DetalleAsientoDTO;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para conversión entre AsientoContable y AsientoContableDTO.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
@Component
@RequiredArgsConstructor
public class AsientoContableMapper {
    
    private final DetalleAsientoMapper detalleMapper;
    private final FacturaRepository facturaRepository;
    private final PagoRepository pagoRepository;
    
    /**
     * Convierte una entidad AsientoContable a DTO.
     * @param asiento Entidad
     * @return DTO
     */
    public AsientoContableDTO toDTO(AsientoContable asiento) {
        if (asiento == null) {
            return null;
        }
        
        AsientoContableDTO dto = AsientoContableDTO.builder()
            .idAsiento(asiento.getIdAsiento())
            .numero(asiento.getNumero())
            .fecha(asiento.getFecha())
            .concepto(asiento.getConcepto())
            .tipo(asiento.getTipo())
            .estado(asiento.getEstado())
            .creadoPor(asiento.getCreadoPor())
            .modificadoPor(asiento.getModificadoPor())
            .fechaCreacion(asiento.getFechaCreacion().toLocalDate())
            .fechaModificacion(asiento.getFechaModificacion().toLocalDate())
            .build();
        
        // Información de factura
        if (asiento.getFactura() != null) {
            dto.setFacturaId(Long.getLong( asiento.getFactura().getIdFactura().toString()));
            dto.setFacturaNumero(asiento.getFactura().getNumeroFactura());
        }
        
        // Información de pago
        if (asiento.getPago() != null) {
            dto.setPagoId(asiento.getPago().getIdPago());
            dto.setPagoReferencia(asiento.getPago().getReferenciaBancaria());
        }
        
        // Convertir detalles
        if (asiento.getDetalles() != null) {
            List<DetalleAsientoDTO> detallesDTO = detalleMapper.toDTOList(asiento.getDetalles());
            dto.setDetalles(detallesDTO);
        }
        
        // Totales calculados
        dto.setTotalDebe(asiento.getTotalDebe());
        dto.setTotalHaber(asiento.getTotalHaber());
        dto.setDiferencia(asiento.getTotalDebe().subtract(asiento.getTotalHaber()));
        dto.setEstaCuadrado(asiento.estaCuadrado());
        
        // Información de estado
        dto.setPuedeModificarse(asiento.puedeModificarse());
        dto.setPuedeContabilizarse(asiento.puedeContabilizarse());
        dto.setEsAutomatico(asiento.getTipo().esAutomatico());
        dto.setEsEditable(asiento.getTipo().esEditable());
        
        return dto;
    }
    
    /**
     * Convierte DTO a entidad AsientoContable.
     * @param dto DTO
     * @return Entidad
     */
    public AsientoContable toEntity(AsientoContableDTO dto) {
        if (dto == null) {
            return null;
        }
        
        AsientoContable asiento = new AsientoContable();
        asiento.setIdAsiento(dto.getIdAsiento());
        asiento.setNumero(dto.getNumero());
        asiento.setFecha(dto.getFecha());
        asiento.setConcepto(dto.getConcepto());
        asiento.setTipo(dto.getTipo());
        asiento.setEstado(dto.getEstado());
        
        // Buscar factura si existe
        if (dto.getFacturaId() != null) {
            facturaRepository.findById(dto.getFacturaId().intValue())
                .ifPresent(asiento::setFactura);
        }
        
        // Buscar pago si existe
        if (dto.getPagoId() != null) {
            pagoRepository.findById(dto.getPagoId())
                .ifPresent(asiento::setPago);
        }
        
        // Convertir detalles
        if (dto.getDetalles() != null) {
            asiento.setDetalles(detalleMapper.toEntityList(dto.getDetalles()));
        }
        
        return asiento;
    }
    
    /**
     * Convierte una entidad a DTO simplificado (sin detalles).
     * @param asiento Entidad
     * @return DTO simplificado
     */
    public AsientoContableDTO toDTOSimple(AsientoContable asiento) {
        if (asiento == null) {
            return null;
        }
        
        AsientoContableDTO dto = AsientoContableDTO.builder()
            .idAsiento(asiento.getIdAsiento())
            .numero(asiento.getNumero())
            .fecha(asiento.getFecha())
            .concepto(asiento.getConcepto())
            .tipo(asiento.getTipo())
            .estado(asiento.getEstado())
            .totalDebe(asiento.getTotalDebe())
            .totalHaber(asiento.getTotalHaber())
            .estaCuadrado(asiento.estaCuadrado())
            .build();
        
        // Información de factura
        if (asiento.getFactura() != null) {
            dto.setFacturaId(asiento.getFactura().getIdFactura().longValue());
            dto.setFacturaNumero(asiento.getFactura().getNumeroFactura());
        }
        
        // Información de pago
        if (asiento.getPago() != null) {
            dto.setPagoId(asiento.getPago().getIdPago());
            dto.setPagoReferencia(asiento.getPago().getReferenciaBancaria());
        }
        
        return dto;
    }
    
    /**
     * Convierte una lista de entidades a DTOs.
     * @param asientos Lista de entidades
     * @return Lista de DTOs
     */
    public List<AsientoContableDTO> toDTOList(List<AsientoContable> asientos) {
        if (asientos == null) {
            return List.of();
        }
        
        return asientos.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Convierte una lista de entidades a DTOs simplificados.
     * @param asientos Lista de entidades
     * @return Lista de DTOs simplificados
     */
    public List<AsientoContableDTO> toDTOSimpleList(List<AsientoContable> asientos) {
        if (asientos == null) {
            return List.of();
        }
        
        return asientos.stream()
            .map(this::toDTOSimple)
            .collect(Collectors.toList());
    }
    
    /**
     * Convierte una lista de DTOs a entidades.
     * @param dtos Lista de DTOs
     * @return Lista de entidades
     */
    public List<AsientoContable> toEntityList(List<AsientoContableDTO> dtos) {
        if (dtos == null) {
            return List.of();
        }
        
        return dtos.stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * Actualiza una entidad existente con datos del DTO.
     * @param asiento Entidad a actualizar
     * @param dto DTO con nuevos datos
     */
    public void updateEntityFromDTO(AsientoContable asiento, AsientoContableDTO dto) {
        if (asiento == null || dto == null) {
            return;
        }
        
        asiento.setNumero(dto.getNumero());
        asiento.setFecha(dto.getFecha());
        asiento.setConcepto(dto.getConcepto());
        asiento.setTipo(dto.getTipo());
        asiento.setEstado(dto.getEstado());
        
        // Actualizar factura si cambió
        if (dto.getFacturaId() != null) {
            facturaRepository.findById(dto.getFacturaId().intValue())
                .ifPresent(asiento::setFactura);
        } else {
            asiento.setFactura(null);
        }
        
        // Actualizar pago si cambió
        if (dto.getPagoId() != null) {
            pagoRepository.findById(dto.getPagoId())
                .ifPresent(asiento::setPago);
        } else {
            asiento.setPago(null);
        }
        
        // Actualizar detalles
        if (dto.getDetalles() != null) {
            asiento.getDetalles().clear();
            asiento.getDetalles().addAll(detalleMapper.toEntityList(dto.getDetalles()));
        }
    }
}
