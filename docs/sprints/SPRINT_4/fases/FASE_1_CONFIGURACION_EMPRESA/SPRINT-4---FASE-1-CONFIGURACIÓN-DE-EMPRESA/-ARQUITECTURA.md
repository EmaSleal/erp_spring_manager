## 🏗️ ARQUITECTURA

### Componentes Principales

```
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE PRESENTACIÓN                      │
├─────────────────────────────────────────────────────────────┤
│  /admin/empresa/editar.html                                 │
│  - Formulario de datos básicos                              │
│  - Configuración SMTP                                       │
│  - Carga de logotipo                                        │
│  - Envío de email de prueba                                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE CONTROLADOR                       │
├─────────────────────────────────────────────────────────────┤
│  EmpresaController.java                                     │
│  - GET  /admin/empresa/editar                               │
│  - POST /admin/empresa/actualizar                           │
│  - POST /admin/empresa/logo                                 │
│  - POST /admin/empresa/enviar-prueba                        │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE SERVICIO                          │
├─────────────────────────────────────────────────────────────┤
│  EmpresaService.java                                        │
│  - obtenerConfiguracion()                                   │
│  - actualizarConfiguracion(EmpresaDTO)                      │
│  - guardarLogo(MultipartFile)                               │
│                                                              │
│  EmailService.java                                          │
│  - enviarEmailPrueba(String destinatario)                   │
│  - Validación de configuración SMTP                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE PERSISTENCIA                      │
├─────────────────────────────────────────────────────────────┤
│  EmpresaRepository.java (JPA)                               │
│  - findById(1L) // Singleton pattern                        │
│  - save(Empresa)                                            │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    BASE DE DATOS                             │
├─────────────────────────────────────────────────────────────┤
│  Tabla: empresa                                             │
│  - Datos básicos (nombre, CIF, dirección)                   │
│  - Configuración SMTP (host, puerto, usuario)               │
│  - Ruta de logotipo                                         │
│  - Auditoría (creadoPor, modificadoPor, timestamps)         │
└─────────────────────────────────────────────────────────────┘
```

---

