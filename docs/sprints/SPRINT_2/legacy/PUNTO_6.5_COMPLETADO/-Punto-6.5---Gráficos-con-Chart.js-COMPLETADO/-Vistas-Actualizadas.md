## 📝 Vistas Actualizadas

### **reportes/index.html (Dashboard)**

**Cambios:**
1. Agregadas 3 secciones de gráficos
2. Canvas para cada gráfico con IDs únicos
3. Inclusión de `reportes.js`
4. Carga automática de gráficos al cargar página

**Código HTML agregado:**
```html
<!-- Gráficos Estadísticos -->
<div class="row mt-5">
    <div class="col-12">
        <h4 class="mb-3"><i class="fas fa-chart-pie"></i> Gráficos Estadísticos</h4>
    </div>

    <!-- Gráfico: Ventas por Mes -->
    <div class="col-lg-6 mb-4">
        <div class="card shadow-sm">
            <div class="card-body">
                <h5 class="card-title">
                    <i class="fas fa-chart-line text-primary"></i> Ventas por Mes
                </h5>
                <p class="text-muted small">Últimos 12 meses</p>
                <canvas id="ventasPorMesChart" style="max-height: 300px;"></canvas>
            </div>
        </div>
    </div>

    <!-- Gráfico: Clientes Nuevos -->
    <div class="col-lg-6 mb-4">
        <div class="card shadow-sm">
            <div class="card-body">
                <h5 class="card-title">
                    <i class="fas fa-user-plus text-success"></i> Clientes Nuevos por Mes
                </h5>
                <p class="text-muted small">Últimos 12 meses</p>
                <canvas id="clientesNuevosChart" style="max-height: 300px;"></canvas>
            </div>
        </div>
    </div>

    <!-- Gráfico: Productos Más Vendidos -->
    <div class="col-lg-12 mb-4">
        <div class="card shadow-sm">
            <div class="card-body">
                <h5 class="card-title">
                    <i class="fas fa-trophy text-warning"></i> Top 10 Productos Más Vendidos
                </h5>
                <p class="text-muted small">Basado en cantidad total vendida</p>
                <canvas id="productosMasVendidosChart" style="max-height: 400px;"></canvas>
            </div>
        </div>
    </div>
</div>
```

**Scripts agregados:**
```html
<!-- Scripts -->
<div th:replace="~{layout :: scripts}"></div>
<script th:src="@{/js/reportes.js}"></script>
<script>
    document.addEventListener('DOMContentLoaded', function() {
        // Auto-ocultar alertas
        // ...

        // Cargar gráficos
        cargarGraficoVentasPorMes();
        cargarGraficoClientesNuevos();
        cargarGraficoProductosMasVendidos();
    });
</script>
```

---

### **reportes/ventas.html (Reporte de Ventas)**

**Cambios:**
1. Agregada sección de gráfico dinámico
2. Canvas para gráfico de ventas filtradas
3. Función de renderizado con datos inline
4. Condición Thymeleaf para mostrar solo si hay datos

**Código HTML agregado:**
```html
<!-- Gráfico de Ventas -->
<div class="row mt-4" th:if="${facturas != null && !facturas.isEmpty()}">
    <div class="col-12">
        <div class="card shadow-sm">
            <div class="card-body">
                <h5 class="card-title">
                    <i class="fas fa-chart-area text-primary"></i> Visualización de Ventas
                </h5>
                <p class="text-muted small">Distribución de ventas en el período seleccionado</p>
                <canvas id="ventasChart" style="max-height: 300px;"></canvas>
            </div>
        </div>
    </div>
</div>
```

---

