package api.astro.whats_orders_manager.modules.whatsapp.service;


import api.astro.whats_orders_manager.modules.whatsapp.model.PlantillaWhatsApp;
import api.astro.whats_orders_manager.modules.whatsapp.model.PlantillaWhatsApp.CategoriaPlantilla;
import api.astro.whats_orders_manager.modules.whatsapp.model.PlantillaWhatsApp.EstadoMeta;
import api.astro.whats_orders_manager.modules.whatsapp.dto.PlantillaWhatsAppDTO;
import api.astro.whats_orders_manager.modules.whatsapp.repository.PlantillaWhatsAppRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de plantillas de WhatsApp
 * Proporciona operaciones CRUD y caché de plantillas aprobadas
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - Fase 1.3
 */
@Service
@Slf4j
public class PlantillaWhatsAppService {
    
    private final PlantillaWhatsAppRepository plantillaRepository;
    
    public PlantillaWhatsAppService(PlantillaWhatsAppRepository plantillaRepository) {
        this.plantillaRepository = plantillaRepository;
    }
    
    /**
     * Obtiene una plantilla por su ID
     * 
     * @param idPlantilla ID de la plantilla
     * @return Plantilla encontrada
     */
    public Optional<PlantillaWhatsApp> obtenerPorId(Integer idPlantilla) {
        return plantillaRepository.findById(idPlantilla);
    }
    
    /**
     * Obtiene una plantilla por su nombre
     * 
     * @param nombre Nombre de la plantilla
     * @return Plantilla encontrada
     */
    @Cacheable(value = "plantillas", key = "#nombre")
    public Optional<PlantillaWhatsApp> obtenerPorNombre(String nombre) {
        return plantillaRepository.findByNombre(nombre);
    }
    
    /**
     * Obtiene una plantilla por su código de Meta
     * 
     * @param codigoMeta Código de la plantilla en Meta
     * @return Plantilla encontrada
     */
    public Optional<PlantillaWhatsApp> obtenerPorCodigoMeta(String codigoMeta) {
        return plantillaRepository.findByCodigoMeta(codigoMeta);
    }
    
    /**
     * Obtiene una plantilla por su template ID de Meta
     * 
     * @param templateId ID del template en Meta
     * @return Plantilla encontrada
     */
    public Optional<PlantillaWhatsApp> obtenerPorTemplateId(String templateId) {
        return plantillaRepository.findByTemplateId(templateId);
    }
    
    /**
     * Obtiene todas las plantillas activas
     * Se cachea para evitar consultas repetidas
     * 
     * @return Lista de plantillas activas
     */
    @Cacheable(value = "plantillas-activas")
    public List<PlantillaWhatsAppDTO> obtenerPlantillasActivas() {
        List<PlantillaWhatsApp> plantillas = plantillaRepository.findByActivoTrueOrderByNombre();
        return convertirADTOs(plantillas);
    }
    
    /**
     * Obtiene plantillas por estado y activas
     * 
     * @param estado Estado en Meta
     * @return Lista de plantillas que cumplen los criterios
     */
    public List<PlantillaWhatsAppDTO> obtenerPorEstado(EstadoMeta estado) {
        List<PlantillaWhatsApp> plantillas = plantillaRepository.findByEstadoMetaAndActivoTrue(estado);
        return convertirADTOs(plantillas);
    }
    
    /**
     * Obtiene todas las plantillas aprobadas y listas para usar
     * 
     * @return Lista de plantillas aprobadas
     */
    @Cacheable(value = "plantillas-aprobadas")
    public List<PlantillaWhatsAppDTO> obtenerPlantillasAprobadas() {
        return obtenerPorEstado(EstadoMeta.APPROVED);
    }
    
    /**
     * Obtiene plantillas por categoría
     * 
     * @param categoria Categoría de la plantilla
     * @return Lista de plantillas de esa categoría
     */
    public List<PlantillaWhatsAppDTO> obtenerPorCategoria(CategoriaPlantilla categoria) {
        // Filtrar las aprobadas por categoría
        return obtenerPlantillasAprobadas().stream()
                .filter(p -> categoria.name().equals(p.getCategoria()))
                .collect(Collectors.toList());
    }
    
    /**
     * Crea una nueva plantilla
     * 
     * @param plantilla Datos de la plantilla
     * @return Plantilla creada
     */
    @Transactional
    @CacheEvict(value = {"plantillas-activas", "plantillas-aprobadas"}, allEntries = true)
    public PlantillaWhatsApp crear(PlantillaWhatsApp plantilla) {
        log.info("Creando plantilla: {}", plantilla.getNombre());
        
        // Validar que no exista
        if (plantillaRepository.existsByNombre(plantilla.getNombre())) {
            throw new IllegalArgumentException("Ya existe una plantilla con el nombre: " + plantilla.getNombre());
        }
        
        // Guardar
        PlantillaWhatsApp creada = plantillaRepository.save(plantilla);
        log.info("Plantilla creada exitosamente con ID: {}", creada.getIdPlantilla());
        
        return creada;
    }
    
    /**
     * Actualiza una plantilla existente
     * 
     * @param idPlantilla ID de la plantilla
     * @param plantilla Datos actualizados
     * @return Plantilla actualizada
     */
    @Transactional
    @CacheEvict(value = {"plantillas", "plantillas-activas", "plantillas-aprobadas"}, allEntries = true)
    public Optional<PlantillaWhatsApp> actualizar(Integer idPlantilla, PlantillaWhatsApp plantilla) {
        Optional<PlantillaWhatsApp> existente = plantillaRepository.findById(idPlantilla);
        
        if (existente.isPresent()) {
            PlantillaWhatsApp actualizada = existente.get();
            
            // Actualizar campos permitidos
            actualizada.setNombre(plantilla.getNombre());
            actualizada.setContenido(plantilla.getContenido());
            actualizada.setParametros(plantilla.getParametros());
            actualizada.setCategoria(plantilla.getCategoria());
            actualizada.setIdioma(plantilla.getIdioma());
            
            plantillaRepository.save(actualizada);
            log.info("Plantilla {} actualizada exitosamente", idPlantilla);
            
            return Optional.of(actualizada);
        }
        
        return Optional.empty();
    }
    
    /**
     * Marca una plantilla como aprobada por Meta
     * 
     * @param idPlantilla ID de la plantilla
     * @param templateId ID del template en Meta
     * @return Plantilla actualizada
     */
    @Transactional
    @CacheEvict(value = {"plantillas", "plantillas-activas", "plantillas-aprobadas"}, allEntries = true)
    public Optional<PlantillaWhatsApp> marcarComoAprobada(Integer idPlantilla, String templateId) {
        Optional<PlantillaWhatsApp> plantillaOpt = plantillaRepository.findById(idPlantilla);
        
        if (plantillaOpt.isPresent()) {
            PlantillaWhatsApp plantilla = plantillaOpt.get();
            plantilla.marcarComoAprobada();
            plantilla.setTemplateId(templateId);
            plantillaRepository.save(plantilla);
            
            log.info("Plantilla {} marcada como aprobada con template ID: {}", idPlantilla, templateId);
            return Optional.of(plantilla);
        }
        
        return Optional.empty();
    }
    
    /**
     * Marca una plantilla como rechazada por Meta
     * 
     * @param idPlantilla ID de la plantilla
     * @return Plantilla actualizada
     */
    @Transactional
    @CacheEvict(value = {"plantillas", "plantillas-activas", "plantillas-aprobadas"}, allEntries = true)
    public Optional<PlantillaWhatsApp> marcarComoRechazada(Integer idPlantilla) {
        Optional<PlantillaWhatsApp> plantillaOpt = plantillaRepository.findById(idPlantilla);
        
        if (plantillaOpt.isPresent()) {
            PlantillaWhatsApp plantilla = plantillaOpt.get();
            plantilla.setEstadoMeta(EstadoMeta.REJECTED);
            plantillaRepository.save(plantilla);
            
            log.warn("Plantilla {} marcada como rechazada", idPlantilla);
            return Optional.of(plantilla);
        }
        
        return Optional.empty();
    }
    
    /**
     * Activa una plantilla
     * 
     * @param idPlantilla ID de la plantilla
     * @return Plantilla actualizada
     */
    @Transactional
    @CacheEvict(value = {"plantillas-activas", "plantillas-aprobadas"}, allEntries = true)
    public Optional<PlantillaWhatsApp> activar(Integer idPlantilla) {
        Optional<PlantillaWhatsApp> plantillaOpt = plantillaRepository.findById(idPlantilla);
        
        if (plantillaOpt.isPresent()) {
            PlantillaWhatsApp plantilla = plantillaOpt.get();
            plantilla.activar();
            plantillaRepository.save(plantilla);
            
            log.info("Plantilla {} activada", idPlantilla);
            return Optional.of(plantilla);
        }
        
        return Optional.empty();
    }
    
    /**
     * Desactiva una plantilla
     * 
     * @param idPlantilla ID de la plantilla
     * @return Plantilla actualizada
     */
    @Transactional
    @CacheEvict(value = {"plantillas-activas", "plantillas-aprobadas"}, allEntries = true)
    public Optional<PlantillaWhatsApp> desactivar(Integer idPlantilla) {
        Optional<PlantillaWhatsApp> plantillaOpt = plantillaRepository.findById(idPlantilla);
        
        if (plantillaOpt.isPresent()) {
            PlantillaWhatsApp plantilla = plantillaOpt.get();
            plantilla.desactivar();
            plantillaRepository.save(plantilla);
            
            log.info("Plantilla {} desactivada", idPlantilla);
            return Optional.of(plantilla);
        }
        
        return Optional.empty();
    }
    
    /**
     * Elimina una plantilla
     * 
     * @param idPlantilla ID de la plantilla
     */
    @Transactional
    @CacheEvict(value = {"plantillas", "plantillas-activas", "plantillas-aprobadas"}, allEntries = true)
    public void eliminar(Integer idPlantilla) {
        plantillaRepository.deleteById(idPlantilla);
        log.info("Plantilla {} eliminada", idPlantilla);
    }
    
    /**
     * Valida si una plantilla está lista para usar
     * 
     * @param idPlantilla ID de la plantilla
     * @return true si está lista, false si no
     */
    public boolean estaListaParaUsar(Integer idPlantilla) {
        Optional<PlantillaWhatsApp> plantillaOpt = plantillaRepository.findById(idPlantilla);
        return plantillaOpt.isPresent() && plantillaOpt.get().estaListaParaUsar();
    }
    
    /**
     * Valida parámetros de una plantilla
     * 
     * @param nombrePlantilla Nombre de la plantilla
     * @param parametros Parámetros a validar
     * @return true si los parámetros son válidos
     */
    public boolean validarParametros(String nombrePlantilla, List<String> parametros) {
        Optional<PlantillaWhatsApp> plantillaOpt = obtenerPorNombre(nombrePlantilla);
        
        if (plantillaOpt.isEmpty()) {
            log.warn("Plantilla no encontrada: {}", nombrePlantilla);
            return false;
        }
        
        PlantillaWhatsApp plantilla = plantillaOpt.get();
        String parametrosPlantilla = plantilla.getParametros();
        
        if (parametrosPlantilla == null || parametrosPlantilla.isEmpty()) {
            // Plantilla sin parámetros
            return parametros == null || parametros.isEmpty();
        }
        
        // Contar parámetros esperados
        int cantidadEsperada = parametrosPlantilla.split(",").length;
        int cantidadRecibida = parametros != null ? parametros.size() : 0;
        
        boolean valido = cantidadEsperada == cantidadRecibida;
        
        if (!valido) {
            log.warn("Parámetros inválidos para plantilla {}: esperados={}, recibidos={}", 
                    nombrePlantilla, cantidadEsperada, cantidadRecibida);
        }
        
        return valido;
    }
    
    /**
     * Limpia el caché de plantillas
     */
    @CacheEvict(value = {"plantillas", "plantillas-activas", "plantillas-aprobadas"}, allEntries = true)
    public void limpiarCache() {
        log.info("Caché de plantillas limpiado");
    }
    
    /**
     * Obtiene todas las plantillas (sin filtro)
     * 
     * @return Lista completa de plantillas
     */
    public List<PlantillaWhatsAppDTO> obtenerTodas() {
        List<PlantillaWhatsApp> plantillas = plantillaRepository.findAll();
        return convertirADTOs(plantillas);
    }
    
    // ========================================
    // MÉTODOS PRIVADOS
    // ========================================
    
    /**
     * Convierte lista de entidades a DTOs
     */
    private List<PlantillaWhatsAppDTO> convertirADTOs(List<PlantillaWhatsApp> plantillas) {
        return plantillas.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Convierte una entidad a DTO
     */
    private PlantillaWhatsAppDTO convertirADTO(PlantillaWhatsApp plantilla) {
        // Convertir parámetros de String CSV a List
        List<String> parametros = null;
        if (plantilla.getParametros() != null && !plantilla.getParametros().isEmpty()) {
            parametros = Arrays.asList(plantilla.getParametros().split(","));
        }
        
        return PlantillaWhatsAppDTO.builder()
                .idPlantilla(plantilla.getIdPlantilla())
                .nombre(plantilla.getNombre())
                .codigoMeta(plantilla.getCodigoMeta())
                .categoria(plantilla.getCategoria() != null ? plantilla.getCategoria().name() : null)
                .idioma(plantilla.getIdioma())
                .contenido(plantilla.getContenido())
                .parametros(parametros)
                .estadoMeta(plantilla.getEstadoMeta() != null ? plantilla.getEstadoMeta().name() : null)
                .templateId(plantilla.getTemplateId())
                .activo(plantilla.getActivo())
                .fechaCreacion(plantilla.getCreateDate() != null ? plantilla.getCreateDate().toLocalDateTime() : null)
                .fechaAprobacion(plantilla.getFechaAprobacion())
                .fechaActualizacion(plantilla.getUpdateDate() != null ? plantilla.getUpdateDate().toLocalDateTime() : null)
                .build();
    }
}
