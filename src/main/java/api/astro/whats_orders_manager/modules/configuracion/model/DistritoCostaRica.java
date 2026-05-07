package api.astro.whats_orders_manager.modules.configuracion.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/**
 * Entidad para catálogo de distritos de Costa Rica.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 9
 */
@Entity
@Table(name = "cat_distrito_cr")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DistritoCostaRica implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Código de provincia (1-7)
     */
    @Column(name = "provincia_codigo", length = 1, nullable = false)
    private String provinciaCodigo;

    /**
     * Código de cantón (01-99)
     */
    @Column(name = "canton_codigo", length = 2, nullable = false)
    private String cantonCodigo;

    /**
     * Código de distrito dentro del cantón (01-99)
     */
    @Column(name = "codigo", length = 2, nullable = false)
    private String codigo;

    /**
     * Nombre del distrito
     */
    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;
}
