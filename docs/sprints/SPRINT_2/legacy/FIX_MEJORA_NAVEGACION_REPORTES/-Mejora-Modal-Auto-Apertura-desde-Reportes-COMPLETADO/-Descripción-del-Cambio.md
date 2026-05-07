## 📋 Descripción del Cambio

Se modificó el comportamiento de los botones "Ver Detalles" en los reportes de Clientes y Productos para que, en lugar de redirigir a páginas separadas de edición (`/clientes/editar/{id}` o `/productos/editar/{id}`), ahora redirigen a las páginas principales con un parámetro query (`?edit={id}`) que automáticamente abre el modal de edición con los datos del registro seleccionado.

---

