package api.astro.whats_orders_manager.modules.facturacion.dto.mapper;

import api.astro.whats_orders_manager.modules.facturacion.dto.PagoDTO;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre entidades Pago y PagoDTO.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 1
 */
public class PagoMapper {
    
    /**
     * Convierte una entidad Pago a PagoDTO.
     * @param pago Entidad Pago
     * @return PagoDTO
     */
    public static PagoDTO toDTO(Pago pago) {
        if (pago == null) {
            return null;
        }
        
        PagoDTO dto = PagoDTO.builder()
            .idPago(pago.getIdPago())
            .numeroPago(pago.getNumeroPago())
            .idCliente(pago.getCliente() != null ? pago.getCliente().getIdCliente() : null)
            .idFactura(pago.getFactura() != null ? pago.getFactura().getIdFactura() : null)
            .tipoPago(pago.getTipoPago())
            .monto(pago.getMonto())
            .metodoPago(pago.getMetodoPago())
            .fechaPago(pago.getFechaPago())
            .referenciaBancaria(pago.getReferenciaBancaria())
            .banco(pago.getBanco())
            .cuentaBancaria(pago.getCuentaBancaria())
            .comprobanteUrl(pago.getComprobanteUrl())
            .estado(pago.getEstado())
            .observaciones(pago.getObservaciones())
            .conciliado(pago.getConciliado())
            .fechaConciliacion(pago.getFechaConciliacion())
            .creadoPor(pago.getCreadoPor())
            .fechaCreacion(pago.getFechaCreacion())
            .modificadoPor(pago.getModificadoPor())
            .fechaModificacion(pago.getFechaModificacion())
            .anuladoPor(pago.getAnuladoPor())
            .anuladoEn(pago.getAnuladoEn())
            .motivoAnulacion(pago.getMotivoAnulacion())
            .build();
        
        // Agregar datos del cliente si está presente
        if (pago.getCliente() != null) {
            dto.setNombreCliente(pago.getCliente().getNombre());
        }
        
        // Agregar datos de la factura si está presente
        if (pago.getFactura() != null) {
            dto.setNumeroFactura(pago.getFactura().getNumeroFactura());
            dto.setTotalFactura(pago.getFactura().getTotal());
            dto.setSaldoPendiente(pago.getFactura().calcularSaldoPendiente());
            
            if (pago.getFactura().getCliente() != null) {
                dto.setNombreCliente(pago.getFactura().getCliente().getNombre());
            }
        }
        
        // Agregar descripciones para la vista
        if (pago.getTipoPago() != null) {
            dto.setTipoPagoDescripcion(pago.getTipoPago().getDescripcion());
        }
        
        if (pago.getMetodoPago() != null) {
            dto.setMetodoPagoDescripcion(pago.getMetodoPago().getDescripcion());
        }
        
        if (pago.getEstado() != null) {
            dto.setEstadoDescripcion(pago.getEstado().getDescripcion());
        }
        
        return dto;
    }
    
    /**
     * Convierte un PagoDTO a entidad Pago (sin relaciones completas).
     * La factura debe asignarse separadamente en el servicio.
     * @param dto PagoDTO
     * @return Entidad Pago
     */
    public static Pago toEntity(PagoDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Pago pago = new Pago();
        pago.setIdPago(dto.getIdPago());
        pago.setNumeroPago(dto.getNumeroPago());
        pago.setTipoPago(dto.getTipoPago());
        pago.setMonto(dto.getMonto());
        pago.setMetodoPago(dto.getMetodoPago());
        pago.setFechaPago(dto.getFechaPago());
        pago.setReferenciaBancaria(dto.getReferenciaBancaria());
        pago.setBanco(dto.getBanco());
        pago.setCuentaBancaria(dto.getCuentaBancaria());
        pago.setComprobanteUrl(dto.getComprobanteUrl());
        pago.setEstado(dto.getEstado());
        pago.setObservaciones(dto.getObservaciones());
        pago.setConciliado(dto.getConciliado());
        pago.setFechaConciliacion(dto.getFechaConciliacion());
        pago.setAnuladoPor(dto.getAnuladoPor());
        pago.setAnuladoEn(dto.getAnuladoEn());
        pago.setMotivoAnulacion(dto.getMotivoAnulacion());
        
        // Nota: Cliente y factura deben asignarse en el servicio usando idCliente e idFactura
        
        return pago;
    }
    
    /**
     * Convierte una lista de entidades Pago a lista de DTOs.
     * @param pagos Lista de entidades
     * @return Lista de DTOs
     */
    public static List<PagoDTO> toDTOList(List<Pago> pagos) {
        if (pagos == null) {
            return null;
        }
        
        return pagos.stream()
            .map(PagoMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Convierte una lista de DTOs a lista de entidades Pago.
     * @param dtos Lista de DTOs
     * @return Lista de entidades
     */
    public static List<Pago> toEntityList(List<PagoDTO> dtos) {
        if (dtos == null) {
            return null;
        }
        
        return dtos.stream()
            .map(PagoMapper::toEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * Actualiza una entidad Pago existente con los datos del DTO.
     * No modifica el ID ni la factura asociada.
     * @param pago Entidad a actualizar
     * @param dto DTO con los nuevos datos
     */
    public static void updateEntityFromDTO(Pago pago, PagoDTO dto) {
        if (pago == null || dto == null) {
            return;
        }
        
        // No actualizar numeroPago (es único e inmutable)
        pago.setTipoPago(dto.getTipoPago());
        pago.setMonto(dto.getMonto());
        pago.setMetodoPago(dto.getMetodoPago());
        pago.setFechaPago(dto.getFechaPago());
        pago.setReferenciaBancaria(dto.getReferenciaBancaria());
        pago.setBanco(dto.getBanco());
        pago.setCuentaBancaria(dto.getCuentaBancaria());
        pago.setComprobanteUrl(dto.getComprobanteUrl());
        pago.setEstado(dto.getEstado());
        pago.setObservaciones(dto.getObservaciones());
        pago.setConciliado(dto.getConciliado());
        pago.setFechaConciliacion(dto.getFechaConciliacion());
        // No actualizar campos de anulación manualmente, se usan métodos de negocio
    }
}
