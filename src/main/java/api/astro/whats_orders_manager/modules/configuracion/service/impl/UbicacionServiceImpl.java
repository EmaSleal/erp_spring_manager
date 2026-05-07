package api.astro.whats_orders_manager.modules.configuracion.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import api.astro.whats_orders_manager.modules.configuracion.dto.CantonDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.DistritoDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.ProvinciaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.UbicacionCompletaDTO;
import api.astro.whats_orders_manager.modules.configuracion.model.CantonCostaRica;
import api.astro.whats_orders_manager.modules.configuracion.model.DistritoCostaRica;
import api.astro.whats_orders_manager.modules.configuracion.model.ProvinciaCostaRica;
import api.astro.whats_orders_manager.modules.configuracion.repository.CantonCostaRicaRepository;
import api.astro.whats_orders_manager.modules.configuracion.repository.DistritoCostaRicaRepository;
import api.astro.whats_orders_manager.modules.configuracion.repository.ProvinciaCostaRicaRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.UbicacionService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UbicacionServiceImpl implements UbicacionService {
    
    private final ProvinciaCostaRicaRepository provinciaRepository;
    private final CantonCostaRicaRepository cantonRepository;
    private final DistritoCostaRicaRepository distritoRepository;
    
    // ========== OPERACIONES DE PROVINCIAS ==========
    
    /**
     * Obtener todas las provincias
     */
    @Override
    public List<ProvinciaDTO> obtenerProvincias() {
        log.debug("Obteniendo todas las provincias");
        return provinciaRepository.findAllByOrderByCodigo()
                .stream()
                .map(this::convertirAProvinciaDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtener provincia por código
     */
    @Override
    public ProvinciaDTO obtenerProvinciaPorCodigo(@NonNull String codigo) {
        log.debug("Obteniendo provincia con código: {}", codigo);
        return provinciaRepository.findByCodigo(codigo)
                .map(this::convertirAProvinciaDTO)
                .orElse(null);
    }
    
    /**
     * Buscar provincia por nombre
     */
    @Override
    public ProvinciaDTO buscarProvinciaPorNombre(String nombre) {
        log.debug("Buscando provincia con nombre: {}", nombre);
        ProvinciaCostaRica provincia = provinciaRepository.findByNombre(nombre);
        return provincia != null ? convertirAProvinciaDTO(provincia) : null;
    }
    
    // ========== OPERACIONES DE CANTONES ==========
    
    /**
     * Obtener cantones de una provincia
     */
    @Override
    public List<CantonDTO> obtenerCantonesPorProvincia(String provinciaCodigo) {
        log.debug("Obteniendo cantones de provincia: {}", provinciaCodigo);
        return cantonRepository.findByProvinciaCodigoOrderByCodigo(provinciaCodigo)
                .stream()
                .map(this::convertirACantonDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtener cantón específico
     */
    @Override
    public CantonDTO obtenerCanton(String provinciaCodigo, String cantonCodigo) {
        log.debug("Obteniendo cantón {}-{}", provinciaCodigo, cantonCodigo);
        CantonCostaRica canton = cantonRepository.findByProvinciaCodigoAndCodigo(provinciaCodigo, cantonCodigo);
        return canton != null ? convertirACantonDTO(canton) : null;
    }
    
    /**
     * Buscar cantones por nombre (búsqueda parcial)
     */
    @Override
    public List<CantonDTO> buscarCantonesPorNombre(String nombre) {
        log.debug("Buscando cantones con nombre: {}", nombre);
        return cantonRepository.findByNombreContaining(nombre)
                .stream()
                .map(this::convertirACantonDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Contar cantones de una provincia
     */
    @Override
    public long contarCantonesPorProvincia(String provinciaCodigo) {
        log.debug("Contando cantones de provincia: {}", provinciaCodigo);
        return cantonRepository.countByProvinciaCodigo(provinciaCodigo);
    }
    
    // ========== OPERACIONES DE DISTRITOS ==========
    
    /**
     * Obtener distritos de una provincia
     */
    @Override
    public List<DistritoDTO> obtenerDistritosPorProvincia(String provinciaCodigo) {
        log.debug("Obteniendo distritos de provincia: {}", provinciaCodigo);
        return distritoRepository.findByProvinciaCodigoOrderByCantonCodigoAscCodigoAsc(provinciaCodigo)
                .stream()
                .map(this::convertirADistritoDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtener distritos de un cantón específico
     */
    @Override
    public List<DistritoDTO> obtenerDistritosPorCanton(String provinciaCodigo, String cantonCodigo) {
        log.debug("Obteniendo distritos de cantón {}-{}", provinciaCodigo, cantonCodigo);
        return distritoRepository.findByProvinciaCodigoAndCantonCodigoOrderByCodigo(provinciaCodigo, cantonCodigo)
                .stream()
                .map(this::convertirADistritoDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtener distrito específico
     */
    @Override
    public DistritoDTO obtenerDistrito(String provinciaCodigo, String cantonCodigo, String distritoCodigo) {
        log.debug("Obteniendo distrito {}-{}-{}", provinciaCodigo, cantonCodigo, distritoCodigo);
        DistritoCostaRica distrito = distritoRepository.findByProvinciaCodigoAndCantonCodigoAndCodigo(
                provinciaCodigo, cantonCodigo, distritoCodigo);
        return distrito != null ? convertirADistritoDTO(distrito) : null;
    }
    
    /**
     * Buscar distritos por nombre (búsqueda parcial)
     */
    @Override
    public List<DistritoDTO> buscarDistritosPorNombre(String nombre) {
        log.debug("Buscando distritos con nombre: {}", nombre);
        return distritoRepository.findByNombreContaining(nombre)
                .stream()
                .map(this::convertirADistritoDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Contar distritos de un cantón
     */
    @Override
    public long contarDistritosPorCanton(String provinciaCodigo, String cantonCodigo) {
        log.debug("Contando distritos de cantón {}-{}", provinciaCodigo, cantonCodigo);
        return distritoRepository.countByProvinciaCodigoAndCantonCodigo(provinciaCodigo, cantonCodigo);
    }
    
    // ========== OPERACIONES COMBINADAS ==========
    
    /**
     * Obtener ubicación completa (provincia + cantón + distrito)
     */
    @Override
    public UbicacionCompletaDTO obtenerUbicacionCompleta(@NonNull String provinciaCodigo, @NonNull String cantonCodigo, @NonNull String distritoCodigo) {
        log.debug("Obteniendo ubicación completa: {}-{}-{}", provinciaCodigo, cantonCodigo, distritoCodigo);
        
        ProvinciaCostaRica provincia = provinciaRepository.findByCodigo(provinciaCodigo).orElse(null);
        if (provincia == null) {
            return null;
        }
        
        CantonCostaRica canton = cantonRepository.findByProvinciaCodigoAndCodigo(provinciaCodigo, cantonCodigo);
        if (canton == null) {
            return null;
        }
        
        DistritoCostaRica distrito = distritoRepository.findByProvinciaCodigoAndCantonCodigoAndCodigo(
                provinciaCodigo, cantonCodigo, distritoCodigo);
        if (distrito == null) {
            return null;
        }
        
        return new UbicacionCompletaDTO(
                provincia.getCodigo(),
                provincia.getNombre(),
                canton.getCodigo(),
                canton.getNombre(),
                distrito.getCodigo(),
                distrito.getNombre(),
                String.format("%s-%s-%s", provinciaCodigo, cantonCodigo, distritoCodigo),
                String.format("%s, %s, %s", provincia.getNombre(), canton.getNombre(), distrito.getNombre())
        );
    }
    
    /**
     * Validar que una ubicación existe
     */
    @Override
    public boolean validarUbicacion(@NonNull String provinciaCodigo, String cantonCodigo, String distritoCodigo) {
        log.debug("Validando ubicación: {}-{}-{}", provinciaCodigo, cantonCodigo, distritoCodigo);
        
        if (!provinciaRepository.existsByCodigo(provinciaCodigo)) {
            return false;
        }
        
        if (cantonCodigo != null) {
            CantonCostaRica canton = cantonRepository.findByProvinciaCodigoAndCodigo(provinciaCodigo, cantonCodigo);
            if (canton == null) {
                return false;
            }
            
            if (distritoCodigo != null) {
                DistritoCostaRica distrito = distritoRepository.findByProvinciaCodigoAndCantonCodigoAndCodigo(
                        provinciaCodigo, cantonCodigo, distritoCodigo);
                return distrito != null;
            }
        }
        
        return true;
    }
    
    // ========== MÉTODOS DE CONVERSIÓN ==========
    
    private ProvinciaDTO convertirAProvinciaDTO(ProvinciaCostaRica provincia) {
        return new ProvinciaDTO(
                provincia.getCodigo(),
                provincia.getNombre()
        );
    }
    
    private CantonDTO convertirACantonDTO(CantonCostaRica canton) {
        return new CantonDTO(
                canton.getProvinciaCodigo(),
                canton.getCodigo(),
                canton.getNombre(),
                String.format("%s-%s", canton.getProvinciaCodigo(), canton.getCodigo())
        );
    }
    
    private DistritoDTO convertirADistritoDTO(DistritoCostaRica distrito) {
        return new DistritoDTO(
                distrito.getProvinciaCodigo(),
                distrito.getCantonCodigo(),
                distrito.getCodigo(),
                distrito.getNombre(),
                String.format("%s-%s-%s", distrito.getProvinciaCodigo(), distrito.getCantonCodigo(), distrito.getCodigo())
        );
    }
}
