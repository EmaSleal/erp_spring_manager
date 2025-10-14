package api.astro.whats_orders_manager.models;

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

    // Getters y Setters
}
