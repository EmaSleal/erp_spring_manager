## 🧪 TESTING PENDIENTE

### Testing Manual Recomendado:

**1. Acceso y Permisos:**
- [ ] Login como ADMIN → Ver enlace Usuarios en sidebar
- [ ] Login como USER → No ver enlace Usuarios
- [ ] Usuario USER intenta acceder a /usuarios directamente → 403

**2. CRUD de Usuarios:**
- [ ] Crear usuario nuevo con todos los campos
- [ ] Crear usuario con teléfono duplicado → Error
- [ ] Crear usuario con email duplicado → Error
- [ ] Editar usuario existente
- [ ] Editar sin cambiar contraseña → Contraseña se mantiene
- [ ] Eliminar usuario (no el propio) → Éxito
- [ ] Intentar eliminar cuenta propia → Error

**3. Filtros y Búsqueda:**
- [ ] Filtrar por rol ADMIN
- [ ] Filtrar por estado Activo
- [ ] Buscar por nombre parcial
- [ ] Buscar por teléfono parcial
- [ ] Buscar por email parcial
- [ ] Combinar múltiples filtros
- [ ] Cambiar ordenamiento
- [ ] Cambiar dirección de orden

**4. Paginación:**
- [ ] Crear más de 10 usuarios
- [ ] Navegar a página 2
- [ ] Ir a última página
- [ ] Volver a primera página
- [ ] Verificar conservación de filtros entre páginas

**5. Toggle Estado:**
- [ ] Desactivar usuario (no propio)
- [ ] Verificar que usuario inactivo no puede hacer login
- [ ] Activar usuario nuevamente
- [ ] Intentar desactivar cuenta propia → Error

**6. Reset Contraseña:**
- [ ] Resetear contraseña de otro usuario
- [ ] Verificar que se genera contraseña de 12 caracteres
- [ ] Copiar contraseña al portapapeles
- [ ] Login con nueva contraseña → Éxito

**7. Generador de Contraseñas:**
- [ ] Click en botón "Generar" en formulario
- [ ] Verificar contraseña generada (12 caracteres)
- [ ] Verificar que incluye mayúsculas, minúsculas, números, símbolos
- [ ] Crear usuario con contraseña generada
- [ ] Login con contraseña generada → Éxito

**8. Validaciones:**
- [ ] Intentar crear usuario sin nombre → Error HTML5
- [ ] Intentar crear con teléfono de 8 dígitos → Error HTML5
- [ ] Intentar crear con email inválido → Error HTML5
- [ ] Intentar crear con contraseña de 5 caracteres → Error HTML5
- [ ] Intentar crear con contraseñas que no coinciden → Error JS

**9. Responsive:**
- [ ] Abrir en móvil → Verificar layout adaptado
- [ ] Verificar que columnas menos importantes se ocultan
- [ ] Verificar que filtros se apilan verticalmente
- [ ] Verificar que tarjetas de estadísticas se adaptan

**10. Interfaz:**
- [ ] Verificar avatares con colores correctos
- [ ] Verificar badges de rol con colores correctos
- [ ] Verificar tooltips en botones
- [ ] Verificar alertas toast
- [ ] Verificar modal de reset password
- [ ] Verificar loading states en botones

---

