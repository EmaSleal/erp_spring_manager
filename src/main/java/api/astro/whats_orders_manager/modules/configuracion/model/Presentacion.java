package api.astro.whats_orders_manager.modules.configuracion.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "presentacion")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Presentacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPresentacion")
    private Integer idPresentacion;
    @Column(name = "nombre")
    private String nombre;
    
    /**
     * Código de unidad de medida para Facturación Electrónica Costa Rica
     * Ejemplos: Unid, Sp, m, kg, l, etc.
     * Según catálogo de Hacienda
     */
    @Column(name = "codigo_unidad_fe", length = 10)
    private String codigoUnidadFE;

    // Getters y Setters

    @CreatedDate
    @Column(name = "createDate", updatable = false)
    private LocalDateTime createDate;

    @CreatedDate
    @Column(name = "updateDate")
    private LocalDateTime updateDate;

    @CreatedBy
    @Column(name = "createBy", updatable = false)
    private Integer createBy;

    @CreatedBy
    @Column(name = "updateBy")
    private Integer updateBy;

}

