package api.astro.whats_orders_manager.modules.contabilidad.dto.mapper;

import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaContableDTO;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para conversión entre CuentaContable y CuentaContableDTO.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
@Component
@RequiredArgsConstructor
public class CuentaContableMapper {
    
    private final CuentaContableRepository cuentaRepository;
    private final DetalleAsientoRepository detalleRepository;
    
    /**
     * Convierte una entidad CuentaContable a DTO.
     * @param cuenta Entidad
     * @return DTO
     */
    public CuentaContableDTO toDTO(CuentaContable cuenta) {
        if (cuenta == null) {
            return null;
        }
        
        CuentaContableDTO dto = CuentaContableDTO.builder()
            .idCuenta(cuenta.getIdCuenta())
            .codigo(cuenta.getCodigo())
            .nombre(cuenta.getNombre())
            .descripcion(cuenta.getDescripcion())
            .tipo(cuenta.getTipo())
            .naturaleza(cuenta.getNaturaleza())
            .nivel(cuenta.getNivel())
            .activa(cuenta.getActiva())
            .aceptaMovimientos(cuenta.getAceptaMovimientos())
            .creadoPor(cuenta.getCreadoPor())
            .fechaCreacion(cuenta.getFechaCreacion())
            .modificadoPor(cuenta.getModificadoPor())
            .fechaModificacion(cuenta.getFechaModificacion())
            .build();
        
        // Información de cuenta padre
        if (cuenta.getCuentaPadre() != null) {
            dto.setCuentaPadreId(cuenta.getCuentaPadre().getIdCuenta());
            dto.setCuentaPadreCodigo(cuenta.getCuentaPadre().getCodigo());
            dto.setCuentaPadreNombre(cuenta.getCuentaPadre().getNombre());
        }
        
        return dto;
    }
    
    /**
     * Convierte DTO a entidad CuentaContable.
     * @param dto DTO
     * @return Entidad
     */
    public CuentaContable toEntity(CuentaContableDTO dto) {
        if (dto == null) {
            return null;
        }
        
        CuentaContable cuenta = new CuentaContable();
        cuenta.setIdCuenta(dto.getIdCuenta());
        cuenta.setCodigo(dto.getCodigo());
        cuenta.setNombre(dto.getNombre());
        cuenta.setDescripcion(dto.getDescripcion());
        cuenta.setTipo(dto.getTipo());
        cuenta.setNaturaleza(dto.getNaturaleza());
        cuenta.setNivel(dto.getNivel());
        cuenta.setActiva(dto.getActiva() != null ? dto.getActiva() : true);
        cuenta.setAceptaMovimientos(dto.getAceptaMovimientos() != null ? dto.getAceptaMovimientos() : true);
        
        // Buscar cuenta padre si existe
        if (dto.getCuentaPadreId() != null) {
            cuentaRepository.findById(dto.getCuentaPadreId())
                .ifPresent(cuenta::setCuentaPadre);
        }
        
        return cuenta;
    }
    
    /**
     * Convierte una entidad a DTO con información extendida.
     * @param cuenta Entidad
     * @param incluirSubcuentas Si debe incluir subcuentas
     * @return DTO extendido
     */
    public CuentaContableDTO toDTOExtendido(CuentaContable cuenta, boolean incluirSubcuentas) {
        if (cuenta == null) {
            return null;
        }
        
        CuentaContableDTO dto = toDTO(cuenta);
        
        // Cantidad de subcuentas
        Long cantidadSubcuentas = cuentaRepository.countSubcuentas(cuenta.getIdCuenta());
        dto.setCantidadSubcuentas(cantidadSubcuentas);
        
        // Verificar si tiene movimientos
        Long movimientos = detalleRepository.contarMovimientosCuenta(cuenta.getIdCuenta());
        dto.setTieneMovimientos(movimientos > 0);
        
        // Incluir subcuentas si se solicita
        if (incluirSubcuentas && cuenta.getSubcuentas() != null && !cuenta.getSubcuentas().isEmpty()) {
            List<CuentaContableDTO> subcuentasDTO = cuenta.getSubcuentas().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
            dto.setSubcuentas(subcuentasDTO);
        }
        
        return dto;
    }
    
    /**
     * Convierte una lista de entidades a DTOs.
     * @param cuentas Lista de entidades
     * @return Lista de DTOs
     */
    public List<CuentaContableDTO> toDTOList(List<CuentaContable> cuentas) {
        if (cuentas == null) {
            return List.of();
        }
        
        return cuentas.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Convierte una lista de DTOs a entidades.
     * @param dtos Lista de DTOs
     * @return Lista de entidades
     */
    public List<CuentaContable> toEntityList(List<CuentaContableDTO> dtos) {
        if (dtos == null) {
            return List.of();
        }
        
        return dtos.stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * Actualiza una entidad existente con datos del DTO.
     * @param cuenta Entidad a actualizar
     * @param dto DTO con nuevos datos
     */
    public void updateEntityFromDTO(CuentaContable cuenta, CuentaContableDTO dto) {
        if (cuenta == null || dto == null) {
            return;
        }
        
        cuenta.setCodigo(dto.getCodigo());
        cuenta.setNombre(dto.getNombre());
        cuenta.setDescripcion(dto.getDescripcion());
        cuenta.setTipo(dto.getTipo());
        cuenta.setNaturaleza(dto.getNaturaleza());
        cuenta.setActiva(dto.getActiva() != null ? dto.getActiva() : true);
        cuenta.setAceptaMovimientos(dto.getAceptaMovimientos() != null ? dto.getAceptaMovimientos() : true);
        
        // Actualizar cuenta padre si cambió
        if (dto.getCuentaPadreId() != null) {
            cuentaRepository.findById(dto.getCuentaPadreId())
                .ifPresent(cuenta::setCuentaPadre);
        } else {
            cuenta.setCuentaPadre(null);
        }
    }
}
