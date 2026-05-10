## 🎨 Características Comunes de los Templates

### Diseño y Estilo

**Paleta de Colores:**
- **Primario:** #667eea (morado) - Factura y credenciales
- **Secundario:** #ffc107 (amarillo/naranja) - Recordatorio de pago
- **Éxito:** #28a745 (verde) - Estados positivos
- **Advertencia:** #ffc107 (amarillo) - Alertas
- **Peligro:** #dc3545 (rojo) - Errores y vencimientos
- **Neutro:** #6c757d (gris) - Textos secundarios

**Tipografía:**
- **Fuente Principal:** Arial, Helvetica, sans-serif
- **Fuente Monoespaciada:** 'Courier New' (para credenciales y números)
- **Tamaños:**
  - Títulos principales: 24-28px
  - Subtítulos: 18-22px
  - Texto normal: 14-16px
  - Footer: 12-14px

**Layout:**
- **Ancho Máximo:** 600-700px
- **Padding Principal:** 30-40px
- **Padding Móvil:** 15-20px
- **Border Radius:** 8-12px
- **Sombras:** box-shadow suaves

### Responsive Design

**Breakpoint:** 600px

**Ajustes Móviles:**
```css
@media only screen and (max-width: 600px) {
    .content { padding: 20px 15px; }
    .products-table th,
    .products-table td { 
        padding: 8px; 
        font-size: 13px; 
    }
    .total-row.final { font-size: 20px; }
    .amount-due .amount { font-size: 32px; }
}
```

### Accesibilidad

- ✅ Contraste de colores adecuado
- ✅ Tamaños de fuente legibles
- ✅ Jerarquía visual clara
- ✅ Iconos descriptivos
- ✅ Textos alternativos
- ✅ Estructura semántica HTML5

### Compatibilidad

**Clientes de Email Probados:**
- ✅ Gmail (Web, Android, iOS)
- ✅ Outlook (Web, Desktop)
- ✅ Apple Mail
- ✅ Yahoo Mail
- ✅ Thunderbird

**Técnicas de Compatibilidad:**
- CSS inline para estilos críticos
- Tables para layout principal
- Estilos en `<style>` tag para reducir tamaño
- Imágenes externas evitadas (solo emojis Unicode)
- Sin JavaScript

---

