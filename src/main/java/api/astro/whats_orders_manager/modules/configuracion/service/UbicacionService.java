package api.astro.whats_orders_manager.modules.configuracion.service;

import api.astro.whats_orders_manager.modules.configuracion.dto.CantonDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.DistritoDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.ProvinciaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.UbicacionCompletaDTO;

import lombok.NonNull;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Servicio unificado para gestión de ubicaciones de Costa Rica
 * Maneja provincias, cantones y distritos desde un único punto
 */
@Service
public interface UbicacionService {
    public List<ProvinciaDTO> obtenerProvincias() ;
    public ProvinciaDTO obtenerProvinciaPorCodigo(@NonNull String codigo);
    public ProvinciaDTO buscarProvinciaPorNombre(String nombre);
    public List<CantonDTO> obtenerCantonesPorProvincia(String provinciaCodigo);
    public CantonDTO obtenerCanton(String provinciaCodigo, String cantonCodigo);
    public List<CantonDTO> buscarCantonesPorNombre(String nombre);
    public long contarCantonesPorProvincia(String provinciaCodigo);
    public List<DistritoDTO> obtenerDistritosPorProvincia(String provinciaCodigo);
    public List<DistritoDTO> obtenerDistritosPorCanton(String provinciaCodigo, String cantonCodigo);
    public DistritoDTO obtenerDistrito(String provinciaCodigo, String cantonCodigo, String distritoCodigo);
    public List<DistritoDTO> buscarDistritosPorNombre(String nombre);
    public long contarDistritosPorCanton(String provinciaCodigo, String cantonCodigo);
    public UbicacionCompletaDTO obtenerUbicacionCompleta(@NonNull String provinciaCodigo, @NonNull String cantonCodigo, @NonNull String distritoCodigo);
    public boolean validarUbicacion(@NonNull String provinciaCodigo, String cantonCodigo, String distritoCodigo);

}
