## 📋 JUSTIFICACIÓN DEL CAMBIO

### ¿Por qué "Precio Institucional" y no "Precio Público"?

Según la estructura del negocio (WhatsApp Orders Manager):

1. **precioMayorista**: Precio para clientes mayoristas
2. **precioInstitucional**: Precio para clientes institucionales

No existe un "precio público" como tal en el modelo de negocio. Los dos tipos de clientes son:
- `TipoCliente.MAYORISTA`
- `TipoCliente.INSTITUCIONAL`

Por lo tanto, es coherente mostrar ambos precios según los tipos de cliente existentes.

---

