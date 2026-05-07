## 📦 FASE 1: CONFIGURACIÓN DE EMPRESA

### Objetivo
Permitir a los administradores configurar los datos de la empresa que aparecerán en facturas y documentos.

### Tareas

#### 1.1 Modelo de Datos
**Archivo:** `Empresa.java`

```java
@Entity
@Table(name = "empresa")
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEmpresa;
    
    @Column(name = "nombre_empresa", nullable = false, length = 200)
    private String nombreEmpresa;
    
    @Column(name = "nombre_comercial", length = 200)
    private String nombreComercial;
    
    @Column(name = "ruc", length = 20)
    private String ruc;
    
    @Column(name = "direccion", length = 300)
    private String direccion;
    
    @Column(name = "telefono", length = 20)
    private String telefono;
    
    @Column(name = "email", length = 100)
    private String email;
    
    @Column(name = "sitio_web", length = 200)
    private String sitioWeb;
    
    @Column(name = "logo", length = 255)
    private String logo;
    
    @Column(name = "favicon", length = 255)
    private String favicon;
    
    @Column(name = "activo")
    private Boolean activo = true;
    
    @Column(name = "fecha_creacion")
    private Timestamp fechaCreacion;
    
    @Column(name = "fecha_modificacion")
    private Timestamp fechaModificacion;
    
    @Column(name = "usuario_modificacion")
    private Integer usuarioModificacion;
}
```

**Script SQL:**
```sql
CREATE TABLE empresa (
    id_empresa INT AUTO_INCREMENT PRIMARY KEY,
    nombre_empresa VARCHAR(200) NOT NULL,
    nombre_comercial VARCHAR(200),
    ruc VARCHAR(20),
    direccion VARCHAR(300),
    telefono VARCHAR(20),
    email VARCHAR(100),
    sitio_web VARCHAR(200),
    logo VARCHAR(255),
    favicon VARCHAR(255),
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    usuario_modificacion INT
);

-- Insertar configuración por defecto
INSERT INTO empresa (nombre_empresa, nombre_comercial, activo) 
VALUES ('Mi Empresa', 'Mi Empresa', TRUE);
```

#### 1.2 Repository, Service y Controller
- `EmpresaRepository.java`
- `EmpresaService.java`
- `EmpresaServiceImpl.java`
- `ConfiguracionController.java`

#### 1.3 Vistas
- `configuracion/empresa.html` - Formulario de datos de empresa
- `configuracion/index.html` - Página principal con tabs

#### 1.4 Funcionalidades
- ✅ Ver datos de la empresa
- ✅ Editar datos de la empresa
- ✅ Upload de logo
- ✅ Upload de favicon
- ✅ Preview de logo y favicon
- ✅ Validaciones (RUC, email, teléfono)

---

