## 📝 INSTRUCCIONES PARA EL USUARIO

### **Paso 1: Verificar Orden de Scripts**
1. Abrir: `src/main/resources/templates/usuarios/form.html`
2. Ir al final del archivo (línea ~330)
3. Verificar que el orden sea:
   ```html
   <th:block th:replace="~{layout :: scripts}"></th:block>
   <script th:src="@{/js/usuarios.js}"></script>
   ```

### **Paso 2: Reiniciar Servidor**
```bash
# En la terminal:
mvn spring-boot:run
```

### **Paso 3: Limpiar Caché del Navegador**
```
1. Presionar Ctrl + Shift + R (recarga forzada)
2. O ir a DevTools → Application → Clear Storage → Clear
```

### **Paso 4: Probar Funcionalidad**
- Seguir los tests de verificación descritos arriba
- Abrir consola del navegador (F12) para ver errores

### **Paso 5: Reportar Resultados**
Si algo sigue sin funcionar, reportar:
- ¿Qué botón no funciona?
- ¿Hay errores en la consola? (captura de pantalla)
- ¿El orden de scripts está correcto?

---

