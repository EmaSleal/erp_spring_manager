## 📊 FLUJO DE TRABAJO

### Guardar Configuración
1. Usuario completa formulario
2. JavaScript valida básico
3. Submit con CSRF token
4. Controller valida con `@Valid`
5. Service guarda/actualiza
6. Redirect con mensaje flash
7. Vista muestra confirmación

### Probar Email
1. Usuario ingresa email
2. Click "Enviar Email de Prueba"
3. JavaScript valida email
4. Fetch POST a `/probar-email`
5. EmailService envía email
6. Respuesta OK/ERROR
7. SweetAlert muestra resultado

### Ejecutar Recordatorios
1. Usuario click botón
2. Confirmación con SweetAlert
3. Fetch POST a `/ejecutar-recordatorios`
4. Scheduler se ejecuta
5. Logs muestran resultados
6. SweetAlert confirma ejecución

---

