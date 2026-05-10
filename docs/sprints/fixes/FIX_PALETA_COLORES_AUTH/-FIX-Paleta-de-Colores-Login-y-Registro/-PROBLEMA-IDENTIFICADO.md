## 📋 PROBLEMA IDENTIFICADO

Las páginas de autenticación (Login y Registro) usaban una **paleta de colores púrpura** que no coincidía con el **Material Design azul** del resto de la aplicación.

### **Antes:**
```css
/* Login y Registro */
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
```

### **Resto de la aplicación:**
```css
/* Dashboard, Navbar, etc. */
--primary-color: #1976D2;  /* Azul Material Design */
--primary-dark: #1565C0;
--primary-light: #42A5F5;
```

---

