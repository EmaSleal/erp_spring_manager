## 🗄️ MODELO DE DATOS

### Entidad: `Empresa`

```java
@Entity
@Table(name = "empresa")
@EntityListeners(AuditingEntityListener.class)
public class Empresa {
    
    // Identificador único (siempre ID = 1, singleton)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // DATOS BÁSICOS
    @Column(nullable = false, length = 200)
    private String nombre;
    
    @Column(length = 20)
    private String cif;
    
    @Column(length = 500)
    private String direccion;
    
    @Column(length = 100)
    private String ciudad;
    
    @Column(length = 50)
    private String provincia;
    
    @Column(length = 10)
    private String codigoPostal;
    
    @Column(length = 50)
    private String pais;
    
    // CONTACTO
    @Column(length = 20)
    private String telefono;
    
    @Column(length = 100)
    private String email;
    
    @Column(length = 200)
    private String sitioWeb;
    
    // CONFIGURACIÓN SMTP
    @Column(length = 100)
    private String smtpHost;
    
    private Integer smtpPort;
    
    @Column(length = 100)
    private String smtpUsuario;
    
    @Column(length = 100)
    private String smtpPassword; // Encriptado en próximas versiones
    
    private Boolean smtpSsl;
    private Boolean smtpAuth;
    
    // PERSONALIZACIÓN
    @Column(length = 500)
    private String logoUrl;
    
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

### DTO: `EmpresaDTO`

```java
public class EmpresaDTO {
    // Mismo mapeo de campos que la entidad
    // Usado para transferencia de datos entre capas
    // Incluye validaciones Bean Validation
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200)
    private String nombre;
    
    @Pattern(regexp = "[A-Z0-9]{9}", message = "CIF inválido")
    private String cif;
    
    @Email(message = "Email inválido")
    private String email;
    
    // ... resto de campos
}
```

---

