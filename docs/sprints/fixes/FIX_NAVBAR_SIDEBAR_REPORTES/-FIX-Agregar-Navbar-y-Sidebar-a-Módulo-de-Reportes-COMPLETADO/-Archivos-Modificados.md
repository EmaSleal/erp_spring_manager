## 📝 Archivos Modificados

### **1. reportes/index.html**
```diff
- <head>
-     <meta charset="UTF-8">
-     <link href="...bootstrap.min.css">
-     <link rel="...font-awesome...">
-     <link th:href="@{/css/navbar.css}">
- </head>
+ <head th:replace="~{layout :: head}">
+     <title>Dashboard de Reportes</title>
+ </head>

  <body>
      <div th:replace="~{components/navbar :: navbar}"></div>
+     <div th:replace="~{components/sidebar :: sidebar}"></div>
      
-     <div class="container-fluid mt-4">
+     <main class="main-content">
+         <div class="container-fluid py-4">
              <!-- Contenido -->
+         </div>
+     </main>
  </body>
```

### **2. reportes/ventas.html**
- Aplicados los mismos cambios que index.html
- Eliminado `<div class="content-wrapper">` duplicado
- Limpieza de estructura HTML

### **3. reportes/clientes.html**
- Aplicados los mismos cambios que index.html
- Eliminado `<div class="content-wrapper">` duplicado

### **4. reportes/productos.html**
- Aplicados los mismos cambios que index.html
- Ya tenía navbar, solo faltaba sidebar y estructura correcta

---

