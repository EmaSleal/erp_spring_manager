package api.astro.whats_orders_manager.modules.regimen.verificacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VerificacionRegimenRepository extends JpaRepository<VerificacionRegimen, Long> {

    Optional<VerificacionRegimen> findFirstByOrderByFechaHoraDescIdDesc();

    /** Las más recientes primero. */
    List<VerificacionRegimen> findTop60ByOrderByFechaHoraDescIdDesc();

    /** Borra las anteriores al corte, salvo la que tiene el id indicado (la última). */
    @Modifying
    @Query("DELETE FROM VerificacionRegimen v WHERE v.fechaHora < :corte AND v.id <> :idUltima")
    int purgarAnterioresA(LocalDateTime corte, Long idUltima);
}
