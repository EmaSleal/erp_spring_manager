## 🔄 MIGRACIÓN DE DATOS

### Script SQL Necesario (Pendiente)
```sql
-- Agregar columna id_usuario a mensaje_whatsapp
ALTER TABLE mensaje_whatsapp 
ADD COLUMN id_usuario INT NULL AFTER estado;

-- Crear índice para id_usuario
CREATE INDEX idx_usuario ON mensaje_whatsapp(id_usuario);

-- Migrar datos existentes (si los hay)
-- Relacionar mensajes con usuarios basándose en el teléfono
UPDATE mensaje_whatsapp m
INNER JOIN usuario u ON m.telefono = u.telefono
SET m.id_usuario = u.id_usuario;

-- Agregar foreign key
ALTER TABLE mensaje_whatsapp
ADD CONSTRAINT fk_mensaje_usuario 
FOREIGN KEY (id_usuario) 
REFERENCES usuario(id_usuario)
ON DELETE SET NULL
ON UPDATE CASCADE;

-- Eliminar columna id_factura (opcional, si ya existe)
-- ALTER TABLE mensaje_whatsapp DROP COLUMN id_factura;
```

---

