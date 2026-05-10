## 🔧 CONFIGURACIÓN DE BASE DE DATOS

### Campo en Tabla usuario

El campo `ultimo_acceso` ya existía en la tabla `usuario`:

```sql
CREATE TABLE usuario (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    avatar VARCHAR(255),
    activo BOOLEAN DEFAULT TRUE,
    ultimo_acceso TIMESTAMP NULL,  -- ✅ Ya existía
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by INT,
    update_by INT
);
```

**Tipo:** `TIMESTAMP NULL`  
**Permite NULL:** Sí (para usuarios que nunca han iniciado sesión)  
**Default:** NULL

### Actualización Automática

```sql
-- Ejemplo de UPDATE ejecutado en cada login:
UPDATE usuario 
SET ultimo_acceso = '2025-10-20 11:37:45.123' 
WHERE id_usuario = 1;
```

---

