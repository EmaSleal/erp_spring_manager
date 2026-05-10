package api.astro.whats_orders_manager.modules.configuracion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/**
 * Entidad para catálogo de provincias de Costa Rica.
 * Datos oficiales de Hacienda/INEC.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 9
 */
@Entity
@Table(name = "cat_provincia_cr")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProvinciaCostaRica implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Código de provincia (1-7)
     * 1=San José, 2=Alajuela, 3=Cartago, 4=Heredia, 
     * 5=Guanacaste, 6=Puntarenas, 7=Limón
     */
    @Id
    @Column(name = "codigo", length = 1, nullable = false)
    private String codigo;

    /**
     * Nombre de la provincia
     */
    @Column(name = "nombre", length = 50, nullable = false)
    private String nombre;
}
