package api.astro.whats_orders_manager.modules.cliente.repository;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    
    /**
     * Llama al SP para obtener clientes nuevos por mes
     * @param meses Número de meses hacia atrás
     * @return Lista de arrays [mes, cantidad_clientes]
     */
    @Query(value = "CALL sp_obtener_clientes_nuevos_por_mes(:meses)", nativeQuery = true)
    List<Object[]> obtenerClientesNuevosPorMes(@Param("meses") int meses);
    
    /**
     * Llama al SP para obtener top clientes por volumen de compras
     * @param limite Número de clientes a retornar
     * @return Lista de arrays [cliente, total_compras, cantidad_facturas]
     */
    @Query(value = "CALL sp_obtener_top_clientes(:limite)", nativeQuery = true)
    List<Object[]> obtenerTopClientes(@Param("limite") int limite);

    // Listar todos los clientes ordenados por nombre
    List<Cliente> findAllByOrderByNombreAsc();
           
    //findById
    Optional<Cliente> findByIdCliente(Integer idCliente);
}
