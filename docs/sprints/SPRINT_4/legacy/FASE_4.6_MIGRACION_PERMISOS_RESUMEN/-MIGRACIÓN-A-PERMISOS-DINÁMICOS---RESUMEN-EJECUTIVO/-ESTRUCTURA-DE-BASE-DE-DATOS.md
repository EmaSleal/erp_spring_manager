## 🗄️ ESTRUCTURA DE BASE DE DATOS

### Tablas que se Crearán Automáticamente

Al arrancar Spring Boot, JPA/Hibernate creará automáticamente estas tablas:

#### 1. **permiso**
```sql
CREATE TABLE permiso (
    id_permiso BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(100) UNIQUE NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    descripcion TEXT,
    categoria VARCHAR(50) NOT NULL,
    es_critico BOOLEAN DEFAULT FALSE,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

#### 2. **rol**
```sql
CREATE TABLE rol (
    id_rol BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(50) UNIQUE NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

#### 3. **rol_permiso** (Many-to-Many)
```sql
CREATE TABLE rol_permiso (
    id_rol BIGINT NOT NULL,
    id_permiso BIGINT NOT NULL,
    PRIMARY KEY (id_rol, id_permiso),
    FOREIGN KEY (id_rol) REFERENCES rol(id_rol) ON DELETE CASCADE,
    FOREIGN KEY (id_permiso) REFERENCES permiso(id_permiso) ON DELETE CASCADE
);
```

#### 4. **usuario_permiso**
```sql
CREATE TABLE usuario_permiso (
    id_usuario_permiso BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    id_permiso BIGINT NOT NULL,
    tipo ENUM('CONCEDIDO', 'DENEGADO') NOT NULL,
    concedido_por BIGINT,
    created_at TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuario(id) ON DELETE CASCADE,
    FOREIGN KEY (id_permiso) REFERENCES permiso(id_permiso) ON DELETE CASCADE,
    FOREIGN KEY (concedido_por) REFERENCES usuario(id)
);
```

#### 5. **usuario** (columna adicional)
```sql
ALTER TABLE usuario 
ADD COLUMN id_rol BIGINT,
ADD FOREIGN KEY (id_rol) REFERENCES rol(id_rol);
```

---

