package api.astro.whats_orders_manager.services;

import api.astro.whats_orders_manager.models.Empresa;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

/**
 * Servicio para gestión de Empresa
 * Maneja la lógica de negocio relacionada con la configuración de la empresa.
 * 
 * @author Astro Dev Team
 * @version 1.0
 * @since Sprint 2
 */
public interface EmpresaService {

    /**
     * Busca la empresa por ID
     * 
     * @param id ID de la empresa
     * @return Optional con la empresa si existe
     */
    Optional<Empresa> findById(Integer id);

    /**
     * Obtiene la empresa activa (principal)
     * Solo debe existir una empresa activa en el sistema
     * 
     * @return Optional con la empresa activa, o vacío si no existe
     */
    Optional<Empresa> findEmpresaActiva();

    /**
     * Obtiene la empresa activa, o crea una por defecto si no existe
     * 
     * @return La empresa activa del sistema
     */
    Empresa getEmpresaPrincipal();

    /**
     * Guarda o actualiza una empresa
     * Si es una actualización, preserva los archivos existentes
     * 
     * @param empresa La empresa a guardar
     * @param usuarioId ID del usuario que realiza la operación
     * @return La empresa guardada
     */
    Empresa save(Empresa empresa, Integer usuarioId);

    /**
     * Actualiza los datos de la empresa
     * 
     * @param empresa La empresa con los datos actualizados
     * @param usuarioId ID del usuario que realiza la operación
     * @return La empresa actualizada
     */
    Empresa update(Empresa empresa, Integer usuarioId);

    /**
     * Guarda el archivo del logo y actualiza el registro de empresa
     * 
     * @param empresaId ID de la empresa
     * @param file Archivo del logo
     * @param usuarioId ID del usuario que realiza la operación
     * @return La empresa actualizada con la ruta del logo
     * @throws IOException Si hay error al guardar el archivo
     */
    Empresa guardarLogo(Integer empresaId, MultipartFile file, Integer usuarioId) throws IOException;

    /**
     * Guarda el archivo del favicon y actualiza el registro de empresa
     * 
     * @param empresaId ID de la empresa
     * @param file Archivo del favicon
     * @param usuarioId ID del usuario que realiza la operación
     * @return La empresa actualizada con la ruta del favicon
     * @throws IOException Si hay error al guardar el archivo
     */
    Empresa guardarFavicon(Integer empresaId, MultipartFile file, Integer usuarioId) throws IOException;

    /**
     * Elimina el logo de la empresa
     * 
     * @param empresaId ID de la empresa
     * @param usuarioId ID del usuario que realiza la operación
     * @return La empresa actualizada sin logo
     * @throws IOException Si hay error al eliminar el archivo
     */
    Empresa eliminarLogo(Integer empresaId, Integer usuarioId) throws IOException;

    /**
     * Elimina el favicon de la empresa
     * 
     * @param empresaId ID de la empresa
     * @param usuarioId ID del usuario que realiza la operación
     * @return La empresa actualizada sin favicon
     * @throws IOException Si hay error al eliminar el archivo
     */
    Empresa eliminarFavicon(Integer empresaId, Integer usuarioId) throws IOException;

    /**
     * Verifica si existe una empresa activa en el sistema
     * 
     * @return true si existe al menos una empresa activa
     */
    boolean existeEmpresaActiva();

    /**
     * Valida que los datos de la empresa sean correctos
     * 
     * @param empresa La empresa a validar
     * @return true si es válida, false en caso contrario
     */
    boolean validarEmpresa(Empresa empresa);
}
