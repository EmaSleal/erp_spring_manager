## 🗄️ MODELO DE DATOS

### Entidad: `Notificacion`

```java
@Entity
@Table(name = "notificaciones")
@EntityListeners(AuditingEntityListener.class)
public class Notificacion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // DESTINATARIO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    
    // TIPO Y CANAL
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacion tipo; // FACTURA_NUEVA, FACTURA_PAGADA, etc.
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalNotificacion canal; // WEB, EMAIL, WHATSAPP
    
    // CONTENIDO
    @Column(nullable = false, length = 200)
    private String titulo;
    
    @Column(columnDefinition = "TEXT")
    private String mensaje;
    
    // REFERENCIAS
    @Column(name = "factura_id")
    private Long facturaId;
    
    @Column(name = "pedido_id")
    private Long pedidoId;
    
    @Column(name = "cliente_id")
    private Long clienteId;
    
    // ESTADOS
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoNotificacion estado; // PENDIENTE, ENVIADO, ENTREGADO, LEIDO, FALLIDO
    
    private Boolean leida = false;
    
    @Column(columnDefinition = "TEXT")
    private String errorMensaje;
    
    // TIMESTAMPS
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;
    
    private LocalDateTime enviadoEn;
    private LocalDateTime entregadoEn;
    private LocalDateTime leidoEn;
    
    // WHATSAPP ESPECÍFICO
    @Column(length = 100)
    private String whatsappMessageId; // ID de mensaje de WhatsApp
    
    @Column(length = 20)
    private String whatsappEstado; // sent, delivered, read, failed
}
```

### Entidad: `PlantillaWhatsApp`

```java
@Entity
@Table(name = "plantillas_whatsapp")
@EntityListeners(AuditingEntityListener.class)
public class PlantillaWhatsApp {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String codigo; // FACTURA_NUEVA, FACTURA_RECORDATORIO
    
    @Column(nullable = false, length = 200)
    private String nombre;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenido; // Texto con variables: "Hola {nombre}, tu factura #{numero}..."
    
    @Column(columnDefinition = "TEXT")
    private String descripcion;
    
    // VARIABLES DISPONIBLES (JSON array)
    @Column(columnDefinition = "TEXT")
    private String variablesDisponibles; // ["nombre", "numero", "total", "fecha"]
    
    private Boolean activa = true;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TipoNotificacion tipoNotificacion;
    
    // AUDITORÍA
    @CreatedBy
    @Column(updatable = false)
    private String creadoPor;
    
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime creadoEn;
    
    @LastModifiedBy
    private String modificadoPor;
    
    @LastModifiedDate
    private LocalDateTime modificadoEn;
}
```

### Entidad: `PreferenciaNotificacion`

```java
@Entity
@Table(name = "preferencias_notificacion")
public class PreferenciaNotificacion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;
    
    // CANALES HABILITADOS
    private Boolean notificacionesWeb = true;
    private Boolean notificacionesEmail = true;
    private Boolean notificacionesWhatsApp = false;
    
    // TIPOS DE EVENTOS
    private Boolean recibirFacturas = true;
    private Boolean recibirPedidos = true;
    private Boolean recibirRecordatorios = true;
    private Boolean recibirNoticias = false;
    
    // CONFIGURACIÓN
    @Column(length = 20)
    private String telefono; // Para WhatsApp
    
    private Boolean validadoTelefono = false;
    
    @LastModifiedDate
    private LocalDateTime modificadoEn;
}
```

### Enumeraciones

```java
public enum TipoNotificacion {
    FACTURA_NUEVA,
    FACTURA_PAGADA,
    FACTURA_VENCIDA,
    FACTURA_RECORDATORIO,
    PEDIDO_CONFIRMADO,
    PEDIDO_ENVIADO,
    USUARIO_NUEVO,
    PASSWORD_RESET
}

public enum CanalNotificacion {
    WEB,
    EMAIL,
    WHATSAPP
}

public enum EstadoNotificacion {
    PENDIENTE,
    ENVIANDO,
    ENVIADO,
    ENTREGADO,
    LEIDO,
    FALLIDO
}
```

---

