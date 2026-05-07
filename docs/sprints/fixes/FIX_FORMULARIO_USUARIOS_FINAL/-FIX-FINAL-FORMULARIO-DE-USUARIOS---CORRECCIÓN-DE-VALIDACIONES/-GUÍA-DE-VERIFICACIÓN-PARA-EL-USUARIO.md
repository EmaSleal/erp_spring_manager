## 🧪 GUÍA DE VERIFICACIÓN PARA EL USUARIO

### **Test 1: Verificar Carga de Scripts**
```
1. Abrir el formulario de nuevo usuario
2. Presionar F12 (abrir DevTools)
3. Ir a la pestaña "Console"
4. ¿Hay errores de "$ is not defined" o "Swal is not defined"?
   - SI → El orden de scripts está mal
   - NO → Los scripts se cargan correctamente ✅
```

**Si hay errores, VERIFICAR el archivo `form.html` líneas 330-340:**
```html
<!-- DEBE ESTAR EN ESTE ORDEN: -->
</section>
    </div>
</main>

<!-- Scripts comunes PRIMERO -->
<th:block th:replace="~{layout :: scripts}"></th:block>

<!-- Scripts específicos DESPUÉS -->
<script th:src="@{/js/usuarios.js}"></script>
</body>
```

---

### **Test 2: Botón Generar Contraseña**
```
1. Ir a /usuarios/form (nuevo usuario)
2. Hacer clic en el botón "Generar"
   
ESPERADO:
✅ Aparece modal de SweetAlert2 con la contraseña
✅ Campos de contraseña se rellenan automáticamente
✅ Contraseñas se muestran (no ocultas)
✅ Campos marcados con borde verde (válidos)
✅ NO se envía el formulario
```

---

### **Test 3: Botón Ver/Ocultar Contraseña**
```
1. Escribir cualquier texto en el campo contraseña
2. Hacer clic en el ícono del ojo

ESPERADO:
✅ La contraseña se muestra/oculta
✅ El ícono cambia (ojo ↔ ojo tachado)
✅ NO se envía el formulario
```

---

### **Test 4: Validación de Teléfono**
```
1. Hacer clic en el campo teléfono
2. Escribir letras (abc)
   ESPERADO: ✅ No se escriben (solo acepta números)

3. Escribir "123"
   ESPERADO: ✅ Campo con borde rojo (inválido)

4. Escribir "12345678" (8 dígitos)
   ESPERADO: ✅ Campo con borde verde (válido)

5. Intentar escribir más dígitos
   ESPERADO: ✅ No permite más de 8 caracteres
```

---

### **Test 5: Validación de Email**
```
1. Escribir "usuario"
   ESPERADO: ✅ Campo con borde rojo

2. Escribir "usuario@"
   ESPERADO: ✅ Campo con borde rojo

3. Escribir "usuario@ejemplo"
   ESPERADO: ✅ Campo con borde rojo

4. Escribir "usuario@ejemplo.com"
   ESPERADO: ✅ Campo con borde verde
```

---

### **Test 6: Validación de Contraseña**
```
1. Escribir "abc" (menos de 6 caracteres)
   ESPERADO: ✅ Campo con borde rojo

2. Escribir "abcdef" (6 caracteres)
   ESPERADO: ✅ Campo con borde verde

3. En confirmación escribir "123456"
   ESPERADO: ✅ Campo confirmación con borde rojo

4. En confirmación escribir "abcdef"
   ESPERADO: ✅ Campo confirmación con borde verde
```

---

### **Test 7: Formulario con Errores**
```
1. Llenar todos los campos con datos inválidos
2. Hacer clic en "Crear Usuario"

ESPERADO:
✅ Formulario NO se envía
✅ Campos inválidos marcados con borde rojo
✅ Datos ingresados NO se pierden
✅ Mensajes de error específicos visibles
```

---

