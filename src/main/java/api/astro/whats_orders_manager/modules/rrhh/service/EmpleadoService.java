package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.dto.EmpleadoDTO;
import api.astro.whats_orders_manager.modules.rrhh.model.Empleado;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface EmpleadoService {

    /**
     * Creates a new Empleado. Validates cedula uniqueness.
     * Sets estado=ACTIVO and activo=true.
     *
     * @throws IllegalArgumentException if cedula already exists
     */
    Empleado crear(EmpleadoDTO dto);

    /**
     * Updates an existing Empleado.
     *
     * @throws java.util.NoSuchElementException if not found
     */
    Empleado actualizar(Long id, EmpleadoDTO dto);

    /**
     * Soft-delete: sets activo=false, estado=BAJA_DEFINITIVA,
     * fechaSalida=fecha, motivoSalida=motivo.
     *
     * @throws java.util.NoSuchElementException if not found
     */
    void darDeBaja(Long id, LocalDate fechaSalida, String motivo);

    /**
     * @throws java.util.NoSuchElementException if not found
     */
    Empleado findById(Long id);

    Page<Empleado> findAll(Pageable pageable);

    List<Empleado> findActivos();

    long countActivos();

    /**
     * Links the given employee to the given user.
     * If another employee is already linked to this user, their link is cleared first.
     * Both operations are in a single transaction.
     */
    void vincularUsuario(Long empleadoId, Integer usuarioId);

    /**
     * Clears the usuario link from whichever employee holds this usuarioId.
     * No-op if no employee is linked to this user.
     */
    void desvincularUsuario(Integer usuarioId);

    /**
     * Returns all active users not already linked to another employee.
     * If excludeEmpleadoId is non-null, the user linked to that employee is also included
     * (so the current value remains selectable in edit forms).
     *
     * @param excludeEmpleadoId the employee being edited (null for new employee forms)
     */
    List<Usuario> findUsuariosDisponibles(Long excludeEmpleadoId);

    /**
     * Returns all active employees not already linked to another user.
     * If excludeUsuarioId is non-null, the employee linked to that user is also included
     * (so the current value remains selectable in edit forms).
     *
     * @param excludeUsuarioId the user being edited (null for new user forms)
     */
    List<Empleado> findEmpleadosDisponibles(Integer excludeUsuarioId);
}
