## 🗄️ ESTRUCTURA DE BASE DE DATOS

### Tabla: configuracion_notificaciones

```sql
CREATE TABLE configuracion_notificaciones (
    id_configuracion INT AUTO_INCREMENT PRIMARY KEY,
    activar_email BOOLEAN NOT NULL DEFAULT TRUE,
    enviar_factura_automatica BOOLEAN NOT NULL DEFAULT FALSE,
    dias_recordatorio_preventivo INT DEFAULT 3,
    dias_recordatorio_pago INT DEFAULT 0,
    frecuencia_recordatorios INT DEFAULT 7,
    notificar_nuevo_cliente BOOLEAN NOT NULL DEFAULT FALSE,
    notificar_nuevo_usuario BOOLEAN NOT NULL DEFAULT FALSE,
    email_admin VARCHAR(100),
    email_copia_facturas VARCHAR(100),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    create_by VARCHAR(50),
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(50),
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_activo (activo)
);
```

### Registro por Defecto

```sql
INSERT INTO configuracion_notificaciones (
    activar_email,
    enviar_factura_automatica,
    dias_recordatorio_preventivo,
    dias_recordatorio_pago,
    frecuencia_recordatorios,
    notificar_nuevo_cliente,
    notificar_nuevo_usuario,
    activo,
    create_by
) VALUES (
    TRUE,    -- Sistema activo
    FALSE,   -- Envío manual por defecto
    3,       -- 3 días antes
    0,       -- Mismo día de vencimiento
    7,       -- Cada 7 días
    FALSE,   -- Sin notificar clientes
    FALSE,   -- Sin notificar usuarios
    TRUE,    -- Configuración activa
    'SYSTEM'
);
```

---

