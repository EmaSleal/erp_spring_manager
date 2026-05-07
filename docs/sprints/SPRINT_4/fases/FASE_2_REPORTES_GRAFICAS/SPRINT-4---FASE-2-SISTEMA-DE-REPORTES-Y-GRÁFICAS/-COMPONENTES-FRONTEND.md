## 🎨 COMPONENTES FRONTEND

### Vista: `dashboard.html`

**Ubicación:** `src/main/resources/templates/admin/reportes/dashboard.html`

**Estructura de gráficas:**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout}">
<head>
    <title>Reportes y Análisis</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
</head>
<body>
    <div layout:fragment="content">
        
        <!-- Filtros -->
        <div class="card mb-4">
            <div class="card-header">
                <h5><i class="bi bi-filter"></i> Filtros de Reportes</h5>
            </div>
            <div class="card-body">
                <form id="formFiltros">
                    <div class="row">
                        <div class="col-md-3">
                            <label>Fecha Inicio</label>
                            <input type="date" 
                                   class="form-control" 
                                   id="fechaInicio"
                                   th:value="${filtros.fechaInicio}">
                        </div>
                        <div class="col-md-3">
                            <label>Fecha Fin</label>
                            <input type="date" 
                                   class="form-control" 
                                   id="fechaFin"
                                   th:value="${filtros.fechaFin}">
                        </div>
                        <div class="col-md-3">
                            <label>Categoría</label>
                            <select class="form-select" id="categoriaId">
                                <option value="">Todas</option>
                                <option th:each="cat : ${categorias}"
                                        th:value="${cat.id}"
                                        th:text="${cat.nombre}"></option>
                            </select>
                        </div>
                        <div class="col-md-3 d-flex align-items-end">
                            <button type="button" 
                                    class="btn btn-primary w-100"
                                    onclick="cargarReportes()">
                                <i class="bi bi-search"></i> Generar Reportes
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>

        <!-- Gráfica 1: Ventas por Mes (Line Chart) -->
        <div class="row mb-4">
            <div class="col-md-12">
                <div class="card">
                    <div class="card-header d-flex justify-content-between">
                        <h5><i class="bi bi-graph-up"></i> Ventas por Mes</h5>
                        <div>
                            <button class="btn btn-sm btn-outline-primary"
                                    onclick="exportar('ventas', 'pdf')">
                                <i class="bi bi-file-pdf"></i> PDF
                            </button>
                            <button class="btn btn-sm btn-outline-success"
                                    onclick="exportar('ventas', 'excel')">
                                <i class="bi bi-file-excel"></i> Excel
                            </button>
                        </div>
                    </div>
                    <div class="card-body">
                        <canvas id="chartVentas" height="80"></canvas>
                    </div>
                </div>
            </div>
        </div>

        <!-- Gráfica 2: Productos Más Vendidos (Bar Chart) -->
        <div class="row mb-4">
            <div class="col-md-6">
                <div class="card">
                    <div class="card-header">
                        <h5><i class="bi bi-bar-chart"></i> Top 10 Productos</h5>
                    </div>
                    <div class="card-body">
                        <canvas id="chartProductos" height="150"></canvas>
                    </div>
                </div>
            </div>

            <!-- Gráfica 3: Distribución por Categorías (Doughnut) -->
            <div class="col-md-6">
                <div class="card">
                    <div class="card-header">
                        <h5><i class="bi bi-pie-chart"></i> Distribución por Categorías</h5>
                    </div>
                    <div class="card-body">
                        <canvas id="chartCategorias" height="150"></canvas>
                    </div>
                </div>
            </div>
        </div>

        <!-- Gráfica 4: Comparativa Anual (Line Chart) -->
        <div class="row mb-4">
            <div class="col-md-12">
                <div class="card">
                    <div class="card-header">
                        <h5><i class="bi bi-arrow-left-right"></i> Comparativa Anual</h5>
                    </div>
                    <div class="card-body">
                        <canvas id="chartComparativa" height="80"></canvas>
                    </div>
                </div>
            </div>
        </div>

        <!-- Tabla: Estadísticas de Clientes -->
        <div class="row mb-4">
            <div class="col-md-12">
                <div class="card">
                    <div class="card-header">
                        <h5><i class="bi bi-people"></i> Estadísticas de Clientes</h5>
                    </div>
                    <div class="card-body">
                        <table class="table table-hover" id="tablaClientes">
                            <thead>
                                <tr>
                                    <th>Cliente</th>
                                    <th>Email</th>
                                    <th>Compras</th>
                                    <th>Total Gastado</th>
                                    <th>Ticket Promedio</th>
                                    <th>Última Compra</th>
                                    <th>Categoría</th>
                                </tr>
                            </thead>
                            <tbody>
                                <!-- Populated by JavaScript -->
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>

    </div>

    <!-- JavaScript -->
    <th:block layout:fragment="scripts">
        <script th:inline="javascript">
            // Variables globales para los gráficos
            let chartVentas, chartProductos, chartCategorias, chartComparativa;

            // Cargar reportes al iniciar
            document.addEventListener('DOMContentLoaded', function() {
                cargarReportes();
            });

            // Función principal de carga
            function cargarReportes() {
                const filtros = {
                    fechaInicio: document.getElementById('fechaInicio').value,
                    fechaFin: document.getElementById('fechaFin').value,
                    categoriaId: document.getElementById('categoriaId').value || null
                };

                fetch('/admin/reportes/datos', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(filtros)
                })
                .then(response => response.json())
                .then(data => {
                    renderizarGraficoVentas(data.ventasPorMes);
                    renderizarGraficoProductos(data.productosTop);
                    renderizarGraficoCategorias(data.distribucionCategorias);
                    renderizarGraficoComparativa(data.comparativaAnual);
                    renderizarTablaClientes(data.estadisticasClientes);
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error al cargar reportes');
                });
            }

            // 1. Gráfico de ventas (Line Chart)
            function renderizarGraficoVentas(datos) {
                const ctx = document.getElementById('chartVentas').getContext('2d');
                
                if (chartVentas) chartVentas.destroy();
                
                chartVentas = new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: datos.map(d => d.mes),
                        datasets: [{
                            label: 'Total Ventas (€)',
                            data: datos.map(d => d.totalVentas),
                            borderColor: 'rgb(75, 192, 192)',
                            backgroundColor: 'rgba(75, 192, 192, 0.2)',
                            tension: 0.4,
                            fill: true
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: true,
                        plugins: {
                            legend: { display: true },
                            tooltip: {
                                callbacks: {
                                    label: function(context) {
                                        return context.dataset.label + ': ' + 
                                               context.parsed.y.toFixed(2) + ' €';
                                    }
                                }
                            }
                        },
                        scales: {
                            y: {
                                beginAtZero: true,
                                ticks: {
                                    callback: function(value) {
                                        return value.toLocaleString('es-ES') + ' €';
                                    }
                                }
                            }
                        }
                    }
                });
            }

            // 2. Gráfico de productos (Bar Chart)
            function renderizarGraficoProductos(datos) {
                const ctx = document.getElementById('chartProductos').getContext('2d');
                
                if (chartProductos) chartProductos.destroy();
                
                chartProductos = new Chart(ctx, {
                    type: 'bar',
                    data: {
                        labels: datos.map(d => d.nombreProducto),
                        datasets: [{
                            label: 'Cantidad Vendida',
                            data: datos.map(d => d.cantidadVendida),
                            backgroundColor: 'rgba(54, 162, 235, 0.6)',
                            borderColor: 'rgb(54, 162, 235)',
                            borderWidth: 1
                        }]
                    },
                    options: {
                        indexAxis: 'y',
                        responsive: true,
                        maintainAspectRatio: true,
                        plugins: {
                            legend: { display: false }
                        },
                        scales: {
                            x: { beginAtZero: true }
                        }
                    }
                });
            }

            // 3. Gráfico de categorías (Doughnut)
            function renderizarGraficoCategorias(datos) {
                const ctx = document.getElementById('chartCategorias').getContext('2d');
                
                if (chartCategorias) chartCategorias.destroy();
                
                chartCategorias = new Chart(ctx, {
                    type: 'doughnut',
                    data: {
                        labels: datos.map(d => d.categoria),
                        datasets: [{
                            data: datos.map(d => d.totalVentas),
                            backgroundColor: datos.map(d => d.color)
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: true,
                        plugins: {
                            legend: { position: 'right' },
                            tooltip: {
                                callbacks: {
                                    label: function(context) {
                                        const porcentaje = datos[context.dataIndex].porcentajeVentas;
                                        return context.label + ': ' + 
                                               context.parsed.toFixed(2) + ' € (' + 
                                               porcentaje.toFixed(1) + '%)';
                                    }
                                }
                            }
                        }
                    }
                });
            }

            // 4. Gráfico comparativa (Line Chart con 2 datasets)
            function renderizarGraficoComparativa(datos) {
                const ctx = document.getElementById('chartComparativa').getContext('2d');
                
                if (chartComparativa) chartComparativa.destroy();
                
                chartComparativa = new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: datos.map(d => d.mes),
                        datasets: [
                            {
                                label: 'Año Actual',
                                data: datos.map(d => d.ventasAnioActual),
                                borderColor: 'rgb(54, 162, 235)',
                                backgroundColor: 'rgba(54, 162, 235, 0.2)'
                            },
                            {
                                label: 'Año Anterior',
                                data: datos.map(d => d.ventasAnioAnterior),
                                borderColor: 'rgb(255, 99, 132)',
                                backgroundColor: 'rgba(255, 99, 132, 0.2)'
                            }
                        ]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: true,
                        interaction: { mode: 'index', intersect: false },
                        scales: {
                            y: { beginAtZero: true }
                        }
                    }
                });
            }

            // 5. Tabla de clientes
            function renderizarTablaClientes(datos) {
                const tbody = document.querySelector('#tablaClientes tbody');
                tbody.innerHTML = '';

                datos.forEach(cliente => {
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td>${cliente.nombreCliente}</td>
                        <td>${cliente.email}</td>
                        <td>${cliente.totalCompras}</td>
                        <td>${cliente.totalGastado.toFixed(2)} €</td>
                        <td>${cliente.ticketPromedio.toFixed(2)} €</td>
                        <td>${cliente.ultimaCompra || 'N/A'}</td>
                        <td><span class="badge bg-${getBadgeColor(cliente.categoria)}">${cliente.categoria}</span></td>
                    `;
                    tbody.appendChild(tr);
                });
            }

            function getBadgeColor(categoria) {
                switch(categoria) {
                    case 'VIP': return 'success';
                    case 'Frecuente': return 'primary';
                    case 'Ocasional': return 'warning';
                    case 'Nuevo': return 'secondary';
                    default: return 'info';
                }
            }

            // Función de exportación
            function exportar(tipo, formato) {
                const filtros = new URLSearchParams({
                    fechaInicio: document.getElementById('fechaInicio').value,
                    fechaFin: document.getElementById('fechaFin').value,
                    tipoReporte: tipo
                });

                window.location.href = `/admin/reportes/exportar/${formato}?${filtros.toString()}`;
            }
        </script>
    </th:block>
</body>
</html>
```

---

