## 🧪 CASOS DE PRUEBA

### Test 1: Generar Contraseña
```
1. Ir a /usuarios/form
2. Hacer clic en "Generar"
   → ✅ Aparece modal con contraseña
   → ✅ Contraseña se muestra en ambos campos
   → ✅ Campos marcados como válidos
   → ✅ Contraseña visible automáticamente
```

### Test 2: Ver/Ocultar Contraseña
```
1. Escribir una contraseña
2. Hacer clic en el ícono del ojo
   → ✅ Contraseña se muestra/oculta
   → ✅ Ícono cambia (ojo/ojo tachado)
   → ✅ No se envía el formulario
```

### Test 3: Validación de Teléfono
```
1. Escribir letras en el campo teléfono
   → ✅ Solo acepta números
2. Escribir 5 dígitos
   → ✅ Campo marcado como inválido
3. Escribir 9 dígitos
   → ✅ Campo marcado como válido
```

### Test 4: Validación de Email
```
1. Escribir "usuario"
   → ✅ Campo marcado como inválido
2. Escribir "usuario@"
   → ✅ Campo marcado como inválido
3. Escribir "usuario@ejemplo.com"
   → ✅ Campo marcado como válido
```

### Test 5: Validación de Contraseñas
```
1. Escribir "abc" en contraseña
   → ✅ Campo marcado como inválido (< 6 caracteres)
2. Escribir "abcdef" en contraseña
   → ✅ Campo marcado como válido
3. Escribir "123456" en confirmación
   → ✅ Confirmación marcada como inválida (no coincide)
4. Escribir "abcdef" en confirmación
   → ✅ Confirmación marcada como válida (coincide)
```

### Test 6: Error en Formulario
```
1. Llenar formulario con datos inválidos
2. Hacer clic en "Crear Usuario"
   → ✅ Formulario NO se reinicia
   → ✅ Datos ingresados permanecen
   → ✅ Campos inválidos marcados en rojo
   → ✅ Mensajes de error específicos
```

---

