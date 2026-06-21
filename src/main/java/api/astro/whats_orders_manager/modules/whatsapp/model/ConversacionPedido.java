package api.astro.whats_orders_manager.modules.whatsapp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversacion_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversacionPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conversacion")
    private Integer idConversacion;

    @Column(name = "telefono_cliente", nullable = false, unique = true, length = 20)
    private String telefonoCliente;

    @Column(name = "id_vendedor", nullable = false)
    private Integer idVendedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_conversacion", nullable = false, length = 30)
    private EstadoConversacion estadoConversacion;

    @Column(name = "lineas_acumuladas", columnDefinition = "TEXT")
    private String lineasAcumuladas;

    @Column(name = "id_cliente_propuesto")
    private Integer idClientePropuesto;

    @Column(name = "esperando_confirmacion", nullable = false)
    private Boolean esperandoConfirmacion = false;

    @Column(name = "ultimo_mensaje_bot", columnDefinition = "TEXT")
    private String ultimoMensajeBot;

    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
        this.updateDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateDate = LocalDateTime.now();
    }
}
