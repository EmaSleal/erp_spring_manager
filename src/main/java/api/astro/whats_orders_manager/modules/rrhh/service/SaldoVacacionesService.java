package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.model.SaldoVacaciones;

import java.math.BigDecimal;

/**
 * Manages employee vacation balances per Código de Trabajo CR Art. 153.
 */
public interface SaldoVacacionesService {

    /**
     * Creates a new SaldoVacaciones record for the given employee with
     * diasGenerados=0, diasDisfrutados=0, and fechaUltimoCalculo=today.
     * Called automatically when an Empleado is created.
     *
     * @param empleadoId the ID of the newly created employee
     * @throws java.util.NoSuchElementException if the employee does not exist
     */
    void inicializar(Long empleadoId);

    /**
     * Adds dias to diasGenerados. Updates fechaUltimoCalculo to today.
     * Creates the balance record if it does not yet exist.
     *
     * @param empleadoId the employee whose balance is updated
     * @param dias       the number of days to credit (must be positive)
     * @throws java.util.NoSuchElementException if the employee does not exist
     */
    void acreditarDias(Long empleadoId, BigDecimal dias);

    /**
     * Subtracts dias from diasDisfrutados.
     *
     * @param empleadoId the employee whose balance is updated
     * @param dias       the number of days to deduct (must be positive)
     * @throws IllegalArgumentException         if dias > diasDisponibles
     * @throws java.util.NoSuchElementException if no balance record exists
     */
    void descontarDias(Long empleadoId, BigDecimal dias);

    /**
     * Returns the current balance for the given employee.
     *
     * @param empleadoId the employee ID
     * @throws java.util.NoSuchElementException if no balance record exists
     */
    SaldoVacaciones consultarSaldo(Long empleadoId);
}
