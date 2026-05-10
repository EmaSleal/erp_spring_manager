## 🔄 FLUJOS DE TRABAJO

### 1. Actualizar Datos de Empresa

```
Usuario (Admin)
      │
      ├─→ Accede a /admin/empresa/editar
      │
      ├─→ EmpresaController.mostrarFormulario()
      │         │
      │         └─→ EmpresaService.obtenerConfiguracion()
      │                   │
      │                   └─→ EmpresaRepository.findById(1L)
      │                             │
      │                             └─→ Retorna Empresa → DTO
      │
      ├─→ Renderiza editar.html con datos
      │
      ├─→ Usuario modifica campos
      │
      ├─→ Submit formulario → POST /admin/empresa/actualizar
      │
      ├─→ EmpresaController.actualizar(dto)
      │         │
      │         ├─→ Validación Bean Validation
      │         │
      │         └─→ EmpresaService.actualizarConfiguracion(dto)
      │                   │
      │                   ├─→ Mapea DTO → Empresa
      │                   ├─→ Registra auditoría (modificadoPor)
      │                   └─→ EmpresaRepository.save()
      │
      └─→ Redirect con mensaje de éxito
```

### 2. Enviar Email de Prueba

```
Usuario (Admin)
      │
      ├─→ Click en "Enviar email de prueba"
      │
      ├─→ JavaScript: prompt() solicita destinatario
      │
      ├─→ fetch() → POST /admin/empresa/enviar-prueba
      │
      ├─→ EmpresaController.enviarEmailPrueba(destinatario)
      │         │
      │         └─→ EmailService.enviarEmailPrueba(destinatario)
      │                   │
      │                   ├─→ Obtiene configuración SMTP
      │                   ├─→ Crea javax.mail.Session
      │                   ├─→ Configura MimeMessage
      │                   └─→ Transport.send()
      │
      └─→ Retorna JSON { mensaje: "Email enviado" }
               │
               └─→ JavaScript: alert() muestra resultado
```

### 3. Subir Logotipo

```
Usuario (Admin)
      │
      ├─→ Selecciona archivo de imagen
      │
      ├─→ Submit form → POST /admin/empresa/logo
      │
      ├─→ EmpresaController.subirLogo(file)
      │         │
      │         └─→ EmpresaService.guardarLogo(file)
      │                   │
      │                   ├─→ Valida tipo MIME (image/*)
      │                   ├─→ Genera nombre único
      │                   ├─→ Guarda en /uploads/logos/
      │                   ├─→ Actualiza empresa.logoUrl
      │                   └─→ Save en BD
      │
      └─→ Redirect con mensaje de éxito
```

---

