package api.astro.whats_orders_manager.services.impl;

import api.astro.whats_orders_manager.models.Empresa;
import api.astro.whats_orders_manager.repositories.EmpresaRepository;
import api.astro.whats_orders_manager.services.EmpresaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación del servicio de Empresa
 * Maneja toda la lógica de negocio relacionada con la configuración de la empresa.
 * 
 * @author Astro Dev Team
 * @version 1.0
 * @since Sprint 2
 */
@Service
@Slf4j
@Transactional
public class EmpresaServiceImpl implements EmpresaService {

    @Autowired
    private EmpresaRepository empresaRepository;

    /**
     * Directorio base para guardar archivos de empresa (logo, favicon)
     * Configurado en application.yml
     */
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Subdirectorio específico para archivos de empresa
     */
    private static final String EMPRESA_SUBDIR = "empresa";

    /**
     * Extensiones permitidas para logo y favicon
     */
    private static final String[] ALLOWED_EXTENSIONS = {".png", ".jpg", ".jpeg", ".svg", ".ico"};

    /**
     * Tamaño máximo de archivo en bytes (2MB)
     */
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    @Override
    @Transactional(readOnly = true)
    public Optional<Empresa> findById(Integer id) {
        log.debug("Buscando empresa con ID: {}", id);
        return empresaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Empresa> findEmpresaActiva() {
        log.debug("Buscando empresa activa");
        return empresaRepository.findEmpresaActiva();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "empresa", key = "'principal'")
    public Empresa getEmpresaPrincipal() {
        log.debug("Obteniendo empresa principal (sin caché)");
        
        Optional<Empresa> empresaOpt = findEmpresaActiva();
        
        if (empresaOpt.isPresent()) {
            return empresaOpt.get();
        }
        
        // Si no existe, crear una por defecto
        log.info("No existe empresa activa, creando una por defecto");
        Empresa empresaDefault = new Empresa();
        empresaDefault.setNombreEmpresa("Mi Empresa S.A.C.");
        empresaDefault.setNombreComercial("Mi Empresa");
        empresaDefault.setActivo(true);
        
        return empresaRepository.save(empresaDefault);
    }

    @Override
    @CacheEvict(value = "empresa", allEntries = true)
    public Empresa save(Empresa empresa, Integer usuarioId) {
        log.info("Guardando empresa: {} (invalidando caché)", empresa.getNombreEmpresa());
        
        // Validar datos
        if (!validarEmpresa(empresa)) {
            throw new IllegalArgumentException("Los datos de la empresa no son válidos");
        }
        
        // Si ya existe una empresa activa y esta es nueva, desactivar la anterior
        if (empresa.getIdEmpresa() == null && existeEmpresaActiva()) {
            log.warn("Ya existe una empresa activa. Se desactivará la anterior.");
            Optional<Empresa> empresaActivaOpt = findEmpresaActiva();
            empresaActivaOpt.ifPresent(empresaActiva -> {
                empresaActiva.setActivo(false);
                empresaRepository.save(empresaActiva);
            });
        }
        
        empresa.setActivo(true);
        
        return empresaRepository.save(empresa);
    }

    @Override
    @CacheEvict(value = "empresa", allEntries = true)
    public Empresa update(Empresa empresa, Integer usuarioId) {
        log.info("Actualizando empresa ID: {} (invalidando caché)", empresa.getIdEmpresa());
        
        if (empresa.getIdEmpresa() == null) {
            throw new IllegalArgumentException("El ID de la empresa no puede ser nulo para actualizar");
        }
        
        // Verificar que existe
        Empresa empresaExistente = empresaRepository.findById(empresa.getIdEmpresa())
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada con ID: " + empresa.getIdEmpresa()));
        
        // Validar datos
        if (!validarEmpresa(empresa)) {
            throw new IllegalArgumentException("Los datos de la empresa no son válidos");
        }
        
        // Preservar archivos si no se proporcionaron nuevos
        if (empresa.getLogo() == null) {
            empresa.setLogo(empresaExistente.getLogo());
        }
        if (empresa.getFavicon() == null) {
            empresa.setFavicon(empresaExistente.getFavicon());
        }
        
        return empresaRepository.save(empresa);
    }

    @Override
    @CacheEvict(value = "empresa", allEntries = true)
    public Empresa guardarLogo(Integer empresaId, MultipartFile file, Integer usuarioId) throws IOException {
        log.info("Guardando logo para empresa ID: {} (invalidando caché)", empresaId);
        
        // Validar archivo
        validarArchivo(file);
        
        // Buscar empresa
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada con ID: " + empresaId));
        
        // Eliminar logo anterior si existe
        if (empresa.getLogo() != null) {
            eliminarArchivo(empresa.getLogo());
        }
        
        // Guardar nuevo logo
        String nombreArchivo = guardarArchivo(file, "logo");
        empresa.setLogo(nombreArchivo);
        
        return empresaRepository.save(empresa);
    }

    @Override
    @CacheEvict(value = "empresa", allEntries = true)
    public Empresa guardarFavicon(Integer empresaId, MultipartFile file, Integer usuarioId) throws IOException {
        log.info("Guardando favicon para empresa ID: {} (invalidando caché)", empresaId);
        
        // Validar archivo
        validarArchivo(file);
        
        // Buscar empresa
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada con ID: " + empresaId));
        
        // Eliminar favicon anterior si existe
        if (empresa.getFavicon() != null) {
            eliminarArchivo(empresa.getFavicon());
        }
        
        // Guardar nuevo favicon
        String nombreArchivo = guardarArchivo(file, "favicon");
        empresa.setFavicon(nombreArchivo);
        
        return empresaRepository.save(empresa);
    }

    @Override
    @CacheEvict(value = "empresa", allEntries = true)
    public Empresa eliminarLogo(Integer empresaId, Integer usuarioId) throws IOException {
        log.info("Eliminando logo de empresa ID: {} (invalidando caché)", empresaId);
        
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada con ID: " + empresaId));
        
        if (empresa.getLogo() != null) {
            eliminarArchivo(empresa.getLogo());
            empresa.setLogo(null);
            empresaRepository.save(empresa);
        }
        
        return empresa;
    }

    @Override
    @CacheEvict(value = "empresa", allEntries = true)
    public Empresa eliminarFavicon(Integer empresaId, Integer usuarioId) throws IOException {
        log.info("Eliminando favicon de empresa ID: {} (invalidando caché)", empresaId);
        
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada con ID: " + empresaId));
        
        if (empresa.getFavicon() != null) {
            eliminarArchivo(empresa.getFavicon());
            empresa.setFavicon(null);
            empresaRepository.save(empresa);
        }
        
        return empresa;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeEmpresaActiva() {
        return empresaRepository.existeEmpresaActiva();
    }

    @Override
    public boolean validarEmpresa(Empresa empresa) {
        if (empresa == null) {
            log.error("Empresa es nula");
            return false;
        }
        
        if (empresa.getNombreEmpresa() == null || empresa.getNombreEmpresa().trim().isEmpty()) {
            log.error("Nombre de empresa es obligatorio");
            return false;
        }
        
        if (empresa.getNombreEmpresa().length() > 200) {
            log.error("Nombre de empresa excede 200 caracteres");
            return false;
        }
        
        // Validar email si está presente
        if (empresa.getEmail() != null && !empresa.getEmail().isEmpty()) {
            if (!empresa.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                log.error("Email no es válido: {}", empresa.getEmail());
                return false;
            }
        }
        
        log.debug("Validación de empresa exitosa");
        return true;
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Valida que el archivo sea válido (tamaño y extensión)
     */
    private void validarArchivo(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }
        
        // Validar tamaño
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El archivo excede el tamaño máximo de 2MB");
        }
        
        // Validar extensión
        String nombreOriginal = file.getOriginalFilename();
        if (nombreOriginal == null) {
            throw new IllegalArgumentException("El nombre del archivo no es válido");
        }
        
        boolean extensionValida = false;
        for (String ext : ALLOWED_EXTENSIONS) {
            if (nombreOriginal.toLowerCase().endsWith(ext)) {
                extensionValida = true;
                break;
            }
        }
        
        if (!extensionValida) {
            throw new IllegalArgumentException("Extensión no permitida. Solo: .png, .jpg, .jpeg, .svg, .ico");
        }
    }

    /**
     * Guarda un archivo en el sistema de archivos
     * 
     * @param file Archivo a guardar
     * @param prefijo Prefijo para el nombre del archivo (logo, favicon)
     * @return Nombre del archivo guardado (solo nombre, no ruta completa)
     */
    private String guardarArchivo(MultipartFile file, String prefijo) throws IOException {
        // Crear directorio si no existe
        Path dirPath = Paths.get(uploadDir, EMPRESA_SUBDIR);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            log.info("Directorio creado: {}", dirPath.toAbsolutePath());
        }
        
        // Generar nombre único para el archivo
        String extension = getExtension(file.getOriginalFilename());
        String nombreArchivo = prefijo + "_" + UUID.randomUUID() + extension;
        
        // Guardar archivo
        Path archivoPath = dirPath.resolve(nombreArchivo);
        Files.copy(file.getInputStream(), archivoPath, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("Archivo guardado: {}", archivoPath.toAbsolutePath());
        
        // Retornar solo el nombre del archivo (no la ruta completa)
        return nombreArchivo;
    }

    /**
     * Elimina un archivo del sistema de archivos
     */
    private void eliminarArchivo(String nombreArchivo) throws IOException {
        if (nombreArchivo == null || nombreArchivo.isEmpty()) {
            return;
        }
        
        Path archivoPath = Paths.get(uploadDir, EMPRESA_SUBDIR, nombreArchivo);
        
        if (Files.exists(archivoPath)) {
            Files.delete(archivoPath);
            log.info("Archivo eliminado: {}", archivoPath.toAbsolutePath());
        } else {
            log.warn("Archivo no encontrado para eliminar: {}", archivoPath.toAbsolutePath());
        }
    }

    /**
     * Obtiene la extensión de un archivo
     */
    private String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        return (lastDot == -1) ? "" : filename.substring(lastDot);
    }
}
