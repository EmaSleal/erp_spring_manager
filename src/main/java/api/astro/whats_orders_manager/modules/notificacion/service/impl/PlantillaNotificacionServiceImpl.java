package api.astro.whats_orders_manager.modules.notificacion.service.impl;

import api.astro.whats_orders_manager.modules.notificacion.model.PlantillaNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.repository.PlantillaNotificacionRepository;
import api.astro.whats_orders_manager.modules.notificacion.service.PlantillaNotificacionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * PLANTILLA NOTIFICACIÓN SERVICE IMPLEMENTATION
 * ERP Orders Manager - Sprint 4 Fase 3
 * ============================================================================
 * Implementación del servicio de gestión de plantillas de notificaciones.
 * 
 * Responsabilidades:
 * - CRUD completo de plantillas
 * - Sistema de versionado automático
 * - Procesamiento de variables dinámicas {{variable}}
 * - Gestión de plantillas predeterminadas y del sistema
 * - Generación de vista previa con datos de ejemplo
 * ============================================================================
 */
@Slf4j
@Service
public class PlantillaNotificacionServiceImpl implements PlantillaNotificacionService {

    @Autowired
    private PlantillaNotificacionRepository plantillaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    // ==================== CRUD ====================

    @Override
    public Optional<PlantillaNotificacion> findById(Integer idPlantilla) {
        return plantillaRepository.findById(idPlantilla);
    }

    @Override
    public Optional<PlantillaNotificacion> findByCodigo(String codigo) {
        return plantillaRepository.findByCodigo(codigo);
    }

    @Override
    public Page<PlantillaNotificacion> findAll(Pageable pageable) {
        return plantillaRepository.findAll(pageable);
    }

    @Override
    public Page<PlantillaNotificacion> findActivas(Pageable pageable) {
        return plantillaRepository.findByActivaTrueOrderByNombreAsc(pageable);
    }

    @Override
    @Transactional
    public PlantillaNotificacion crear(PlantillaNotificacion plantilla) {
        log.info("📝 Creando nueva plantilla: {}", plantilla.getNombre());

        // Asignar versión 1 si no existe
        if (plantilla.getVersion() == null) {
            plantilla.setVersion(1);
        }

        // Validar que no exista otra con el mismo nombre y versión
        Optional<PlantillaNotificacion> existente = 
            plantillaRepository.findByNombreAndVersion(plantilla.getNombre(), plantilla.getVersion());
        
        if (existente.isPresent()) {
            throw new IllegalArgumentException(
                "Ya existe una plantilla con nombre '" + plantilla.getNombre() + 
                "' y versión " + plantilla.getVersion()
            );
        }

        // Generar código único si no tiene
        if (plantilla.getCodigo() == null || plantilla.getCodigo().isEmpty()) {
            plantilla.setCodigo(generarCodigoUnico(plantilla.getNombre()));
        }

        // Extraer variables automáticamente si no se especificaron
        if (plantilla.getVariablesDisponibles() == null || plantilla.getVariablesDisponibles().isEmpty()) {
            List<String> variables = plantilla.extraerVariables();
            plantilla.setVariablesDisponibles(String.join(",", variables));
        }

        PlantillaNotificacion guardada = plantillaRepository.save(plantilla);
        log.info("✅ Plantilla creada con ID: {}", guardada.getIdPlantilla());

        return guardada;
    }

    @Override
    @Transactional
    public PlantillaNotificacion actualizar(Integer idPlantilla, PlantillaNotificacion plantilla) {
        log.info("📝 Actualizando plantilla ID: {}", idPlantilla);

        PlantillaNotificacion existente = plantillaRepository.findById(idPlantilla)
            .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + idPlantilla));

        // No se puede editar plantilla del sistema
        if (Boolean.TRUE.equals(existente.getPlantillaSistema())) {
            throw new IllegalStateException("No se puede editar una plantilla del sistema");
        }

        // Actualizar campos
        existente.setDescripcion(plantilla.getDescripcion());
        existente.setAsunto(plantilla.getAsunto());
        existente.setContenido(plantilla.getContenido());
        existente.setTextoBoton(plantilla.getTextoBoton());
        existente.setUrlAccion(plantilla.getUrlAccion());

        // Actualizar variables disponibles
        List<String> variables = existente.extraerVariables();
        existente.setVariablesDisponibles(String.join(",", variables));

        PlantillaNotificacion actualizada = plantillaRepository.save(existente);
        log.info("✅ Plantilla actualizada exitosamente");

        return actualizada;
    }

    @Override
    @Transactional
    public boolean eliminar(Integer idPlantilla) {
        log.info("🗑️ Eliminando plantilla ID: {}", idPlantilla);

        PlantillaNotificacion plantilla = plantillaRepository.findById(idPlantilla)
            .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + idPlantilla));

        // No se puede eliminar plantilla del sistema
        if (Boolean.TRUE.equals(plantilla.getPlantillaSistema())) {
            throw new IllegalStateException("No se puede eliminar una plantilla del sistema");
        }

        plantillaRepository.delete(plantilla);
        log.info("✅ Plantilla eliminada exitosamente");

        return true;
    }

    // ==================== BÚSQUEDAS ESPECÍFICAS ====================

    @Override
    public Page<PlantillaNotificacion> findByTipo(TipoNotificacion tipo, Pageable pageable) {
        List<PlantillaNotificacion> lista = plantillaRepository.findByTipoOrderByNombreAsc(tipo);
        return convertirAPage(lista, pageable);
    }

    @Override
    public Page<PlantillaNotificacion> findByCanal(CanalNotificacion canal, Pageable pageable) {
        List<PlantillaNotificacion> lista = plantillaRepository.findByCanalOrderByNombreAsc(canal);
        return convertirAPage(lista, pageable);
    }

    @Override
    public Page<PlantillaNotificacion> findByTipoAndCanal(
            TipoNotificacion tipo,
            CanalNotificacion canal,
            Pageable pageable) {
        List<PlantillaNotificacion> lista = plantillaRepository.findByTipoAndCanalOrderByNombreAsc(tipo, canal);
        return convertirAPage(lista, pageable);
    }

    @Override
    public List<PlantillaNotificacion> findPlantillasSistema() {
        return plantillaRepository.findByPlantillaSistemaTrueOrderByNombreAsc();
    }

    @Override
    public List<PlantillaNotificacion> findPlantillasPersonalizadas() {
        return plantillaRepository.findByPlantillaSistemaFalseOrderByNombreAsc();
    }

    // ==================== VERSIONADO ====================

    @Override
    public List<PlantillaNotificacion> findVersiones(String nombre) {
        return plantillaRepository.findByNombreOrderByVersionDesc(nombre);
    }

    @Override
    public Optional<PlantillaNotificacion> findUltimaVersion(String nombre) {
        return plantillaRepository.findUltimaVersion(nombre);
    }

    @Override
    public Optional<PlantillaNotificacion> findVersion(String nombre, Integer version) {
        return plantillaRepository.findByNombreAndVersion(nombre, version);
    }

    @Override
    @Transactional
    public PlantillaNotificacion crearNuevaVersion(Integer idPlantillaBase) {
        log.info("📋 Creando nueva versión de plantilla ID: {}", idPlantillaBase);

        PlantillaNotificacion base = plantillaRepository.findById(idPlantillaBase)
            .orElseThrow(() -> new IllegalArgumentException("Plantilla base no encontrada: " + idPlantillaBase));

        PlantillaNotificacion nuevaVersion = base.crearNuevaVersion();
        PlantillaNotificacion guardada = plantillaRepository.save(nuevaVersion);

        log.info("✅ Nueva versión {} creada con ID: {}", guardada.getVersion(), guardada.getIdPlantilla());

        return guardada;
    }

    // ==================== PLANTILLAS PREDETERMINADAS ====================

    @Override
    public Optional<PlantillaNotificacion> obtenerPlantillaPredeterminada(
            TipoNotificacion tipo,
            CanalNotificacion canal) {
        return plantillaRepository.findPlantillaPredeterminada(tipo, canal);
    }

    @Override
    @Transactional
    public boolean establecerComoPredeterminada(Integer idPlantilla) {
        log.info("⭐ Estableciendo plantilla {} como predeterminada", idPlantilla);

        PlantillaNotificacion plantilla = plantillaRepository.findById(idPlantilla)
            .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + idPlantilla));

        // Quitar predeterminada anterior para el mismo tipo y canal
        plantillaRepository.quitarPredeterminadasPreviasParaTipoCanal(
            idPlantilla,
            plantilla.getTipo(),
            plantilla.getCanal()
        );

        // Establecer como predeterminada
        int updated = plantillaRepository.establecerComoPredeterminada(idPlantilla);

        log.info("✅ Plantilla establecida como predeterminada");

        return updated > 0;
    }

    @Override
    public boolean existePlantillaPredeterminada(TipoNotificacion tipo, CanalNotificacion canal) {
        return plantillaRepository.existePlantillaPredeterminada(tipo, canal);
    }

    // ==================== ACTIVACIÓN/DESACTIVACIÓN ====================

    @Override
    @Transactional
    public boolean activar(Integer idPlantilla) {
        log.debug("Activando plantilla ID: {}", idPlantilla);
        int updated = plantillaRepository.activarPlantilla(idPlantilla);
        return updated > 0;
    }

    @Override
    @Transactional
    public boolean desactivar(Integer idPlantilla) {
        log.debug("Desactivando plantilla ID: {}", idPlantilla);
        int updated = plantillaRepository.desactivarPlantilla(idPlantilla);
        return updated > 0;
    }

    // ==================== PROCESAMIENTO DE CONTENIDO ====================

    @Override
    public String procesarContenido(Integer idPlantilla, Map<String, Object> variables) {
        PlantillaNotificacion plantilla = plantillaRepository.findById(idPlantilla)
            .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + idPlantilla));

        return plantilla.procesarContenido(variables);
    }

    @Override
    public String procesarAsunto(Integer idPlantilla, Map<String, Object> variables) {
        PlantillaNotificacion plantilla = plantillaRepository.findById(idPlantilla)
            .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + idPlantilla));

        return plantilla.procesarAsunto(variables);
    }

    @Override
    public Map<String, String> procesarPlantilla(Integer idPlantilla, Map<String, Object> variables) {
        PlantillaNotificacion plantilla = plantillaRepository.findById(idPlantilla)
            .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + idPlantilla));

        Map<String, String> resultado = new HashMap<>();
        resultado.put("asunto", plantilla.procesarAsunto(variables));
        resultado.put("contenido", plantilla.procesarContenido(variables));

        return resultado;
    }

    @Override
    public List<String> obtenerVariablesDisponibles(Integer idPlantilla) {
        PlantillaNotificacion plantilla = plantillaRepository.findById(idPlantilla)
            .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + idPlantilla));

        return plantilla.extraerVariables();
    }

    @Override
    public List<String> obtenerVariablesNecesarias(Integer idPlantilla, Map<String, Object> variables) {
        List<String> disponibles = obtenerVariablesDisponibles(idPlantilla);
        
        return disponibles.stream()
            .filter(var -> !variables.containsKey(var))
            .collect(Collectors.toList());
    }

    @Override
    public boolean validarVariables(Integer idPlantilla, Map<String, Object> variables) {
        PlantillaNotificacion plantilla = plantillaRepository.findById(idPlantilla)
            .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + idPlantilla));

        return plantilla.tieneTodasLasVariables(variables);
    }

    // ==================== DATOS DE EJEMPLO ====================

    @Override
    public String obtenerDatosEjemplo(Integer idPlantilla) {
        PlantillaNotificacion plantilla = plantillaRepository.findById(idPlantilla)
            .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + idPlantilla));

        return plantilla.getDatosEjemplo();
    }

    @Override
    @Transactional
    public boolean actualizarDatosEjemplo(Integer idPlantilla, String datosEjemplo) {
        log.debug("Actualizando datos de ejemplo para plantilla ID: {}", idPlantilla);

        PlantillaNotificacion plantilla = plantillaRepository.findById(idPlantilla)
            .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + idPlantilla));

        plantilla.setDatosEjemplo(datosEjemplo);
        plantillaRepository.save(plantilla);

        return true;
    }

    @Override
    public Map<String, String> generarVistaPrevia(Integer idPlantilla) {
        log.debug("Generando vista previa de plantilla ID: {}", idPlantilla);

        PlantillaNotificacion plantilla = plantillaRepository.findById(idPlantilla)
            .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + idPlantilla));

        // Convertir JSON de ejemplo a Map
        Map<String, Object> variables = new HashMap<>();
        
        if (plantilla.getDatosEjemplo() != null && !plantilla.getDatosEjemplo().isEmpty()) {
            try {
                variables = objectMapper.readValue(
                    plantilla.getDatosEjemplo(),
                    new TypeReference<Map<String, Object>>() {}
                );
            } catch (Exception e) {
                log.warn("No se pudieron cargar datos de ejemplo: {}", e.getMessage());
                // Usar variables por defecto
                variables = generarVariablesPorDefecto(plantilla);
            }
        } else {
            variables = generarVariablesPorDefecto(plantilla);
        }

        // Procesar plantilla
        Map<String, String> resultado = new HashMap<>();
        resultado.put("asunto", plantilla.procesarAsunto(variables));
        resultado.put("contenido", plantilla.procesarContenido(variables));

        return resultado;
    }

    // ==================== ESTADÍSTICAS ====================

    @Override
    public long countByTipo(TipoNotificacion tipo) {
        return plantillaRepository.countByTipo(tipo);
    }

    @Override
    public long countByCanal(CanalNotificacion canal) {
        return plantillaRepository.countByCanal(canal);
    }

    @Override
    public Map<TipoNotificacion, Long> obtenerEstadisticasPorTipo() {
        List<Object[]> resultados = plantillaRepository.obtenerEstadisticasPorTipo();
        Map<TipoNotificacion, Long> estadisticas = new HashMap<>();
        
        for (Object[] resultado : resultados) {
            TipoNotificacion tipo = (TipoNotificacion) resultado[0];
            Long count = (Long) resultado[1];
            estadisticas.put(tipo, count);
        }
        
        return estadisticas;
    }

    @Override
    public Map<CanalNotificacion, Long> obtenerEstadisticasPorCanal() {
        List<Object[]> resultados = plantillaRepository.obtenerEstadisticasPorCanal();
        Map<CanalNotificacion, Long> estadisticas = new HashMap<>();
        
        for (Object[] resultado : resultados) {
            CanalNotificacion canal = (CanalNotificacion) resultado[0];
            Long count = (Long) resultado[1];
            estadisticas.put(canal, count);
        }
        
        return estadisticas;
    }

    @Override
    public long countActivas() {
        return plantillaRepository.countByActivaTrue();
    }

    @Override
    public long countPlantillasSistema() {
        return plantillaRepository.countByPlantillaSistemaTrue();
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Genera un código único para una plantilla basado en su nombre
     */
    private String generarCodigoUnico(String nombre) {
        String codigo = nombre.toUpperCase()
            .replaceAll("[^A-Z0-9]", "_")
            .replaceAll("_+", "_");
        
        // Verificar si existe
        int contador = 1;
        String codigoFinal = codigo;
        
        while (plantillaRepository.findByCodigo(codigoFinal).isPresent()) {
            codigoFinal = codigo + "_" + contador;
            contador++;
        }
        
        return codigoFinal;
    }

    /**
     * Genera variables por defecto para vista previa
     */
    private Map<String, Object> generarVariablesPorDefecto(PlantillaNotificacion plantilla) {
        Map<String, Object> variables = new HashMap<>();
        List<String> variablesNecesarias = plantilla.extraerVariables();
        
        for (String variable : variablesNecesarias) {
            switch (variable) {
                case "nombre":
                case "nombreCliente":
                case "nombreUsuario":
                    variables.put(variable, "Juan Pérez");
                    break;
                case "numero":
                case "numeroFactura":
                    variables.put(variable, "FAC-2024-001");
                    break;
                case "monto":
                case "total":
                    variables.put(variable, "$1,234.56");
                    break;
                case "fecha":
                case "fechaVencimiento":
                    variables.put(variable, "31/12/2024");
                    break;
                case "empresa":
                case "nombreEmpresa":
                    variables.put(variable, "Mi Empresa S.A.");
                    break;
                case "producto":
                case "nombreProducto":
                    variables.put(variable, "Producto Demo");
                    break;
                case "cantidad":
                    variables.put(variable, "10");
                    break;
                case "dias":
                    variables.put(variable, "7");
                    break;
                default:
                    variables.put(variable, "[" + variable + "]");
            }
        }
        
        return variables;
    }

    /**
     * Convierte una lista a Page aplicando paginación manual
     */
    private <T> Page<T> convertirAPage(List<T> lista, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), lista.size());
        
        if (start > lista.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, lista.size());
        }
        
        List<T> subLista = lista.subList(start, end);
        return new PageImpl<>(subLista, pageable, lista.size());
    }
}
