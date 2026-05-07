## 🧪 Flujo de Pruebas

### Caso 1: Ver Perfil
1. ✅ Login como usuario
2. ✅ Click en "Mi Perfil" en navbar
3. ✅ Verificar que se muestra toda la información
4. ✅ Verificar avatar (imagen o iniciales)
5. ✅ Verificar fechas formateadas correctamente

### Caso 2: Editar Información Personal
1. ✅ Click en "Editar Perfil"
2. ✅ Modificar nombre
3. ✅ Modificar email (verificar unicidad)
4. ✅ Modificar teléfono (verificar unicidad)
5. ✅ Click "Guardar Cambios"
6. ✅ Verificar mensaje success
7. ✅ Verificar redirección a `/perfil`
8. ✅ Verificar que los cambios se guardaron

### Caso 3: Cambiar Contraseña
1. ✅ Ir a tab "Cambiar Contraseña"
2. ✅ Ingresar contraseña actual incorrecta → Ver error
3. ✅ Ingresar contraseña actual correcta
4. ✅ Ingresar nueva contraseña débil → Ver indicador
5. ✅ Ingresar contraseñas que no coinciden → Ver error
6. ✅ Ingresar contraseñas correctas
7. ✅ Click "Cambiar Contraseña"
8. ✅ Verificar mensaje success
9. ✅ Logout y login con nueva contraseña

### Caso 4: Subir Avatar
1. ✅ Ir a tab "Foto de Perfil"
2. ✅ Seleccionar archivo > 2MB → Ver error
3. ✅ Seleccionar archivo .txt → Ver error
4. ✅ Seleccionar imagen válida
5. ✅ Verificar preview
6. ✅ Click "Subir Foto"
7. ✅ Verificar mensaje success
8. ✅ Verificar que la imagen se muestra en perfil
9. ✅ Verificar que la imagen se muestra en navbar

### Caso 5: Eliminar Avatar
1. ✅ Ir a tab "Foto de Perfil"
2. ✅ Click "Eliminar Foto Actual"
3. ✅ Confirmar eliminación
4. ✅ Verificar mensaje success
5. ✅ Verificar que se muestran iniciales en perfil
6. ✅ Verificar que se muestran iniciales en navbar

---

