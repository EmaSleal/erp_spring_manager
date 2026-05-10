## 📝 NOTAS DE IMPLEMENTACIÓN

### Decisiones Técnicas

1. **Patrón Singleton:**
   - Se usa `ID = 1` para la única configuración de empresa
   - Simplifica la lógica de negocio
   - Evita complejidad de múltiples configuraciones

2. **Encriptación de Contraseñas:**
   - ⚠️ Pendiente implementar: jasypt o similar
   - Actualmente almacenado en texto plano (usar solo en desarrollo)

3. **Validación SMTP:**
   - Email de prueba valida configuración antes de producción
   - Evita errores en envío de facturas/notificaciones

4. **Gestión de Archivos:**
   - Logotipo almacenado en `/uploads/logos/`
   - Nombre único con timestamp para evitar colisiones
   - Validación de tipo MIME en servidor

### Mejoras Futuras

- [ ] Encriptar contraseña SMTP con Jasypt
- [ ] Versionado de configuraciones (histórico de cambios)
- [ ] Preview de plantillas de email con datos de empresa
- [ ] Validación de CIF contra API de Hacienda
- [ ] Multi-tenant: soporte para múltiples empresas

### Dependencias

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
</dependency>
```

---

**FIN DEL DOCUMENTO**
