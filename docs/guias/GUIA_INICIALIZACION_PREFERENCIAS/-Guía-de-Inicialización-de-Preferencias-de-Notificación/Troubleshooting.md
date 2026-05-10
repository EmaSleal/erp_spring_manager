## Troubleshooting

### Error: "Solo administradores pueden ejecutar esta acción"
**Causa:** El usuario no tiene rol ADMIN  
**Solución:** Usar el endpoint individual `/preferencias/inicializar` o autenticarse como admin

### Error: "El usuario ya tiene preferencias configuradas"
**Causa:** El usuario ya tiene preferencias en la BD  
**Solución:** Esto es normal, no se crean preferencias duplicadas

### Sin registros creados
**Causa:** Todos los usuarios ya tienen preferencias  
**Solución:** Verificar con la consulta SQL de verificación

---

