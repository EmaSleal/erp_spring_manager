package api.astro.whats_orders_manager.modules.configuracion.dto.mapper;

import api.astro.whats_orders_manager.modules.configuracion.dto.ParametroSistemaDTO;
import api.astro.whats_orders_manager.modules.configuracion.model.ParametroSistema;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between ParametroSistema entity and DTO.
 * Note: toEntity is provided for symmetry but is currently unused by any
 * controller — the POST endpoint calls crearParametro with individual fields.
 */
@Component
public class ParametroSistemaMapper {

    /**
     * Converts a ParametroSistema entity to its DTO representation.
     * tipoDatoNombre and categoriaNombre are null-safe computed fields.
     * valorValido is derived from the entity's isValido() method.
     *
     * @param entity source entity; returns null if entity is null
     * @return mapped DTO
     */
    public ParametroSistemaDTO toDTO(ParametroSistema entity) {
        if (entity == null) {
            return null;
        }

        ParametroSistemaDTO dto = new ParametroSistemaDTO();

        dto.setIdParametro(entity.getIdParametro());
        dto.setClave(entity.getClave());
        dto.setValor(entity.getValor());
        dto.setTipoDato(entity.getTipoDato());
        dto.setTipoDatoNombre(entity.getTipoDato() != null ? entity.getTipoDato().getDescripcion() : null);
        dto.setCategoria(entity.getCategoria());
        dto.setCategoriaNombre(entity.getCategoria() != null ? entity.getCategoria().getNombre() : null);
        dto.setDescripcion(entity.getDescripcion());
        dto.setEditable(entity.getEditable());
        dto.setValorValido(entity.isValido());

        return dto;
    }

    /**
     * Converts a ParametroSistemaDTO to its entity representation.
     * Maps persisted fields only — tipoDatoNombre, categoriaNombre, and
     * valorValido are derived/computed and are not stored in the entity.
     *
     * @param dto source DTO; returns null if dto is null
     * @return mapped entity
     */
    public ParametroSistema toEntity(ParametroSistemaDTO dto) {
        if (dto == null) {
            return null;
        }

        return ParametroSistema.builder()
                .idParametro(dto.getIdParametro())
                .clave(dto.getClave())
                .valor(dto.getValor())
                .tipoDato(dto.getTipoDato())
                .categoria(dto.getCategoria())
                .descripcion(dto.getDescripcion())
                .editable(dto.getEditable())
                .build();
    }
}
