## 📝 Convenciones de Nomenclatura

### Entidades JPA
- **Patrón:** `NombreSingular.java`
- **Ejemplo:** `Cliente.java`, `Factura.java`
- **Tabla:** `nombre_plural` (snake_case)

### DTOs
- **Patrón:** `NombreDTO.java`
- **Ejemplo:** `WhatsAppMensajeDTO.java`
- **Ubicación:** `models/dto/`

### Records
- **Patrón:** `NombreRecord.java` o `NombreR.java`
- **Ejemplo:** `ProductoRecord.java`, `LineaFacturaR.java`
- **Ubicación:** `models/records/`

### Enums
- **Patrón:** `NombrePascalCase.java`
- **Ejemplo:** `TipoMensaje.java`, `EstadoMensaje.java`
- **Ubicación:** `models/enums/` (futuro)
- **Actual:** Inner classes en entidades

### Servicios
- **Patrón:** `NombreService.java`
- **Ejemplo:** `MensajeWhatsAppService.java`

### Repositorios
- **Patrón:** `NombreRepository.java`
- **Ejemplo:** `MensajeWhatsAppRepository.java`

### Controladores
- **Patrón:** `NombreController.java` o `NombreViewController.java`
- **Ejemplo:** `WhatsAppViewController.java`

---

