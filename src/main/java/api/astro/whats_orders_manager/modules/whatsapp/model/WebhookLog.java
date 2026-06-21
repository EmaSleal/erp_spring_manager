package api.astro.whats_orders_manager.modules.whatsapp.model;

import lombok.*;import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
@Table(name = "webhooklogs")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class WebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "message_id")
    private String messageId;
    @Column(name = "phone_number")
    private String phoneNumber;
    @Column(name = "message_body")
    private String messageBody;

    @Column(columnDefinition = "json", name = "whole_message")
    private String wholeMessage;

    private Timestamp timestamp;

    @Column(name = "estado_conversacion", length = 100)
    private String estadoConversacion;

    @Column(name = "lineas_acumuladas", columnDefinition = "json")
    private String lineasAcumuladas;

    @Column(name = "id_cliente_propuesto")
    private Integer idClientePropuesto;

    @Column(name = "esperando_confirmacion")
    private Boolean esperandoConfirmacion;
}
