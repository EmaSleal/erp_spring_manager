## 📦 Estructura Detallada de `/models`

### 🎯 Nueva Organización (Sprint 3 - Fase 1.5)

```
models/
├── dto/                               # Data Transfer Objects
│   ├── EstadisticasUsuariosDTO.java
│   ├── ModuloDTO.java
│   ├── PaginacionDTO.java
│   ├── PlantillaWhatsAppDTO.java
│   ├── ResponseDTO.java
│   ├── WebhookValidationDTO.java
│   └── WhatsAppMensajeDTO.java
│
├── enums/                             # Enumeraciones standalone
│   └── (vacío - enums están como inner classes)
│
├── class/                             # Clases auxiliares
│   └── (vacío - reservado para futuras clases)
│
├── records/                           # Java Records
│   ├── LineaFacturaR.java             # Record para líneas de factura
│   └── ProductoRecord.java            # Record para productos
│
└── Entidades JPA (raíz de models/)
    ├── Cliente.java
    ├── ConfiguracionFacturacion.java
    ├── ConfiguracionNotificaciones.java
    ├── Empresa.java
    ├── Factura.java
    ├── LineaFactura.java
    ├── MensajeWhatsApp.java           # Contiene TipoMensaje y EstadoMensaje como inner enums
    ├── PlantillaWhatsApp.java         # Contiene CategoriaPlantilla y EstadoMeta como inner enums
    ├── Presentacion.java
    ├── Producto.java
    ├── Usuario.java
    └── WebhookLog.java
```

---

